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
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BOMGenerator {

    public void buildBomFile(Map<DependencyInfo, Integer> rpmMap, List<DependencyInfo> artifactList, ArtifactGav currentISOGav, Map<String, String> pluginMap, Log log) throws MojoFailureException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        List<String> addedProperties = new ArrayList();
        try {
            docBuilder = builderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element mainRootElement = doc.createElementNS("http://maven.apache.org/POM/4.0.0", "project");

            mainRootElement.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                            "xs:schemaLocation",
                            "http://maven.apache.org/POM/4.0.0"  +
                            " http://maven.apache.org/xsd/maven-4.0.0.xsd");
            doc.appendChild(mainRootElement);

            mainRootElement.appendChild(getNode(doc, "modelVersion", "4.0.0"));
            mainRootElement.appendChild(getNode(doc, "groupId", currentISOGav.getGroupId()));
            mainRootElement.appendChild(getNode(doc, "artifactId", currentISOGav.getArtifactId()));
            mainRootElement.appendChild(getNode(doc, "version", currentISOGav.getVersion()));
            mainRootElement.appendChild(getNode(doc, "packaging", "pom"));

            Element properties = doc.createElement("properties");
            mainRootElement.appendChild(properties);
            Element dependencyManagement = doc.createElement("dependencyManagement");
            mainRootElement.appendChild(dependencyManagement);
            Element dependencies = doc.createElement("dependencies");
            dependencyManagement.appendChild(dependencies);
            for(Map.Entry<DependencyInfo, Integer> entry : rpmMap.entrySet()){
                if (entry.getValue() != 0){
                    dependencies.appendChild(doc.createComment("Content from " + entry.getKey().getArtifactID() + "-" 
                                                        + entry.getKey().getArtifactVersion()));
                }
                for (int i = 0; i < entry.getValue(); i++){
                    dependencies.appendChild(getDependency(doc, artifactList.get(0), properties, pluginMap, addedProperties));
                    artifactList.remove(0);
                }
            }

            setUpTransformer(doc, currentISOGav.getArtifactId() + ".xml");

        }catch (TransformerConfigurationException tce) {
            log.error("Transformer Factory error: " + tce.getMessage());
            throw new MojoFailureException(tce.getMessage());
        } catch (TransformerException te) {
            log.error("Transformation error: " + te.getMessage());
            throw new MojoFailureException(te.getMessage());
        } catch (ParserConfigurationException pce) {
            log.error("Parse error: " + pce.getMessage());
            throw new MojoFailureException(pce.getMessage());
        } catch (DOMException e) {
            log.error("DOM error" + e.getMessage());
            throw new MojoFailureException(e.getMessage());
        }
    }

    private Node getDependency(Document doc, DependencyInfo artifact, Node properties, Map<String, String> pluginMap, List<String> addedProperties) {
        Element dependency = doc.createElement("dependency");
        dependency.appendChild(getNode(doc, "groupId", artifact.getGroupID()));
        dependency.appendChild(getNode(doc, "artifactId", artifact.getArtifactID()));
        if (pluginMap.containsKey(artifact.getArtifactID())){           
            if (!addedProperties.contains(pluginMap.get(artifact.getArtifactID()))){
                properties.appendChild(getNode(doc, pluginMap.get(artifact.getArtifactID()), artifact.getArtifactVersion()));
                addedProperties.add(pluginMap.get(artifact.getArtifactID()));
            }
            dependency.appendChild(getNode(doc, "version", "${" + pluginMap.get(artifact.getArtifactID()) + "}"));   
        } else 
            dependency.appendChild(getNode(doc, "version", artifact.getArtifactVersion()));
        dependency.appendChild(getNode(doc, "type", artifact.getPackaging()));
        return dependency;
    }

    private Node getNode(Document doc, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

    private void setUpTransformer(Document doc, String filePath) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult console = new StreamResult(filePath);
        transformer.transform(source, console);
    }
}
