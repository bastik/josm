// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Locale;

/**
 *
 * @since xxx
 */
public enum Platform {

    UNIXOID {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitUnixoid();
        }
    },
    WINDOWS {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitWindows();
        }
    },
    OSX {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitOsx();
        }
    };

    public abstract <T> T accept(PlatformVisitor<T> visitor);

    /**
     * Identifies the current operating system family.
     * @return the the current operating system family
     */
    public static Platform determinePlatform() {
        String os = System.getProperty("os.name");
        if (os == null) {
            Logging.warn("Your operating system has no name, so I'm guessing its some kind of *nix.");
            return Platform.UNIXOID;
        } else if (os.toLowerCase(Locale.ENGLISH).startsWith("windows")) {
            return Platform.WINDOWS;
        } else if ("Linux".equals(os) || "Solaris".equals(os) ||
                "SunOS".equals(os) || "AIX".equals(os) ||
                "FreeBSD".equals(os) || "NetBSD".equals(os) || "OpenBSD".equals(os)) {
            return Platform.UNIXOID;
        } else if (os.toLowerCase(Locale.ENGLISH).startsWith("mac os x")) {
            return Platform.OSX;
        } else {
            Logging.warn("I don't know your operating system '"+os+"', so I'm guessing its some kind of *nix.");
            return Platform.UNIXOID;
        }
    }

}
