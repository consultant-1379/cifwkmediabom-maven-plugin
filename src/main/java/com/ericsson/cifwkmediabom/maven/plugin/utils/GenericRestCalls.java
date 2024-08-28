package com.ericsson.cifwkmediabom.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public class GenericRestCalls {

    private HttpResponse response = null;
    public String paramsString;
    public String result = "";
    private String encodedURL;

    public String setUpGETRestCall(List<NameValuePair> params, String restUrl, Log log) throws MojoExecutionException, MojoFailureException {
        try {
            HttpClient client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            paramsString = URLEncodedUtils.format(params, "UTF-8");
            log.debug(paramsString);
            HttpGet request = new HttpGet(restUrl + "?" + paramsString);
            response = client.execute(request);
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall: " + IOerror);
            throw new MojoFailureException("Error trying to create a UrlEncodedFormEntity with db information " + IOerror);
        }
        try {
            log.debug("*** Executing Rest GET to CIFWK DB ***");
            log.debug("Rest Call GET: " + restUrl + "?" + paramsString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                result += line + "\r\n";
                if (line.toLowerCase().contains("error")) {
                    throw new MojoFailureException("Error getting information from cifwk DB :" + line);
                }
                log.debug(line);
            }
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall execute on the client: ", IOerror);
            throw new MojoFailureException("Error posting information to the cifwk DB :" + IOerror);
        }
        return result;
    }

    public String setUpGETRestCallWithString(String restUrl, Log log) throws MojoExecutionException, MojoFailureException {
        try {
            HttpClient client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            HttpGet request = new HttpGet(restUrl);
            response = client.execute(request);
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall: " + IOerror);
            throw new MojoFailureException("Error trying to create a UrlEncodedFormEntity with db information " + IOerror);
        }
        try {
            log.info("*** Executing Rest GET to CIFWK DB ***");
            log.info("Rest Call GET: " + restUrl + "?" + paramsString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                result += line + "\r\n";
                if (line.toLowerCase().contains("error")) {
                    throw new MojoFailureException("Error getting information from cifwk DB :" + line);
                }
                log.debug(line);
            }
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall execute on the client: ", IOerror);
            throw new MojoFailureException("Error posting information to the cifwk DB :" + IOerror);
        }
        return result;
    }

    public String setUpPOSTRestCall(List<BasicNameValuePair> params, String restUrl, Log log) {
        HttpPost post = null;
        try {
            HttpClient client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            post = new HttpPost(restUrl);
            post.setEntity(new UrlEncodedFormEntity(params));
            log.debug("*** Executing Rest POST to CIFWK DB ***");
            log.debug("Rest Call POST: " + post.toString() + " with parameters: " + params);
            response = client.execute(post);
        } catch (Exception error) {
            log.error("Error with setUpPOSTRestCall: " + error);
            try {
                throw new MojoFailureException("Error trying to create a UrlEncodedFormEntity with db information " + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        try {
            log.info("*** Get Content of Post Response ***");
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result += line;
                if (line.toLowerCase().contains("error")){
                    throw new MojoFailureException("Error posting information to the cifwk DB :" +line);
                }
            }
        } catch (Exception error) {
            log.error("Error with setUpPOSTRestCall execute on the client: ", error);
            try {
                throw new MojoFailureException("Error getting information from the cifwk DB :" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        log.info("*** Got Content of Post Response ***");
        return result;
    }

    public String setUpPOSTRestCallJSON(String isoContentsJSONString, String restUrl, Log log) throws MojoExecutionException, MojoFailureException {
        String result = "";
        HttpPost post = null;
        try {
            HttpClient client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            post = new HttpPost(restUrl);
            post.setEntity(new StringEntity(isoContentsJSONString));
            post.setHeader("Content-type","application/json");
            log.debug("*** Executing Rest POST to CIFWK DB ***");
            log.debug("Rest Call POST: " + post.toString() + " with parameters: " + isoContentsJSONString);
            response = client.execute(post);
        } catch (IOException IOerror) {
            log.error("Error with setUpPOSTRestCall: " + IOerror);
            throw new MojoFailureException("Error with setUpPOSTRestCall: " + IOerror);
        }

        try {
            log.info("*** Get Content of Post Response ***");
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result += line + "\r\n";
                if (line.toLowerCase().contains("error")) {
                    throw new MojoFailureException("Error posting information to the cifwk DB :" + line);
                }
                log.debug(line);
            }
        } catch (IOException IOerror) {
            log.error("Error with setUpPOSTRestCall execute on the client: ", IOerror);
            throw new MojoFailureException("Error posting information to the cifwk DB :" + IOerror);
        }
        return result;
    }
}
