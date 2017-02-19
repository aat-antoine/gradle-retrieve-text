[ ![Download](https://api.bintray.com/packages/arnoult-antoine/maven/gradle-retrieve-text/images/download.svg) ](https://bintray.com/arnoult-antoine/maven/gradle-retrieve-text/_latestVersion)

# texts-manager

A gradle plugin to manage texts from WS or csv hosted as google sheet

## WS

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

## gSheet

TODO

## Basic usage

Add to your build.gradle

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.mrroboaat:gradle-retrieve-text:0.5.0'
    }
}

apply plugin: 'com.mrroboaat'
text {
    ...
}
```

## Advanced usage

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

### Fields
* `defaultLanguage`: Your default language
* `languages`: All languages available for the app
* `ws`: Where you can retrieve texts as json
* `missingKeys`: Keys you want to add (not in WS)
* `removeDuplicate`: Remove all duplicates keys (default is yes)
* `alphabeticallySort`: Use alphabetical sort on the keys (default is yes)
* `removeBadKeys`: Remove keys which contains white space (default yes)


