package com.ericsson.cifwkmediabom.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.ericsson.cifwkmediabom.maven.plugin.utils.FileHandling;
import com.ericsson.cifwkmediabom.maven.plugin.utils.GenericRestCalls;

/**
 * @goal publishprojectbomdelta
 * @phase deploy
 * @requiresProject true
 */
public class PublishProjectArtifactVersionBOMDeltas extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * @parameter default-value="${reactorProjects}"
     * @readonly
     */
    @SuppressWarnings("rawtypes")
    private List reactorProjects;

    /**
     * Base directory of the project.
     * @parameter default-value="${basedir}"
     */
    private File basedir;
    
    /**
     * @parameter property="projectDeltaFile" default-value="target/depDiffs.txt"
    */
    @Parameter
    private String projectDeltaFile;

    /**
     * @parameter property="cifwkRESTAPIURL" default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/api/"
    */
    @Parameter
    private String cifwkRESTAPIURL;

    private static List<String> dependencyDelta = new ArrayList<String>();
    private static List<String> noDependencyDeltas = new ArrayList<String>();
    private final String pattern = "ERIC(.*)_CXP(.*)";
    private final String xpattern = "EXTR(.*)_CXP(.*)";
    private static String deliverableArtifact = "";
    private String repoName;
    private String response;
    private boolean finalProject = false;
    private boolean root = false;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String product = project.getProperties().getProperty("product");
        String intendedDrop = project.getProperties().getProperty("delivery.drop");
        try{
            getLog().debug(" ********** Check if this is the projects execution root ********** " + root);
            root = isThisTheExecutionRoot();
            getLog().debug(" ********** Ended checking if this is the projects execution root: ********** ");

            getLog().debug(" ********** Beginnning Getting Projects Repo Name ********** ");
            repoName = getProjectsSCMRepoName(root);
            getLog().debug(" ********** Finished  Getting Projects Repo Name: ********** " + repoName);

            getLog().debug(" ********** Beginnning Looking for ERIC/EXTR module in Project if one exists ********** ");
            deliverableArtifact = getArtifactBuildModule(project);
            getLog().debug(" ********** Finished Looking for ERIC/EXTR module in Project if one exists: ********** " + deliverableArtifact);

            getLog().debug(" ********** Beginnning Parsing Project Dependency to BOM Dependency Version File ********** ");
            dependencyDelta = parseAndGetArtifactDeltasFromFile(deliverableArtifact, product, intendedDrop, repoName);
            getLog().debug(" ********** Ended Parsing Project Dependency to BOM Dependency Version File: ********** " + dependencyDelta);

            getLog().debug(" ********** Check if Delta Artfacts where found and if so build project details into a list ********** ");
            noDependencyDeltas = getProjectDetailsIfNoDeltasFound(dependencyDelta, deliverableArtifact, product, intendedDrop, repoName);
            getLog().debug(" ********** Ended Check if Delta Artfacts where found and if so build project details into a list: ********** " + noDependencyDeltas);

            getLog().debug(" ********** Check if this is the last module in the projects build ********** ");
            finalProject = finalModuleExecutions();
            getLog().debug(" ********** Finished checking if this is the last module in the projects build: ********** " + finalProject);

        } catch(MojoExecutionException mojoExecutionError){
            getLog().error(mojoExecutionError.toString());
        } catch(MojoFailureException mojoFailureError){
            getLog().error(mojoFailureError.toString());
        } catch(IOException IOError){
            getLog().error(IOError.toString());
        }
    }

    public boolean isThisTheExecutionRoot() throws MojoExecutionException, MojoFailureException{
        getLog().debug(" ********** Root Folder:" + mavenSession.getExecutionRootDirectory() + " ********** ");
        getLog().debug(" **********  Current Folder:"+ basedir + " ********** ");
        root = mavenSession.getExecutionRootDirectory().equalsIgnoreCase(basedir.toString());
        return root;
    }

    public String getProjectsSCMRepoName(boolean root) throws MojoExecutionException, MojoFailureException{
        String repoName;
        if(root){
            String scmInfo [] = project.getScm().getConnection().split(":");
            repoName = scmInfo[scmInfo.length-1].toString().replaceAll("[0-9]+\\/", "");
            
        } else {
            repoName = "OSS" + "/" + project.getParent().getGroupId() + "/" + project.getParent().getArtifactId();
        }
        return repoName;
    }

    public String getArtifactBuildModule(MavenProject project) throws MojoExecutionException, MojoFailureException{
        @SuppressWarnings("unchecked")
        List<String> projectModules = project.getModules();
        for(String module : projectModules){
            if(module.matches(pattern) || module.matches(xpattern)){
                deliverableArtifact = module;
                break;
            }
        }
        return deliverableArtifact;
    }

    public List<String> parseAndGetArtifactDeltasFromFile(String deliverableArtifact, String product, String intendedDrop, String repoName) throws MojoExecutionException, MojoFailureException, IOException{
        getLog().debug(" ********** Beginnning Parsing Project Dependency to BOM Dependency Version File ********** ");
        File filein = new File(basedir.toString() + "/" + projectDeltaFile);
        BufferedReader reader = new BufferedReader(new FileReader(filein));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if(line.startsWith("com")){
                line = line.replace("->", " ").replace(" ", "....").replaceAll("\\.\\.+",":");
                if (!deliverableArtifact.isEmpty()) {
                    line = line + ":" + deliverableArtifact + ":" + product + ":" + intendedDrop + ":" + repoName;
                } else {
                    line = line + "::" + product + ":" + intendedDrop + ":" + repoName;
                }
                if (!dependencyDelta.contains(line)) {
                    dependencyDelta.add(line);
                }
            }
        }
        reader.close();
        getLog().debug(" ********** Ended Parsing Project Dependency to BOM Dependency Version File ********** ");
        return dependencyDelta;
    }

    public List<String> getProjectDetailsIfNoDeltasFound(List<String> dependencyDelta, String deliverableArtifact, String product, String intendedDrop, String repoName) throws MojoExecutionException, MojoFailureException{
        if(dependencyDelta.isEmpty()){
            String result;
            if(!deliverableArtifact.isEmpty()){
                result = product + ":" + intendedDrop + ":" + repoName + ":" + deliverableArtifact;
            }else{
                result = product + ":" + intendedDrop + ":" + repoName + ":";
            }
            if (!noDependencyDeltas.contains(result)) {
                noDependencyDeltas.add(result);
            }
        }
        return noDependencyDeltas;
    }

    public Boolean finalModuleExecutions() throws MojoExecutionException, MojoFailureException, IOException{
        final int size = reactorProjects.size();
        MavenProject lastProject = (MavenProject) reactorProjects.get(size - 1);
        if (lastProject == mavenSession.getCurrentProject()) {
            finalProject = true;
            getLog().debug(" ********** Beginnning Posting Project Dependency to BOM Dependencies to the Database ********** ");
            response = postDependencyDeltas(dependencyDelta, noDependencyDeltas);
            getLog().debug(" ********** Ended Posting Project Dependency to BOM Dependencies to the Database, response: ********** " + response);
        }
        return finalProject;
    }

    public String postDependencyDeltas(List<String> dependencyDelta, List<String> noDependencyDeltas) throws MojoExecutionException, MojoFailureException, IOException{
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        if(!dependencyDelta.isEmpty()){
            String delta = dependencyDelta.toString().replaceAll("[\\[\\]]", "").replaceAll(" ", "");
            getLog().debug(" ********** Posting Project Dependency to BOM Dependencies to the Database ********** " + delta);
            params.add(new BasicNameValuePair("mismatchList", delta));
            response = new GenericRestCalls().setUpPOSTRestCall(params, cifwkRESTAPIURL + "processVersionMismatches/", getLog());
        } else if (!noDependencyDeltas.isEmpty()) {
            String empty = noDependencyDeltas.toString().replaceAll("[\\[\\]]", "").replaceAll(" ", "");
            getLog().debug(" ********** Posting Project Dependency to BOM Dependencies to the Database ********** " + empty);
            params.add(new BasicNameValuePair("emptyList", empty));
            response = new GenericRestCalls().setUpPOSTRestCall(params, cifwkRESTAPIURL + "processVersionMismatches/", getLog());
        }
        return response;
    }

}
