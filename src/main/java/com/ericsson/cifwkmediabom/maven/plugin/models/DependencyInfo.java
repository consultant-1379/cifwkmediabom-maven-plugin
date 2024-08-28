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
package com.ericsson.cifwkmediabom.maven.plugin.models;

public class DependencyInfo {

    private final String groupID;
    private final String artifactID;
    private final String artifactVersion;
    private final String packaging;

    public DependencyInfo(final String groupID, final String artifactID,
            final String artifactVersion, final String packaging) {
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.artifactVersion = artifactVersion;
        this.packaging = packaging;
    }

    public String getGroupID() {
        return groupID;
    }

    public String getArtifactID() {
        return artifactID;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public String getPackaging() {
        return packaging;
    }

    @Override
    public boolean equals(Object objectToCompare) {

        if ((objectToCompare == null) || (objectToCompare.getClass() != this.getClass()))
            return false;
        DependencyInfo dependency = (DependencyInfo) objectToCompare;
        return ((groupID.equals(dependency.groupID))
                && (artifactID.equals(dependency.artifactID))
                && (packaging.equals(dependency.packaging)));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.groupID != null ? this.groupID.hashCode() : 0);
        hash = 79 * hash + (this.artifactID != null ? this.artifactID.hashCode() : 0);
        hash = 79 * hash + (this.packaging != null ? this.packaging.hashCode() : 0);
        return hash;
    }
}
