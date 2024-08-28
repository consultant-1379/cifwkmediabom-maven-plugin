package com.ericsson.cifwkmediabom.maven.plugin.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import java.util.regex.Pattern;

public class PomHelper{

    public static Model getRawModel( MavenProject project ) throws IOException {
        return getRawModel(project.getFile());
    }

    public static Model getRawModel(File moduleProjectFile) throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(moduleProjectFile);
            bufferedReader = new BufferedReader(fileReader);
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(bufferedReader);
        } catch (XmlPullParserException ex) {
            throw new IOException(ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    public static boolean setPropertyVersion(final ModifiedPomXMLEventReader pom, final String profileId,
                                              final String property, final String value) throws XMLStreamException {
        Stack<String> stack = new Stack<String>();
        String path = "";
        final Pattern propertyRegex;
        final Pattern matchScopeRegex;
        final Pattern projectProfileId;
        boolean inMatchScope = false;
        boolean madeReplacement = false;
        if (profileId == null) {
            propertyRegex = Pattern.compile( "/project/properties/" + property);
            matchScopeRegex = Pattern.compile( "/project/properties" );
            projectProfileId = null;
        } else {
            propertyRegex = Pattern.compile( "/project/profiles/profile/properties/" + property);
            matchScopeRegex = Pattern.compile( "/project/profiles/profile" );
            projectProfileId = Pattern.compile( "/project/profiles/profile/id" );
        }
        pom.rewind();
        while (pom.hasNext()){
            XMLEvent event = pom.nextEvent();
            if (event.isStartElement()){
                stack.push(path);
                path = path + "/" + event.asStartElement().getName().getLocalPart();
                if (propertyRegex.matcher(path).matches()){
                    pom.mark(0);
                }
                else if (matchScopeRegex.matcher(path).matches()){
                    inMatchScope = profileId == null;
                    pom.clearMark(0);
                    pom.clearMark(1);
                }
                else if (profileId != null && projectProfileId.matcher(path).matches()){
                    String candidateId = pom.getElementText();
                    inMatchScope = profileId.trim().equals( candidateId.trim() );
                }
            }
            if (event.isEndElement()){
                if (propertyRegex.matcher(path).matches()){
                    pom.mark(1);
                }
                else if (matchScopeRegex.matcher(path).matches()){
                    if (inMatchScope && pom.hasMark(0) && pom.hasMark(1)){
                        pom.replaceBetween(0, 1, value);
                        madeReplacement = true;
                    }
                    pom.clearMark(0);
                    pom.clearMark(1);
                    inMatchScope = false;
                }
                path = stack.pop();
            }
        }
        return madeReplacement;
    }

    public static StringBuilder readXmlFile(File outFile) throws IOException{
        Reader reader = ReaderFactory.newXmlReader(outFile);
        try {
            return new StringBuilder(IOUtil.toString(reader));
        } finally{
            IOUtil.close(reader);
        }
    }
}
