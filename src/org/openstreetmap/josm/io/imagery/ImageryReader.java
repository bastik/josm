// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ImageryReader {

    private InputSource inputSource;

    private final static boolean debug = true;

    private enum State { INIT, IMAGERY, ENTRY, ENTRY_ATTRIBUTE }

    public ImageryReader(InputStream source) throws IOException {
        this.inputSource = new InputSource(UTFInputStreamReader.create(source, "UTF-8"));
    }

    public List<ImageryInfo> parse() throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newSAXParser().parse(inputSource, parser);
            return parser.entries;
        } catch (SAXException e) {
            throw e;
        } catch (ParserConfigurationException e) {
            e.printStackTrace(); // broken SAXException chaining
            throw new SAXException(e);
        }
    }


    private class Parser extends DefaultHandler {
        private StringBuffer accumulator = new StringBuffer();

        private Stack<State> states;

        List<ImageryInfo> entries;

        /**
         * When entering an unknown element, don't try to recognize
         * the inner content. Just keep track of the nesting depth and
         * go on when the unknown element is finished.
         */
        int unknownLevel;
        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        boolean skipEntry;

        ImageryInfo entry;
        Bounds bounds;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            unknownLevel = 0;
            skipEntry = false;
            states = new Stack<State>();
            states.push(State.INIT);
            entries = new ArrayList<ImageryInfo>();
            entry = null;
            bounds = null;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (debug) System.err.println("<"+qName+">"+(unknownLevel > 0 ? "["+unknownLevel+"]" : ""));

            accumulator.setLength(0);
            State newState = null;
            known:{
                if (unknownLevel > 0) {
                    break known;
                } else {
                    switch (states.peek()) {
                        case INIT:
                            if (qName.equals("imagery")) {
                                newState = State.IMAGERY;
                            } else {
                                break known;
                            }
                            break;
                        case IMAGERY:
                            if (qName.equals("entry")) {
                                entry = new ImageryInfo();
                                skipEntry = false;
                                newState = State.ENTRY;
                            } else {
                                break known;
                            }
                            break;
                        case ENTRY:
                            if (Arrays.asList(new String[] {
                                "name",
                                "type",
                                "default",
                                "url",
                                "eula",
                                "min-zoom",
                                "max-zoom",
                                "attribution-text",
                                "attribution-url",
                                "logo-image",
                                "logo-url",
                                "terms-of-use-text",
                                "terms-of-use-url",
                            }).contains(qName)) {
                                newState = State.ENTRY_ATTRIBUTE;
                            } else if (qName.equals("bounds")) {
                                try {
                                    bounds = new Bounds(
                                            atts.getValue("min-lat") + "," +
                                            atts.getValue("min-lon") + "," +
                                            atts.getValue("max-lat") + "," +
                                            atts.getValue("max-lon"), ",");
                                } catch (IllegalArgumentException e) {
                                    break known;
                                }
                                newState = State.ENTRY_ATTRIBUTE;
                            } else {
                                break known;
                            }
                            break;
                        default:
                            break known;
                    }
                }
                if (newState == null) throw new AssertionError();
                states.push(newState);
                return;
            }
            unknownLevel++;
            if (equal(atts.getValue("mandatory"), "true")) {
                skipEntry = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            accumulator.append(ch, start, length);
        }

        @Override
        public void endElement(String namespaceURI, String qName, String rqName) {
            if (debug) System.err.println("</"+qName+">"+(unknownLevel > 0 ? "["+unknownLevel+"]" : ""));

            if (unknownLevel > 0) {
                unknownLevel--;
            } else {
                switch (states.pop()) {
                case INIT:
                    throw new RuntimeException();
                case IMAGERY:
                    break;
                case ENTRY:
                    if (qName.equals("entry")) {
                        if (!skipEntry) {
                            entries.add(entry);
                        }
                        entry = null;
                    }
                    break;
                case ENTRY_ATTRIBUTE:
                    if (qName.equals("name")) {
                        entry.setName(accumulator.toString());
                    } else if (qName.equals("type")) {
                        boolean found = false;
                        for (ImageryType type : ImageryType.values()) {
                            if (equal(accumulator.toString(), type.getUrlString())) {
                                entry.setImageryType(type);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            skipEntry = true;
                        }
                    } else if (qName.equals("default")) {
                        if (accumulator.toString().equals("true")) {
                            entry.setDefaultEntry(true);
                        } else if (accumulator.toString().equals("false")) {
                            entry.setDefaultEntry(false);
                        } else {
                            skipEntry = true;
                        }
                    } else if (qName.equals("url")) {
                        entry.setUrl(accumulator.toString());
                    } else if (qName.equals("eula")) {
                        entry.setEulaAcceptanceRequired(accumulator.toString());
                    } else if (qName.equals("min-zoom") || qName.equals("max-zoom")) {
                        Integer val = null;
                        try {
                            val = Integer.parseInt(accumulator.toString());
                        } catch(NumberFormatException e) {
                            val = null;
                        }
                        if (val == null) {
                            skipEntry = true;
                        } else {
                            if (qName.equals("min-zoom")) {
                                entry.setDefaultMinZoom(val);
                            } else {
                                entry.setDefaultMaxZoom(val);
                                entry.setMaxZoom(val);
                            }
                        }
                    } else if (qName.equals("bounds")) {
                        entry.setBounds(bounds);
                        bounds = null;
                    } else if (qName.equals("attribution-text")) {
                        entry.setAttributionText(accumulator.toString());
                    } else if (qName.equals("attribution-url")) {
                        entry.setAttributionLinkURL(accumulator.toString());
                    } else if (qName.equals("logo-image")) {
                        entry.setAttributionImage(accumulator.toString());
                    } else if (qName.equals("logo-url")) {
                        // FIXME
                    } else if (qName.equals("terms-of-use-text")) {
                        // FIXME
                    } else if (qName.equals("terms-of-use-url")) {
                        entry.setTermsOfUseURL(accumulator.toString());
                    }
                    break;
                }
            }
        }
    }
}
