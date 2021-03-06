[ ![Download](https://api.bintray.com/packages/arnoult-antoine/maven/texts-manager/images/download.svg) ](https://bintray.com/arnoult-antoine/maven/texts-manager/_latestVersion)

# texts-manager

A gradle plugin to manage texts from WS or csv hosted as google sheet

## Basic usage

Add to your build.gradle

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.mrroboaat:texts-manager:1.1.0'
    }
}

apply plugin: 'com.mrroboaat'
android {
    // ...
    texts {
        // ...
    }
}
```

## Webservice

This library download text from ws. Language is sent as header to API. Model should be as json like following :

```json
{
    "data": {
        "texts": [
            {
                "key": "my_key",
                "value": "My Value"
            },
            {
                "key": "other_key",
                "value": "Other wonderful value"
            }
        ]
    }
}
```

Plugin will produce the following lines and create right xml files if it does not exist
```xml
<string name="my_key">My Value</string>
```

## Google sheet

TODO : Explain how to get client_secret.json form google API console

Go to https://console.developers.google.com
Enable google sheets API


Your sheet should look like :

![img](https://github.com/aat-antoine/texts-manager/blob/master/GoogleSheet.png)


## Advanced usage

You have to distinguish 2 use case :

### First : with webservice

```gradle
texts {
        defaultLanguage = 'en'
        languages = ['de', 'es', 'fr']
        ws = 'http://hostname/text'
        missingKeys = '''
<!-- other resources (not present in WS) -->
<string name="app_name">appName</string>
<string name="dontforget">Don\'t Forget</string>
'''
...
}
```

Don't forget to escape your special characters

### Second : with google sheet

```gradle
texts {
        defaultLanguage = 'en'
        languages = ['en', 'fr']
        gSpreadsheetId = '1yrV86y7UixUWNZUj5nwnWLePzZnDdwLTSuIAcbgFIZs'
        gSheetClientSecret = '/home/antoine/Desktop/client_secret.json'
        // A = 0, B = 1, ...
        gColumnIndexForKey = 1
        gColumnIndexForValue = 2
}
```


### Fields
* `defaultLanguage`: Your default language
* `languages`: All languages available for the app
* `ws`: Where you can retrieve texts as json
* `gSpreadsheetId`: Identifier of google sheet
* `gSheetClientSecret`: Path to account service
* `gColumnIndexForKey`: Index from google sheet where we can get keys
* `gColumnIndexForValue`: Index from google sheet where we can get values
* `missingKeys`: Keys you want to add (not in WS or google sheet)
* `removeDuplicate`: Remove all duplicates keys (default is yes)
* `alphabeticallySort`: Use alphabetical sort on the keys (default is yes)
* `removeBadKeys`: Remove keys which contains white space (default yes)


