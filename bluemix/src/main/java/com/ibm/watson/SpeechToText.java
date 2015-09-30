package com.ibm.watson;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.commons.codec.binary.Base64;


@Path("/recognize")
public class SpeechToText {
    String ORIGINAL_SERVICE_URL = "https://stream.watsonplatform.net/speech-to-text/api";
    String API_PATH = "/v1/recognize";
    String SERVICE_NAME = "speech_to_text";
    String XML_DEC = "<?xml version='1.0' encoding='UTF-8'?>";
    String STATUS_OK = "<status>OK</status>";
    String STATUS_ERROR = "<status>ERROR</status>";

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
	    if (incomingBinaryPOSTData.length == 0) return XML_DEC + "<results>" + STATUS_OK + "<transcript />" + "</results>";

            // 'VCAP_SERVICES' contains all the details of services bound to this application
	    JsonReader jsonReader = Json.createReader(new StringReader(System.getenv("VCAP_SERVICES")));
	    JsonObject serviceInfo = jsonReader.readObject();
	    jsonReader.close();

            // Get the Service Credentials for SpeechToText
	    JsonObject credentials = serviceInfo.getJsonArray(SERVICE_NAME)
		.getJsonObject(0).getJsonObject("credentials");

            try {
                serviceURL = credentials.getString("url");
            } catch (Exception e) {}
            // If we didn't find a URL for the service in VCAP_SERVICES,
            // use the original.
            if ("".equals(serviceURL)) {
                serviceURL = ORIGINAL_SERVICE_URL;
            }
            
	    String serviceUser = credentials.getString("username");
	    String servicePass = credentials.getString("password");
	    String auth = serviceUser + ":" + servicePass;

	    // Prepare the query string parameters.
	    // See the Speech to Text service documentation for details.
	    String queryString = "";
	    queryString = queryString + "&model=en-US_NarrowbandModel";
	    queryString = queryString + "&continuous=true";

	    if (DEBUG) {
		System.out.println("POSTing [" + incomingBinaryPOSTData.length + "] bytes");
		System.out.println("Using URL [" + serviceURL + API_PATH + "?" + queryString + "]");
	    }

	    // Prepare the HTTP connection to the service
	    conn = (HttpURLConnection) new URL(serviceURL + API_PATH + "?" + queryString).openConnection();
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
            
            // make the connection
            conn.connect();
            
            // send the POST request
            OutputStream output = conn.getOutputStream();
            output.write(incomingBinaryPOSTData);
            output.flush();
	    output.close();
            
            // Read the response from the service
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

	    if (DEBUG) {
		System.out.println("Service response code: " + conn.getResponseCode());
		System.out.println("Service response message: " + conn.getResponseMessage());
	    }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();

	    if (DEBUG) System.out.println("Service response body:\n" + sb.toString());

	    // Parse the transcript string out of the Speech To Text service's JSON response
	    JsonReader responseReader = Json.createReader(new StringReader(sb.toString()));
	    JsonObject jsonResponse = responseReader.readObject();
	    responseReader.close();

	    String transcript = "";
	    try {
		JsonArray resultsArray = jsonResponse.getJsonArray("results");
		
		for (JsonValue jv : resultsArray) {
		    JsonObject jo = (JsonObject) jv;
		    transcript = transcript + jo.getJsonArray("alternatives")
			.getJsonObject(0)
			.getString("transcript");
		}
	    } catch(Exception e) {
		e.printStackTrace();
		return XML_DEC + "<results>" + STATUS_ERROR + "<statusInfo>Unexpected response from service caused " + e.getClass().getName() + ": " + e.getMessage() + "</statusInfo><transcript /></results>";
	    }
            
            // Return the response from the service
            return XML_DEC + "<results>" + STATUS_OK + "<transcript>" + transcript + "</transcript></results>";

        } catch(Exception e){
            e.printStackTrace();
            return XML_DEC + "<results>" + STATUS_ERROR + "<statusInfo>Bluemix " + e.getClass().getName() + ": " + e.getMessage() + "</statusInfo><transcript /></results>";
        }finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return XML_DEC + "<results>" + STATUS_ERROR + "<statusInfo>Bluemix " + e.getClass().getName() + ": " + e.getMessage() + "</statusInfo><transcript /></results>";
            }
        }
        
    }

}