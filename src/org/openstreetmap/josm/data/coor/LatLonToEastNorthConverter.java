// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

/**
 * Class that converts lat/lon to east/north.
 */
public interface LatLonToEastNorthConverter {
    /**
     * Convert from lat/lon to easting/northing. This method uses the newer {@link ILatLon} interface.
     *
     * @param ll the geographical point to convert (in WGS84 lat/lon)
     * @return the corresponding east/north coordinates
     * @since 12161
     */
    EastNorth latlon2eastNorth(ILatLon ll);

    /**
     * Gets the object used as cache identifier when caching results of this conversion.
     * @return The object to use as cache key
     * @since 10827
     */
    Object getCacheKey();
}
