package com.ericsson.cifwkmediabom.maven.plugin;

import com.ericsson.cifwkmediabom.maven.plugin.utils.GenericRestCalls;
import com.ericsson.cifwkmediabom.maven.plugin.utils.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal buildisobom
 * @phase compile
 * @requiresProject false
 */
public class IdentifyMediaBOMContent extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * @parameter property="baseIsoName" default-value="None"
     */
    @Parameter
    private String baseIsoName;

    /**
     * @parameter property="cifwkGetDropContentsRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/getPackagesInISO/"
     */
    @Parameter
    private String getISOContentsRestUrl;

    /**
     * @parameter property="media.os.version" default-value="LITP1"
     */
    @Parameter
    private String mediaOSVersion;

    /**
     * @parameter default-value="${localRepository}"
     */
    @Parameter
    private ArtifactRepository localRepository;

    /**
     * @parameter property="local.nexus.repo" default-value="https://arm901-eiffel004.athtem.eei.ericsson.se:8443/nexus/content/repositories/enm_iso_local"
     */
    @Parameter
    private String localNexusRepo;

    private String isoName;
    private String isoVersion;
    private String projectMediaDrop = "drop";
    private String projectMediaProduct = "product";
    private String mediaContentName = "iso-dir";
    private String projectRstate = "ericsson.rstate";
    private String projectCXPNumber = "CXP";
    private String mediaDrop = "";
    private String mediaProduct = "";
    private Map<String, String> isoContentsMap = new HashMap<String, String>();
    private String getMediaContentResult = "";
    private String localRepositoryName;
    private String latestMaven = "/home/lciadm100/tools/maven-latest/bin/mvn";
    private List<String> artifactGAVList = new ArrayList<String>();
    public String excludeMediaCategory;
    private String errorMsg;
    private String pluginRestURL;
    private String pluginRestResponse;
    private Map<String, String> pluginProperties;

    public void execute() throws MojoExecutionException, MojoFailureException {

        localRepositoryName = localRepository.getUrl().replaceAll("file:", "");
        mediaDrop = project.getProperties().getProperty(projectMediaDrop);
        isoContentsMap.put(projectMediaDrop, mediaDrop);
        mediaProduct = project.getProperties().getProperty(projectMediaProduct);
        isoContentsMap.put(projectMediaProduct, mediaProduct);
        mediaContentName = project.getProperties().getProperty(mediaContentName);
        projectRstate = project.getProperties().getProperty(projectRstate);
        projectCXPNumber = project.getProperties().getProperty(projectCXPNumber);
        pluginRestURL = project.getProperties().getProperty("plugin.rest.url");

        isoName = project.getProperties().getProperty("iso.name");
        isoVersion = project.getProperties().getProperty("iso.version");

        try {

            getLog().info("Retrieving ISO contents for a given ISO Name and version:");
            String isoContentsJSONString = "{\"isoName\":\"" + isoName + "\",\"isoVersion\":\"" + isoVersion + "\"}";
            GetProductMediaContents getProductMediaContent = new GetProductMediaContents();
            pluginRestResponse = new GenericRestCalls().setUpGETRestCallWithString(pluginRestURL, getLog());
            pluginProperties = new JsonParser().parseVersionedPlugins(pluginRestResponse);
            getMediaContentResult = getProductMediaContent.getMediaContents(isoContentsJSONString, getISOContentsRestUrl, getLog());
            getLog().debug(getMediaContentResult);
        } catch (Exception error) {
            getLog().error("Error getting media information from cifwk" + error);
            try {
                 throw new MojoFailureException("Error getting Media Data from cifwk DB :" + error);
            } catch (MojoFailureException error1) {
                getLog().error(error1);
                error1.printStackTrace();
            }
        }

        try {
            CreateLocalMediaContent createLocalMediaContent = new CreateLocalMediaContent();
            try {
                artifactGAVList = createLocalMediaContent.downLoadArtifactLocally(getMediaContentResult, localRepositoryName, mavenSession, latestMaven, localNexusRepo, pluginProperties, getLog());
            } catch (MojoFailureException error) {
                errorMsg = "Error downloading Artifacts Locally : " + error;
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            } catch (IOException IOError) {
                errorMsg = "Error downloading Artifacts Locally : " + IOError;
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            }

        } catch (Exception error) {
            errorMsg = "Error in creating local media content: " + error;
            getLog().error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }
}
