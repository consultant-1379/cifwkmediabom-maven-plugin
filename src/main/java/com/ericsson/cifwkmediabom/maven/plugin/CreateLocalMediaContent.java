package com.ericsson.cifwkmediabom.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import com.ericsson.cifwkmediabom.maven.plugin.utils.CommandHandling;
import com.ericsson.cifwkmediabom.maven.plugin.utils.FileHandling;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateLocalMediaContent {

    public String mediaApplictaionDirectory = "";
    public String mediaThirdPartyProductsDirectory = "";
    public String mediaPluginsDirectory = "";
    public String mediaImagesDirectory = "";
    public String mediaModelsDirectory = "";
    public String allPackagesDirectory = "";
    public String bomArtifactId;
    public String isoVersion;
    public String isoGroupId;
    private Map<String, String> artifactDetails = new HashMap<String, String>();
    private List<String> artifactGAVList = new ArrayList<String>();
    public Map<String, String> isoDirStructureCategoryMap = new HashMap<String, String>();
    public Map<String, String> rpmMediaCatMapping = new HashMap<String, String>();
    public String mediaDirectory;
    public List<String> artifactList = new ArrayList<String>();
    public List<String> mediaCategoryList = new ArrayList<String>();
    public String bomDependencyIndent = "%-6s%s";
    public String propertyBOMString;
    public String dependencyBOMString;
    public String completeBOMString;
    public String projectRootDirectory = "";
    private File templateFile;
    private Map<String, String> pluginDetails = new HashMap<String, String>();
    private Map<String, String> bomContentDetails = new HashMap<String, String>();
    private Map<String, String> isoGAV = new HashMap<String, String>();
    public String mediaCategoryMediaDirectoryMap;
    private String errorMsg;
    public Boolean pluginFound;
    public String pluginArtifactId;
    String mavenProperty;

    public List<String> downLoadArtifactLocally(String result, String localRepositoryName, MavenSession session, String latestMaven, String localNexusRepo, Map<String, String> pluginProperties, Log log) throws IOException, MojoFailureException {
        log.info("********** Getting Bom Snipplets and Creating ISO BOM **************");
        propertyBOMString = "";
        dependencyBOMString = "";
        pluginFound = false;
        JSONObject jsonObject = new JSONObject(result);
        JSONArray jsonArray = jsonObject.getJSONArray("PackagesInISO");
        for (int i = 0; i < jsonArray.length(); i++) {
            String packageURL = jsonArray.getJSONObject(i).getString("url");
            projectRootDirectory = session.getExecutionRootDirectory();
            buildBOMContentString(log, projectRootDirectory, packageURL, pluginProperties);
        }
        getBOMGAV(result, log);
        handleISOBOM(session, localRepositoryName, latestMaven, localNexusRepo, log);
        return artifactGAVList;
    }

    public void buildBOMContentString(Log log, String projectRootDirectory, String packageURL, Map<String, String> pluginProperties) {
        try {
            String packageTest = packageURL.replace(FilenameUtils.getExtension(packageURL), "bomSnipplet");
            int code = new CommandHandling().getURLResponseCode(packageTest, log);
            if (code == 200) {
                File localBOMFileName = new File(projectRootDirectory + "/target/" + FilenameUtils.getName(packageTest));
                log.debug("Snipplet File Location " + projectRootDirectory + "/target/" + FilenameUtils.getName(packageTest));
                FileUtils.copyURLToFile(new URL(packageTest), localBOMFileName);
                LineIterator localBOMFileIterator = FileUtils.lineIterator(localBOMFileName, "UTF-8");

                try {
                    pluginArtifactId = "";      
                    while (localBOMFileIterator.hasNext()) {
                        String line = localBOMFileIterator.nextLine();
                        bomDependenciesParser(log, line, pluginProperties);
                    }
                } catch (MojoFailureException mfe) {
                     log.error("Error Parsing BOM: " + mfe);
                }finally {
                    localBOMFileIterator.close();
                }
            }
        } catch (IOException error) {
            log.error("Error in get Package Function: " + error);
            error.printStackTrace();
        }
    }

    public void handleISOBOM(MavenSession session, String localRepositoryName, String latestMaven, String localNexusRepo, Log log) throws IOException, MojoFailureException {
        try {
            String[] projectRootContents = new File(session.getExecutionRootDirectory()).list();
            for (String rootContent : projectRootContents) {
                if (rootContent.contains("bom")) {
                    templateFile = new File(rootContent + "/pom.xml");
                    break;
                } else {
                    continue;
                }
            }
            completeBOMString = "  <properties>\n" + propertyBOMString + "  </properties>\n\n  <dependencyManagement>\n    <dependencies>\n" + dependencyBOMString + "    </dependencies>\n  </dependencyManagement>";
            bomContentDetails.put("<!-- BOM Content -->", completeBOMString);
            String localBOMFile = FileHandling.copyFindReplaceInFile(templateFile, "isoCompleteBOM", bomContentDetails);
            Iterator mapIterator = isoGAV.entrySet().iterator();

            while (mapIterator.hasNext()) {
                Map.Entry pair = (Map.Entry)mapIterator.next();
                String find = (pair.getKey()).toString();
                String replace = (pair.getValue()).toString();
                FileHandling.replaceStringInFile(localBOMFile, find, replace);
            }

            String localBOMFileCopy = bomArtifactId + ".xml";
            log.info("Local BOM File Created :" + localBOMFileCopy);

            FileUtils.copyFile(new File(localBOMFile), new File(localBOMFileCopy));
            String sessionGroupID = session.getCurrentProject().getGroupId();
            String sessionArtifactID = session.getCurrentProject().getArtifactId();
            String sessionVersion = session.getCurrentProject().getVersion();
            sessionGroupID = sessionGroupID.replaceAll("\\.", "/");
            String bomURL = localNexusRepo + "/" + sessionGroupID + "/" + sessionArtifactID + "/" + sessionVersion + "-BOM/" + sessionArtifactID + "-" + sessionVersion + "-BOM.pom";
            log.info("Check that ISO BOM does not already exist in Repository. BOM" + bomURL);
            int code = new CommandHandling().getURLResponseCode(bomURL, log);
            log.info("Returned Response code. " + code);
            if (code != 200) {
                log.info("Beginning process of uploading ISO BOM to Repository");
            }

            sessionGroupID = session.getCurrentProject().getGroupId();
            sessionArtifactID = "enm_iso-bom"; 
            sessionGroupID = sessionGroupID.replaceAll("\\.", "/");
            bomURL = localNexusRepo + "/" + sessionGroupID + "/" + sessionArtifactID + "/" + sessionVersion + "/" + sessionArtifactID + "-" + sessionVersion + ".pom";
            log.info("Check that ISO BOM does not already exist in Repository. enm_iso-bom" + bomURL);
            code = new CommandHandling().getURLResponseCode(bomURL, log);
            log.info("Returned Response code. " + code);
            if (code != 200) {
                log.info("Beginning process of uploading ISO BOM to Repository");
            }
        } catch (Exception error) {
            errorMsg = "Error: with handling Media ISO BOM. " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }

    public void getBOMGAV(String result, Log log) throws MojoFailureException {
        try{
            log.info("********** Set ISO BOM GAV from JSON **************");
            JSONObject jsonObject = new JSONObject(result);
            isoGroupId = jsonObject.getString("ISOGroupID");
            bomArtifactId = "ERICenm-bom";
            isoVersion = jsonObject.getString("ISOVersion");

            isoGAV.put("__GROUPID__", isoGroupId);
            isoGAV.put("__ARTIFACTID__", bomArtifactId);
            isoGAV.put("__VERSION__", isoVersion);

            log.info("BOM Details: " + isoGroupId + ":" + bomArtifactId + ":" + isoVersion);

        } catch (Exception error) {
            errorMsg = "Error Retriving GAV from JSON. " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }

    public void bomDependenciesParser(Log log, String line, Map<String, String> pluginProperties) throws MojoFailureException {

        if ((!line.startsWith("<?"))) {
            line = bomPluginsParser(log, line, pluginProperties);
            dependencyBOMString += String.format(bomDependencyIndent, "", line) + "\n";
        }
    }

    public String bomPluginsParser(Log log, String line, Map<String, String> pluginProperties) throws MojoFailureException {

        if ((line.contains("<artifactId>"))){
            pluginArtifactId = line.substring(line.indexOf(">") + 1, line.indexOf("</artifactId>"));
            if((pluginProperties.containsKey(pluginArtifactId))){
                mavenProperty = "<" + pluginProperties.get(pluginArtifactId) + ">pluginVersion</" + pluginProperties.get(pluginArtifactId) + ">";
                pluginFound = true;
            }
        }

        if ((line.contains("<version>") && pluginFound)){
            String pluginVersion = line.substring(line.indexOf(">") + 1, line.indexOf("</version>"));
            String mavenPropertyVariable = "${" + pluginProperties.get(pluginArtifactId) + "}";
            line = line.replace(line.substring(line.indexOf(">") + 1, line.indexOf("</version>")), mavenPropertyVariable);
            if (!propertyBOMString.contains(pluginProperties.get(pluginArtifactId))){
                propertyBOMString += String.format(bomDependencyIndent, "", mavenProperty.replace("pluginVersion", pluginVersion)) + "\n";
            }
            pluginFound = false;
        }
        return line;
    }
}
