/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.cifwkmediabom.maven.plugin.utils;

/**
 *
 * @author esheejo
 */
public class ExtractRepo {
    
    private String scmUrl;
    private String repo;
    
    public String extractRepo(String scmUrl){
        
        this.scmUrl=scmUrl;
        repo = scmUrl.substring(scmUrl.indexOf("OSS"), scmUrl.indexOf("[push=]"));
//        System.out.println("REPO URL "+repo);
        return repo;
    }
}
