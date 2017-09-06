// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.InputStream;

/**
 *
 * @since xxx
 */
public interface NTV2GridShiftFileSource {
    InputStream getNTV2GridShiftFile(String gridFileName);
}
