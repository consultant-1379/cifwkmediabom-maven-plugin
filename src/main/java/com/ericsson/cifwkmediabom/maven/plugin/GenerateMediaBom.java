/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.cifwkmediabom.maven.plugin;

import com.ericsson.cifwkmediabom.maven.plugin.utils.GenericRestCalls;
import com.ericsson.cifwkmediabom.maven.plugin.utils.JsonParser;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal generateisobom
 * @phase compile
 * @requiresProject false
 */
public class GenerateMediaBom extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    private String restResponse;
    private String pluginRestResponse;
    private String restURL;
    private String pluginRestURL;
    private String artifactId;
    private String version;
    private String groupId;
    private String product;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        artifactId = project.getProperties().getProperty("iso.name");
        version = project.getProperties().getProperty("iso.version");
        groupId = project.getProperties().getProperty("iso.groupId");
        product = project.getProperties().getProperty("product");
        restURL = project.getProperties().getProperty("rest.url");
        pluginRestURL = project.getProperties().getProperty("plugin.rest.url");
        try {
            getLog().info("Retrieving ISO contents for a given ISO Name and version:");
            String isoContentsJSONString = "{\"product\":\"" + product + "\",\"groupId\":\"" + groupId + "\",\"artifactId\":\"" + artifactId + "\",\"version\":\"" + version + "\"}";
            pluginRestResponse = new GenericRestCalls().setUpGETRestCallWithString(pluginRestURL, getLog());
            restResponse = new GenericRestCalls().setUpPOSTRestCallJSON(isoContentsJSONString, restURL, getLog());
            getLog().debug(restResponse);                        
            new JsonParser().parseJson(restResponse, pluginRestResponse, getLog());
        } catch (MojoExecutionException error) {
            getLog().error("Error getting media information from dependency management DB" + error);
            throw new MojoExecutionException(error.getMessage());
        } catch (MojoFailureException error) {
            getLog().error("Error getting media information from dependency management DB" + error);
            throw new MojoFailureException(error.getMessage());
        }

    }
}