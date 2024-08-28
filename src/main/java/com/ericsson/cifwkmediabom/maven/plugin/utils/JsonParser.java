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

import com.ericsson.cifwkmediabom.maven.plugin.models.DependencyInfo;
import com.ericsson.cifwkmediabom.maven.plugin.models.ArtifactGav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {

    Map<DependencyInfo, Integer> rpmMap;
    Map<String, String> pluginMap = new HashMap();
    List<DependencyInfo> artifactList;

    public void parseJson(String input, String versionedPlugins, Log log) throws MojoFailureException {
        try {
            rpmMap = new LinkedHashMap();
            artifactList = new ArrayList();
            JSONArray allArtifacts;
            int counter = 0;

            JSONObject initialJSONObject = new JSONObject(input);
            JSONObject data = initialJSONObject.getJSONObject("data");
            ArtifactGav currentISOGav = new ArtifactGav(data.getString("groupId"),
                                                "ERICenm-iso-bom",
                                                data.getString("version"));
            JSONArray jsonArray = data.getJSONArray("rpms");
            JSONObject rpmContents;
            for(int i = 0; i < jsonArray.length(); i++){
                counter = 0;
                rpmContents = jsonArray.getJSONObject(i);
                allArtifacts = rpmContents.getJSONArray("contents");
                for (int j = 0; j < allArtifacts.length(); j++){
                    if (!artifactList.contains(parseArtifactContents(allArtifacts.getString(j)))){
                        artifactList.add(parseArtifactContents(allArtifacts.getString(j)));
                        counter++;
                    }
                }
                rpmMap.put(parseArtifactContents(rpmContents.getString("rpm")), counter);
            }
            pluginMap = parseVersionedPlugins(versionedPlugins);
            new BOMGenerator().buildBomFile(rpmMap, artifactList, currentISOGav, pluginMap, log);
        } catch (JSONException ex) {
            log.error("JSON Exception - " + ex.getMessage());
            throw new MojoFailureException(ex.getMessage());
        }
    }

    private DependencyInfo parseArtifactContents(String artifact){
        List <String> dependencyInfoList = Arrays.asList(artifact.split(":"));
        String groupID = dependencyInfoList.get(0);
        String artifactID = dependencyInfoList.get(1);
        String artifactVersion = dependencyInfoList.get(2);
        String type = dependencyInfoList.get(3);

        return new DependencyInfo(groupID, artifactID, artifactVersion, type);
    }

    public Map<String, String> parseVersionedPlugins(String versionedPlugins){
        JSONArray jsonArray = new JSONArray(versionedPlugins);
        for(int i = 0; i < jsonArray.length(); i++){
            pluginMap.put(jsonArray.getJSONObject(i).getString("name"),
                    jsonArray.getJSONObject(i).getString("property"));
        }
        return pluginMap;
    }

    public String createArtifactMappingJSONParam(String repo, Map<String, List<Dependency>> mapping){
        JSONObject baseObject = new JSONObject();
        JSONArray deliverableArray = new JSONArray();
        for(Map.Entry<String, List<Dependency>> deliverableName : mapping.entrySet()){
            JSONArray contents = new JSONArray();
            JSONObject deliverable = new JSONObject();
            for(Dependency dep : deliverableName.getValue()){
                JSONObject artifact = new JSONObject();
                artifact.put("name", dep.getArtifactId());
                contents.put(artifact);
            }
            deliverable.put("name", deliverableName.getKey());
            deliverable.put("artifacts", contents);
            deliverableArray.put(deliverable);
        }
        baseObject.put("git_repo", repo);
        baseObject.put("rpms", deliverableArray);
        return baseObject.toString();
    }
}
