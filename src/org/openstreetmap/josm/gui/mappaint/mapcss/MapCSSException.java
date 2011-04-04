// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

public class MapCSSException extends RuntimeException {

    public MapCSSException() {
        super();
    }

    public MapCSSException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapCSSException(String message) {
        super(message);
    }

    public MapCSSException(Throwable cause) {
        super(cause);
    }
}
