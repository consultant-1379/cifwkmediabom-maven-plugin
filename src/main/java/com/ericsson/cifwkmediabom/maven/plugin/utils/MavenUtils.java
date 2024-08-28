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
package com.ericsson.cifwkmediabom.maven.plugin.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author eseabyr
 */

public class MavenUtils {

    public boolean isRoot(MavenSession mavenSession, File basedir){
        return mavenSession.getExecutionRootDirectory().equalsIgnoreCase(basedir.toString());
    }

    public String getProjectRepo(MavenProject project) throws MojoExecutionException, MojoFailureException{
        String scmUrl = project.getProperties().getProperty("ericsson.scm.url");
        return scmUrl.substring(scmUrl.indexOf("29418"), scmUrl.indexOf("[push=]")).replaceAll("[0-9]+\\/", "");
    }

    public List<String> getPackageModules(List<String> projectModules, String pattern) throws MojoExecutionException, MojoFailureException{
        List<String> deliverableArtifact = new ArrayList();
        for(String module : projectModules){
            if(module.matches(pattern))
                deliverableArtifact.add(module);
        }
        return deliverableArtifact;
    }

    public Boolean isLastModule(List<MavenProject> reactorProjects, MavenSession mavenSession) throws MojoExecutionException, MojoFailureException{
        boolean finalProject = false;
        MavenProject lastProject = (MavenProject) reactorProjects.get(reactorProjects.size() - 1);
        if (lastProject == mavenSession.getCurrentProject())
            finalProject = true;
        return finalProject;
    }
}
