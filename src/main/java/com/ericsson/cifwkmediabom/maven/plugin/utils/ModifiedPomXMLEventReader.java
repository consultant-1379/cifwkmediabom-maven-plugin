package com.ericsson.cifwkmediabom.maven.plugin.utils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;

public class ModifiedPomXMLEventReader implements XMLEventReader {

    private static final int MAX_MARKS = 3;
    private final StringBuilder pom;
    private boolean modified = false;
    private final XMLInputFactory factory;
    private int nextStart = 0;
    private int nextEnd = 0;
    private int[] markStart = new int[MAX_MARKS];
    private int[] markEnd = new int[MAX_MARKS];
    private int[] markDelta = new int[MAX_MARKS];
    private int lastStart = -1;
    private int lastEnd;
    private int lastDelta = 0;
    private XMLEvent next = null;
    private int nextDelta = 0;
    private XMLEventReader backing;

    public ModifiedPomXMLEventReader(StringBuilder pom, XMLInputFactory factory) throws XMLStreamException {
        this.pom = pom;
        this.factory = factory;
        rewind();
    }

    public void rewind() throws XMLStreamException {
        backing = factory.createXMLEventReader(new StringReader(pom.toString()));
        nextEnd = 0;
        nextDelta = 0;
        for (int i = 0; i < MAX_MARKS; i++){
            markStart[i] = -1;
            markEnd[i] = -1;
            markDelta[i] = 0;
        }
        lastStart = -1;
        lastEnd = -1;
        lastDelta = 0;
        next = null;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        try {
            return next;
        } finally {
            next = null;
            lastStart = nextStart;
            lastEnd = nextEnd;
            lastDelta = nextDelta;
        }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        return backing.peek();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return backing.getElementText();
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        while (hasNext()) {
            XMLEvent e = nextEvent();
            if (e.isCharacters() && !((Characters) e).isWhiteSpace()) {
                throw new XMLStreamException( "Unexpected text" );
            }
            if (e.isStartElement() || e.isEndElement()) {
                return e;
            }
        }
        throw new XMLStreamException( "Unexpected end of Document" );
    }

    @Override
    public Object getProperty(String name) {
        return backing.getProperty(name);
    }
    
    @Override
    public void close() throws XMLStreamException {
        backing.close();
        next = null;
        backing = null;
    }

    public StringBuilder asStringBuilder() {
        return new StringBuilder( pom.toString() );
    }

    public void clearMark(int index) {
        markStart[index] = -1;
    }
       
    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        if (!backing.hasNext()) {
            return false;
        }
        try {
            next = backing.nextEvent();
            nextStart = nextEnd;
            if (backing.hasNext()) {
                nextEnd = backing.peek().getLocation().getCharacterOffset();
            }
            if (nextEnd != -1) {
                if (!next.isCharacters()) {
                    while (nextStart < nextEnd && nextStart < pom.length() &&
                                (c( nextStart ) == '\n' || c( nextStart ) == '\r' )) {
                        nextStart++;
                    }
                } else {
                    while (nextEndIncludesNextEvent() || nextEndIncludesNextEndElement()) {
                        nextEnd--;
                    }
                }
            }
            return nextStart < pom.length();
        } catch (XMLStreamException e) {
            return false;
        }
    }

    public void mark(int index) {
        markStart[index] = lastStart;
        markEnd[index] = lastEnd;
        markDelta[index] = lastDelta;
    }

    private boolean nextEndIncludesNextEndElement() {
        return (nextEnd > nextStart + 2 && nextEnd - 2 < pom.length() && c( nextEnd - 2 ) == '<');
    }

    private boolean nextEndIncludesNextEvent() {
        return nextEnd > nextStart + 1 && nextEnd - 2 < pom.length() &&
            (c( nextEnd - 1 ) == '<' || c( nextEnd - 1 ) == '&');
    }

    private char c(int index) {
        return pom.charAt(nextDelta + index);
    }

    public boolean hasMark(int index) {
        return markStart[index] != -1;
    }

    public void replaceBetween(int index1, int index2, String replacement){
        if (!hasMark( index1 ) || !hasMark( index2 ) || markStart[index1] > markStart[index2]) {
            throw new IllegalStateException();
        }
        int start = markDelta[index1] + markEnd[index1];
        int end = markDelta[index2] + markStart[index2];
        if (replacement.equals(pom.substring(start, end))) {
            return;
        }
        pom.replace( start, end, replacement );
        int delta = replacement.length() - ( end - start );
        nextDelta += delta;

        for (int i = 0; i < MAX_MARKS; i++) {
            if (i == index1 || i == index2 || markStart[i] == -1)
                continue;
            if (markStart[i] > markStart[index2])
                markDelta[i] += delta;
            else if (markStart[i] == markEnd[index1] && markEnd[i] == markStart[index1])
                markDelta[i] += delta;
            else if (markStart[i] > markEnd[index1] || markEnd[i] < markStart[index2])
                markStart[i] = -1;
        }
        modified = true;
    }
}
