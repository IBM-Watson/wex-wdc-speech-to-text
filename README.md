# Integrating the Watson Developer Cloud Speech to Text Service with Watson Explorer

[IBM Watson Explorer](http://www.ibm.com/smarterplanet/us/en/ibmwatson/explorer.html) combines search and content analytics with unique cognitive computing capabilities available through external cloud services such as the [Watson Developer Cloud](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/) to help users find and understand the information they need to work more efficiently and make better, more confident decisions.

The [Watson Speech to Text](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/speech-to-text.html) service can be used anywhere there is a need to bridge the gap between the spoken word and written form. The easy-to-use service uses machine intelligence to combine information about grammar and language structure with knowledge of the composition of the audio signal to generate an accurate transcription. The service uses IBM's speech recognition capabilities to convert speech in multiple languages into text.

This example integration uses the Watson Speech to Text service available on [Bluemix](http://www.ibm.com/cloud-computing/bluemix/). There are additional cognitive functions available on Bluemix that will be covered in other integration examples.  The full Watson Speech to Text documentation is available [online](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/doc/speech-to-text/).

The goal of this tutorial is to demonstrate how to get started with an integration between Watson Explorer and the Watson Speech to Text service available on Bluemix, in the [IBM Watson Developer Cloud](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/). By the end of the tutorial you will have a modified Watson Explorer Engine search collection that can send audio data to a custom Bluemix Java application which uses the Watson Speech to Text service for speech recognition.  The Engine collection will store the audio transcription as search results.


## Prerequisites
Please see the [Introduction](https://github.com/Watson-Explorer/wex-wdc-integration-samples) for an overview of the integration architecture, and the tools and libraries that need to be installed to create Java-based applications in Bluemix.

- An [IBM Bluemix account](https://bluemix.net/)
- [Cloud Foundry](https://github.com/cloudfoundry/cli/releases)
- [Watson Explorer](http://www.ibm.com/smarterplanet/us/en/ibmwatson/explorer.html) - Installed, configured, and running
- Some basic familiarity with Watson Explorer Engine.  Completion of the Watson Explorer [Engine meta-data tutorial](http://www-01.ibm.com/support/knowledgecenter/SS8NLW_11.0.0/com.ibm.swg.im.infosphere.dataexpl.engine.tut.md.doc/c_vse-metadata-tutorial.html) should be sufficient, but is not strictly necessary.


## What's Included in this Tutorial

This tutorial will walk through the creation and deployment of two components.

1. A basic Bluemix application exposing the Watson Speech to Text service as a web service.
2. A Watson Explorer Engine Speech to Text converter.  This converter sends an audio file to the Bluemix application, which in turn sends the audio file to the Speech to Text service for speech recognition and transcription.  The converter takes the audio transcription response text and converts it to VXML.


## Step-by-Step Tutorial

This section provides details on the steps required to create a Speech to Text service in Bluemix, deploy a basic proxy application in Bluemix, and install the new Speech to Text Engine converter.

### Creating the Speech to Text service and application in Bluemix

The Bluemix documentation can be found [here](https://bluemix.net/docs/).

Navigate to the Bluemix dashboard and create a new application with Liberty for Java.  For this example, we have chosen to name the application `wex-stt`.

Navigate to the Bluemix dashboard and create a new Speech to Text service.  Supply the name of the application you just created to which the service will bind.  For this example, we have chosen to name the service `wex-stt-serv`.

Note that your Bluemix application and Speech to Text service may alternatively be provisioned from the command-line via the `cf` tool.


### Configuring and Deploying the Watson Speech to Text proxy application in Bluemix

Clone this Git repository, or download the zip, extract, and navigate to the repository directory on the command-line.

The example Bluemix application uses a `manifest.yml` file to specify the application name, services bindings, and basic application settings.  Using a manifest simplifies distribution and deployment of CloudFoundry applications.

* Modify the manifest.yml file to agree with the service name, application name, and host name of the service and application you created in the previous step.

If you have not done so already, sign in to Bluemix.

```
$> cf api api.ng.bluemix.net
...
$> cf login
```

Build the application web service using [Apache Maven](http://maven.apache.org/). Before performing this step, verify that you are in the `/bluemix` directory of this repository. This will generate a Java WAR called `wex-wdc-SpeechToText-sample.war`.

```
$> mvn install
```


Finally, deploy the application to your space in the Bluemix cloud.  Subsequent pushes to Bluemix will overwrite the previous instances you have deployed.

```
$> cf push
```


Once the application has finished restarting, you can now view the route that was created for your application with `cf routes`.  The running application URL can be determined by combining the host and domain from the routes listing.  You can also find this information in the `manifest.yml` file.


### Installing and Configuring the Speech to Text Converter

The Speech to Text converter provided in this example integration is designed to send crawled data whose URL ends with the string `.flac` to the Speech to Text service for speech recognition analysis.  Flac is one of many different audio file formats.  If you expect to encounter other audio file formats during your crawl, it is recommended that you leverage software like [SoX](http://sox.sourceforge.net/) or [FFmpeg](https://ffmpeg.org/) to convert the audio data to flac format.  The [Watson Developer Cloud AlchemyVision integration](https://github.com/IBM-Watson/wex-wdc-alchemyapi-alchemyvision) provides an example of very similar processing - image files are converted to a single file format (PNG) by external software (ImageMagick) before being sent to the Watson AlchemyAPI service.  Be sure to allow your audio file types in **both** the **Binary file extensions (filter)** crawl condition and converter.

1. In Engine, create a new XML Element by ensuring you are on the **Configuration** tab of the admin tool and then clicking the "+" next to "XML" in the left menu.  The element and name can have any value.
2. Copy the entire contents of [function.vse-converter-speech-to-text-bluemix.xml](/engine/function.vse-converter-speech-to-text-bluemix.xml).
3. Paste the copied XML into the Engine XML text box, replacing all text that was previously there.
4. Save the converter configuration by clicking **OK**.
5. Create a new collection and navigate to the crawling configuration tab.  Add a new **Files** seed and point it at your local copy of the `/audio-samples` directory included with this repository.
6. Navigate to the converting tab of your collection and click **Add a new converter**.  The **Speech to Text Bluemix Converter** should now appear in the list of available converters.  Add the Speech to Text Bluemix Converter to your converter pipeline.
7. Edit the configuration of the Speech to Text converter.  Provide the endpoint of your Bluemix application in the **Bluemix application REST endpoint** setting.
8. Click **OK** to save the Speech to Text converter configuration.  Ensure that the Speech to Text converter appears first in your list of converters.

Use **Test it** or start a crawl and perform a search to confirm that you are receiving and indexing text transcriptions of the audio files.


# Suggested Use Cases

- See the [Watson Speech to Text service](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/speech-to-text.html#how-it-works-block) site for speech transcription use cases.


# Implementation Considerations

- **Crawl Performance** - An audio crawl can be expensive compared to a typical data crawl.  Audio data is generally large, and it must be fetched from its source, possibly converted to flac, sent to the Speech to Text service over the network, and analyzed by the Speech to Text service.
- **Privacy and Security** - The Speech to Text converter makes a web request to the Bluemix application endpoint.  In this example, this call is made over an unencrypted and unauthenticated HTTP connection, but your Bluemix application can be modified to support better security.
- **Failures will happen** - All distributed systems are inherently unreliable and failures will inevitably occur when attempting to call out to Bluemix.  Consider how failures should be handled.
- **Data Preparation** - It is the responsibility of the caller to ensure that representative data is being sent to the Speech to Text service.  Additional data preparation may be required in some cases.  Choose content carefully.
- **Scalability** - This example uses only a single cloud instance with the default Bluemix application settings.  In a production scenario consider how much hardware will be required and adjust the Bluemix application settings accordingly.
- **Additional audio file format support** - The Speech to Text converter provided in this example integration is designed to send crawled data whose URL ends with the string `.flac` to the Speech to Text service for speech recognition analysis.  Flac is one of many different audio file formats.  If you expect to encounter other audio file formats during your crawl, it is recommended that you leverage software like [SoX](http://sox.sourceforge.net/) or [FFmpeg](https://ffmpeg.org/) to convert the audio data to flac format.  The [Watson Developer Cloud AlchemyVision integration](https://github.com/IBM-Watson/wex-wdc-alchemyapi-alchemyvision) provides an example of very similar processing - image files are conveted to a single file format (PNG) by external software (ImageMagick) before being sent to the Watson AlchemyAPI service.  Be sure to allow your audio file types in **both** the **Binary file extensions (filter)** crawl condition and converter.

## Caching Proxy
Given the considerations listed here, the use of a caching proxy is always recommended in a Production environment.  Using a caching proxy can speed up widget refreshes in some situations, overcome some network failures, and in some cases may also allow you to reduce the total number of required Bluemix transactions.


# Licensing
All sample code contained within this project repository or any subdirectories is licensed according to the terms of the MIT license, which can be viewed in the file license.txt.



# Open Source @ IBM
[Find more open source projects on the IBM Github Page](http://ibm.github.io/)

    
