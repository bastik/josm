// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 *
 * @since xxx
 */
public interface PlatformVisitor<T> {
    T visitUnixoid();
    T visitWindows();
    T visitOsx();
}
