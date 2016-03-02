package com.aat

import com.android.build.gradle.api.ApplicationVariant
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.EncoderRegistry
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.URLENC

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.services.oauth2.Oauth2
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.auth.oauth2.Credential


class DownloadTextTask extends DefaultTask {

    ApplicationVariant applicationVariant
    String variantName
    TextPluginExtension textPluginExt
    def ws

    DownloadTextTask() {
        super()
    }

    @TaskAction
    def load() throws IOException {
        textPluginExt = project.texts
        if (textPluginExt.ws) {
            initWsUrl()
            textPluginExt.languages.add(textPluginExt.defaultLanguage)
            textPluginExt.languages.each {
                loadTextWithLang(it.toLowerCase())
            }
        } else if (textPluginExt.gSheetClientId) {
            callSheetApiThree()
        }
    }

    public void loadTextWithLang(String lang) {
        println "WS : " + ws
        def content = new HTTPBuilder(ws.toString()).request(GET, TEXT) { req ->
            headers.'accept' = 'application/json'
            headers.'language' = lang
            headers.'translateKey' = '%_$s'

            response.success = { resp, reader ->
                // println reader.text
                reader.text
            }

            response.failure = { resp, reader ->
                println resp.statusLine
                println 'Impossible to download texts ...'
            }
        }
        if (content) {
            def json = new JsonSlurper().parseText(content)
            def valuesDir = '/values';
            if (!lang.equals(textPluginExt.defaultLanguage)) {
                valuesDir = '/values-' + lang
            }
            
            String resPath = project.file('src/main/res')
            File myDir = new File(resPath + valuesDir);
            if (!myDir.exists()) {
                myDir.mkdirs()
            }
            File file = new File(myDir.getAbsolutePath() + File.separator + 'strings.xml')
            file.write '<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n'
            file << '<!-- DO NOT EDIT THIS FILE, IT HAS BEEN GENERATED BY groovy script made by aat -->\n\n'
            file << '<resources>\n'

            def texts = json.data.texts
            if (textPluginExt.alphabeticallySort) {
                texts.sort {
                    it.key.toLowerCase()
                }
            }
            if (textPluginExt.removeDuplicate) {
                texts.unique {
                    it.key.trim()
                }
            }

            texts.each { myText ->
                if (myText.key != null && myText.value != null) {
                    myText.key = myText.key.trim()
                    if (textPluginExt.removeBadKeys && myText.key.contains(" ")) {
                        return
                    } else if (!myText.key.matches("\\d.*")) {  // key must not start with a digit
                        if (myText.value.contains("&")) {
                            myText.value = myText.value.replaceAll("&", "&amp;")
                        }
                        if (myText.value.contains("'")) {
                            myText.value = myText.value.replaceAll("'", "\\\\'")
                        }

                        // Add formatted="false" if text contains %
                        // But we do not handle %1$s
                        def pattern = /.*%[0-9]\$.*/
                        if (myText.value.contains('%') && !(myText.value ==~ pattern)) {
                            myText.key = myText.key + '" formatted="false'
                        }
                        file << "    <string name=\"${myText.key.trim()}\">$myText.value</string>\n"
                    }
                }
            }

            // Theses keys will be addded by customer later
            if (lang == textPluginExt.defaultLanguage && textPluginExt.missingKeys != null) {
                file << '    <!-- Keys added by user -->\n'
                def values = textPluginExt.missingKeys.tokenize('\n')
                // Add indentation of 4 spaces
                values.each { missingKey ->
                    file << '    ' + missingKey.trim() + '\n'
                }
                // file << textPluginExt.missingKeys
            }
            file << '</resources>'
            println "We've done with [" + lang + ']'
        } else {
            println 'Content is null or empty'
        }
    }

    public void callSheetApi() {
        // https://developers.google.com/identity/protocols/OAuth2WebServer
        // String oauthUrl = 'https://accounts.google.com/o/oauth2/v2/auth?response_type=token&client_id=' + textPluginExt.gSheetClientId + '&redirect_uri=https%3A%2F%2Foauth2-login-demo.appspot.com%2Fcode&scope=https://www.googleapis.com/auth/drive'
        String oauthUrl = 'https://accounts.google.com/o/oauth2/device/code'
        println 'Url : ' + oauthUrl

        def code = new HTTPBuilder(oauthUrl).request(POST, URLENC) { req ->
            // headers.'Content-Type' = 'application/x-www-form-urlencoded'
            body = [client_id: textPluginExt.gSheetClientId, scope: 'https://docs.google.com/feeds']

            headers.'Accept' = 'application/json'
            headers.'Content-Type' = 'application/x-www-form-urlencoded'
            response.success = { resp, reader ->
                def stream
                println 'Status : ' + resp.statusLine
                println 'Stream : ' + reader
                reader.each { key, value ->
                    println 'Key : ' + key
                    println 'Value : ' + value
                    stream = key
                }
                println 'device_code : ' + reader.device_code
                println 'Class : ' + reader.getClass()
                println reader.text
                stream
            }
            response.failure = { resp, reader ->
                println resp.statusLine
                println reader.text
            }
        }

        if (code) {
            println 'JSON is good'
            def json = new JsonSlurper().parseText(code)
            println 'Device code : ' + json.device_code
            def oauthCode = new HTTPBuilder(oauthUrl).request(POST, URLENC) { req ->
                // headers.'Content-Type' = 'application/x-www-form-urlencoded'
                body = [client_id: textPluginExt.gSheetClientId, 
                        scope: 'https://docs.google.com/feeds',
                        code: json.device_code,
                        grant_type: 'http://oauth.net/grant_type/device/1.0']

                headers.'Accept' = 'application/json'
                headers.'Content-Type' = 'application/x-www-form-urlencoded'
                response.success = { resp, reader ->
                    def stream
                    println 'Status : ' + resp.statusLine
                    println 'Stream : ' + reader
                    reader.each { key, value ->
                        println 'Key : ' + key
                        println 'Value : ' + value
                        stream = key
                    }
                    println 'Class : ' + reader.getClass()
                    println reader.text
                    stream
                }
                response.failure = { resp, reader ->
                    println resp.statusLine
                    println reader.text
                }
            }
        } else {
            println 'An error occurred !'
        }

        /*def content = new HTTPBuilder(ws.toString()).request(GET, TEXT) { req ->
            headers.'accept' = 'application/json'
            response.success = { resp, reader ->
                println resp.statusLine
                println reader.text
            }
            response.failure = { resp, reader ->
                println resp.statusLine
                println reader.text
            }
        }*/
    }

    public void callSheetApiTwo() {
        // Make Oauth
        Details details = new Details()
        details.setClientId(textPluginExt.gSheetClientId)
        details.setClientSecret(textPluginExt.gSheetClientSecret)
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
        clientSecrets.setInstalled(details)

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        List<String> SCOPES = ['https://spreadsheets.google.com/feeds']
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JacksonFactory.getDefaultInstance(), clientSecrets, SCOPES).build()

        // authorization
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")

        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential).build()

        println 'Access token : ' + credential.getAccessToken()
    }

    public void callSheetApiThree() {
        // https://developers.google.com/identity/protocols/OAuth2ServiceAccount
        String header = '''{"alg":"RS256","typ":"JWT"}.
{
"iss":"761326798069-r5mljlln1rd4lrbhg75efgigp36m78j5@developer.gserviceaccount.com",
"scope":"https://www.googleapis.com/auth/prediction",
"aud":"https://www.googleapis.com/oauth2/v4/token",
"exp":1328554385,
"iat":1328550785
}.
'''
        String encoded = header.bytes.encodeBase64().toString()
        println encoded
     }

    private void initWsUrl() {
        String wsUrl = textPluginExt.ws
        if (textPluginExt.variantToWs) {
            if (textPluginExt.variantToWs[variantName]) {
                wsUrl = textPluginExt.variantToWs[variantName]
            }
        }
        ws = new URL(wsUrl)
    }
}

