package com.ericsson.cifwkmediabom.maven.plugin;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.ericsson.cifwkmediabom.maven.plugin.utils.GenericRestCalls;

public class GetProductMediaContents {

    private GenericRestCalls genericRestCall;
    public String result;

    public String getMediaContents(String isoContentsJSONString, String getISOContentsRestUrl, Log log) throws MojoExecutionException, MojoFailureException {
        log.info("Get ISO Contents Rest Call run on: " + getISOContentsRestUrl);
        log.info("*** Getting ISO Contents for ***");
        log.info("JSON Imput Parameter :" + isoContentsJSONString);

        String ErrorMsg = "Error in getting http response from generic rest call set ip Get rest call.";
        try {
            genericRestCall = new GenericRestCalls();
            result = genericRestCall.setUpPOSTRestCallJSON(isoContentsJSONString, getISOContentsRestUrl, log);
            if (result.contains("error")) {
                log.error(ErrorMsg);
                throw new MojoFailureException(ErrorMsg);
            }
        } catch (Exception error) {
            log.error(ErrorMsg + ":" + error);
            try {
                throw new MojoFailureException(ErrorMsg + ":" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return result;
    }

    public String getIsoBOMJson(String restURL, Log log) throws MojoExecutionException, MojoFailureException {

        log.info("Get ISO Contents Rest Call run on: " + restURL);
        String ErrorMsg = "Error in getting http response from generic rest call set ip Get rest call.";
        try {
            genericRestCall = new GenericRestCalls();
            result = genericRestCall.setUpGETRestCallWithString(restURL, log);
            if (result.contains("error")) {
                log.error(ErrorMsg);
                throw new MojoFailureException(ErrorMsg);
            }
        } catch (Exception error) {
            log.error(ErrorMsg + ":" + error);
            try {
                throw new MojoFailureException(ErrorMsg + ":" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return result;
    }
}
