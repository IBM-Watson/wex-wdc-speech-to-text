package com.ibm.watson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.codec.binary.Base64;


@Path("/recognize")
public class SpeechToText {
    final String ORIGINAL_SERVICE_URL = "https://stream.watsonplatform.net/speech-to-text/api";
    final String API_PATH = "/v1/recognize";
    final String SERVICE_NAME = "speech_to_text";
    final String XML_DEC = "<?xml version='1.0' encoding='UTF-8'?>";
    final String STATUS_OK = "<status>OK</status>";
    final String STATUS_ERROR = "<status>ERROR</status>";

    boolean DEBUG = false;  // Set to true to enable output to logs/messages.log

    @POST
    @Produces(MediaType.APPLICATION_XML)
    public String speechToText(byte[] incomingBinaryPOSTData) {
    /* We will pass the binary POST data we receive directly on to the service, so there
    *  is no need to break it into individual parameters or URL decode their
    *  values via a param annotation.  This should save us a bit of cpu and mem
    *  as well, since we are dealing with binary data.
    */
        HttpURLConnection conn = null;
        String serviceURL = "";

        try {
            if (incomingBinaryPOSTData.length == 0) {
                return XML_DEC + "<results>" + STATUS_OK + "<transcript />" + "</results>";
            }

            // 'VCAP_SERVICES' contains all the details of services bound to this application
            JsonObject credentials = readJsonObject(new StringReader(System.getenv("VCAP_SERVICES")))
                                    .getJsonArray(SERVICE_NAME)
                                    .getJsonObject(0)
                                    .getJsonObject("credentials");



            serviceURL = credentials.getString("url");

            if ("".equals(serviceURL)) {
                serviceURL = ORIGINAL_SERVICE_URL;
            }


            // set up our http request
            conn = setupHttpConnection(incomingBinaryPOSTData, serviceURL, credentials);

            // make the connection
            conn.connect();

            sendRequest(incomingBinaryPOSTData, conn);

            // Read the response from the service
            String lines = readResponse(conn);

            // Parse the transcript string out of the Speech To Text service's JSON response
            String transcript = readTranscripts(readJsonObject(new StringReader(lines)));

            // Return the response from the service
            return successXML(transcript);
        } catch (Exception e) {
            e.printStackTrace();
            return failXML(e, "Bluemix ");
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return failXML(e, "Bluemix ");
            }
        }

    }


    // Set up a connection with the speech-to-text service
    private HttpURLConnection setupHttpConnection(byte[] incomingBinaryPOSTData,
                                                  String serviceURL,
                                                  JsonObject credentials) throws IOException {

        String serviceUser = credentials.getString("username");
        String servicePass = credentials.getString("password");
        String auth = serviceUser + ":" + servicePass;

        // Prepare the query string parameters.
        // See the Speech to Text service documentation for details.
        String queryString = "&model=en-US_NarrowbandModel"
                + "&continuous=true";
        if (DEBUG) {
            System.out.println("POSTing [" + incomingBinaryPOSTData.length + "] bytes");
            System.out.println("Using URL [" + serviceURL + API_PATH + "?" + queryString + "]");
        }

        // Prepare the HTTP connection to the service
        HttpURLConnection conn = (HttpURLConnection) new URL(serviceURL + API_PATH + "?" + queryString).openConnection();

        // Set up our header with all the information we need to make the api request
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String(auth.getBytes()));
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Content-Type", "audio/flac");
        conn.setRequestProperty("Transfer-encoding", "chunked");
        conn.setFixedLengthStreamingMode(incomingBinaryPOSTData.length);

        // Uncomment the following to opt out of improving the service with this audio file
        //	    conn.setRequestProperty("X-WDC-PL-OPT-OUT", "1");
        return conn;
    }


    private void sendRequest(byte[] incomingBinaryPOSTData, HttpURLConnection conn) throws IOException {
        OutputStream output = conn.getOutputStream();
        output.write(incomingBinaryPOSTData);
        output.flush();
        output.close();
    }


    // Collect the transcripts from all the results in our response body
    private String readTranscripts(JsonObject jsonResponse) {
        JsonArray resultsArray = jsonResponse.getJsonArray("results");

        try {
            return resultsArray.stream().map(jv -> getTranscript((JsonObject) jv)).collect(Collectors.joining());
        } catch (Exception e) {
            e.printStackTrace();
            return failXML(e, "Unexpected response from service caused ");
        }
    }


    private String getTranscript(JsonObject speechResult) {
        return speechResult.getJsonArray("alternatives").getJsonObject(0).getString("transcript");
    }


    private String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        if (DEBUG) {
            System.out.println("Service response code: " + conn.getResponseCode());
            System.out.println("Service response message: " + conn.getResponseMessage());
        }

        String lines = br.lines().collect(Collectors.joining("\n"));
        br.close();

        if (DEBUG) System.out.println("Service response body:\n" + lines);

        return lines;
    }


    private JsonObject readJsonObject(StringReader source) {
        JsonReader jReader = Json.createReader(source);
        JsonObject jObject = jReader.readObject();
        jReader.close();
        return jObject;
    }


    private String successXML(String transcript) {
        return XML_DEC + "<results>" + STATUS_OK + "<transcript>" + transcript + "</transcript></results>";
    }


    private String failXML(Exception e, String msg) {
        return XML_DEC + "<results>"
                + STATUS_ERROR + "<statusInfo>" + msg + e.getClass().getName() + ": " + e.getMessage()
                + "</statusInfo><transcript /></results>";
    }

}