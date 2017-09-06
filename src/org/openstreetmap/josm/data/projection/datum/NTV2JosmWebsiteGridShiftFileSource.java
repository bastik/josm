// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Logging;

/**
 *
 * @since xxx
 */
public class NTV2JosmWebsiteGridShiftFileSource implements NTV2GridShiftFileSource {

    private NTV2JosmWebsiteGridShiftFileSource() {
        // hide constructor
    }

    // lazy initialization
    private static class InstanceHolder {
        static final NTV2JosmWebsiteGridShiftFileSource INSTANCE = new NTV2JosmWebsiteGridShiftFileSource();
    }

    public static NTV2JosmWebsiteGridShiftFileSource getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public InputStream getNTV2GridShiftFile(String gridFileName) {
        String location = Main.getJOSMWebsite() + "/proj/" + gridFileName;
        // Try to load grid file
        CachedFile cf = new CachedFile(location);
        try {
            return cf.getInputStream();
        } catch (IOException ex) {
            Logging.warn(ex);
            return null;
        }
    }
}
