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
import com.ericsson.cifwkmediabom.maven.plugin.utils.MavenUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal artifactmapping
 * @phase install
 * @requiresProject true
 */
public class ArtifactMapping extends AbstractMojo {

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
    public MavenSession mavenSession;

    /**
     * Base directory of the project.
     * @parameter default-value="${basedir}"
     */
    private File basedir;

    /**
     * @parameter property="cifwkGetDropContentsRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/api/createPackageArtifactMapping/"
     */
    private String restUrl;

    /**
     * @parameter default-value="${reactorProjects}"
     * @readonly
     */
    public List reactorProjects;

    private static List<String> modules;
    private static List<String> deliverables;
    private static final Map<String, List<Dependency>> dependencyMap;
    private static final Map<String, List<Dependency>> deliverableMap;
    private static String repo;
    private static final String rpmPattern;
    private static final MavenUtils mavenUtils;
    private List<Dependency> artifacts;

    static {
        rpmPattern = "E[RX][IT][CR].*_CXP[0-9]{7}";
        mavenUtils = new MavenUtils();
        dependencyMap = new HashMap();
        deliverableMap = new HashMap();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException{
        try {
            if(mavenUtils.isRoot(mavenSession, basedir)){
                modules = project.getModules();
                repo = mavenUtils.getProjectRepo(project);
                deliverables = mavenUtils.getPackageModules(modules, rpmPattern);
            }

            getModuleDependencies(project.getArtifactId(), project.getDependencies());

            if(mavenUtils.isLastModule(reactorProjects, mavenSession)){
                createDeliverableMap();
                String restParam = new JsonParser().createArtifactMappingJSONParam(repo, deliverableMap);
                new GenericRestCalls().setUpPOSTRestCallJSON(restParam, restUrl, getLog());
            }
        } catch (MojoExecutionException | MojoFailureException error) {
            getLog().error(error.getMessage());
        }
    }

    private void createDeliverableMap(){
        for (String deliverable : deliverables){
            artifacts = new ArrayList();
            artifacts.addAll(dependencyMap.get(deliverable));
            buildArtifactDependencyList(artifacts);
            removeDuplicates();
            deliverableMap.put(deliverable, artifacts);
        }
    }

    private void getModuleDependencies(String module, List<Dependency> dependencies){
        Iterator<Dependency> iter = dependencies.iterator();
        while (iter.hasNext()){
            if(!modules.contains(iter.next().getArtifactId()))
                iter.remove();
        }
        if(!dependencies.isEmpty())
            dependencyMap.put(module, dependencies);
    }

    private void buildArtifactDependencyList(List<Dependency> artifacts){
        if(artifacts == null){
            return;
        } else {
            Dependency[] localArray = artifacts.toArray(new Dependency[artifacts.size()]);
            for (Dependency artifact : localArray){
                if(dependencyMap.get(artifact.getArtifactId()) != null){
                    this.artifacts.addAll(dependencyMap.get(artifact.getArtifactId()));
                    buildArtifactDependencyList(dependencyMap.get(artifact.getArtifactId()));
                }
            }
        }
    }

    private void removeDuplicates(){
        Map<String, Dependency> map = new LinkedHashMap<>();
        for (Dependency dep : artifacts)
            map.put(dep.getArtifactId(), dep);
        artifacts.clear();
        artifacts.addAll(map.values());
    }
}
