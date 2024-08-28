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
import com.ericsson.cifwkmediabom.maven.plugin.utils.ModifiedPomXMLEventReader;
import com.ericsson.cifwkmediabom.maven.plugin.utils.PomHelper;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.stream.XMLInputFactory;
import org.codehaus.stax2.XMLInputFactory2;

/**
 * @goal update-plugin-versions
 * @phase compile
 * @requiresProject true
 */
public class MaintainPluginVersions extends AbstractMojo {
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter property="nexus.rest.url" default-value="https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/service/local/artifact/maven/redirect"
     * @readonly
     */
    private String nexusRestURL;

    /**
     * @parameter property="bom.groupId" default-value="com.ericsson.oss"
     * @readonly
     */
    private String isoGroupId;

    /**
     * @parameter property="bom.artifact" default-value="ERICenm-iso-bom"
     * @readonly
     */
    private String isoArtifactId;

    /**
     * @parameter property="nexus.repo" default-value="releases"
     * @readonly
     */
    private String nexusRepoId;

    private String bomVersion;
    private List<NameValuePair> params;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        bomVersion = project.getProperties().getProperty("version.enm.iso.bom");
        params = new ArrayList();
        setRestParams();
        try {
            StringBuilder input = PomHelper.readXmlFile(project.getFile());
            ModifiedPomXMLEventReader newPom = newModifiedPomXER(input);
            String remotePom = new GenericRestCalls().setUpGETRestCall(params, nexusRestURL, getLog());
            Document doc = parseStringToXMLDOM(remotePom);
            NodeList bomProperties = doc.getElementsByTagName("properties").item(0).getChildNodes();
            Properties properties = PomHelper.getRawModel(project).getProperties();
            for (int i = 0; i < bomProperties.getLength(); i++){
                if (bomProperties.item(i).getNodeName().startsWith("plugin")){
                    compareProperties(bomProperties.item(i), properties, newPom);
                }
            }
            writeFile(project.getFile(), input);
        } catch (SAXException | IOException | TransformerException | ParserConfigurationException ex) {
            getLog().error(ex.getMessage());
            throw new MojoFailureException(ex.getMessage());
        }
    }

    private void setRestParams(){
        params.add(new BasicNameValuePair("r", nexusRepoId));
        params.add(new BasicNameValuePair("g", isoGroupId));
        params.add(new BasicNameValuePair("a", isoArtifactId));
        params.add(new BasicNameValuePair("v", bomVersion));
        params.add(new BasicNameValuePair("p", "pom"));
    }

    private Document parseStringToXMLDOM(String xmlString) throws SAXException, IOException, TransformerException, ParserConfigurationException{
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlString));
        return db.parse(is);
    }

    private void compareProperties(Node bomProperty, Properties projectProperties, ModifiedPomXMLEventReader newPom){
        if(projectProperties.containsKey(bomProperty.getNodeName())){
            if(!projectProperties.getProperty(bomProperty.getNodeName()).equals(bomProperty.getNodeValue())){
                try{
                    PomHelper.setPropertyVersion(newPom, null, bomProperty.getNodeName(), bomProperty.getTextContent());
                } catch(NullPointerException np){
                    getLog().error("Null Pointer: " + np.getMessage());
                } catch (XMLStreamException ex) {
                    getLog().error(ex.getMessage());
                }
            }
        }
    }

    protected final ModifiedPomXMLEventReader newModifiedPomXER(StringBuilder input) {
        ModifiedPomXMLEventReader newPom = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
            inputFactory.setProperty( XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE );
            newPom = new ModifiedPomXMLEventReader( input, inputFactory);
        } catch (XMLStreamException e){
            getLog().error(e);
        }
        return newPom;
    }

    protected final void writeFile(File outFile, StringBuilder input) throws IOException {
        Writer writer = WriterFactory.newXmlWriter(outFile);
        try {
            IOUtil.copy(input.toString(), writer);
        } finally {
            IOUtil.close(writer);
        }
    }
}

