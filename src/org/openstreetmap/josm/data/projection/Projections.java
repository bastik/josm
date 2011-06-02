// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Class to handle projections
 *
 */
public class Projections {
    
    private static Map<LatLon, EastNorth> cache = new HashMap<LatLon, EastNorth>();

    public static EastNorth get(LatLon ll) {
        EastNorth en = cache.get(ll);
        if (en == null) {
            en = Main.proj.latlon2eastNorth(ll);
            cache.put(ll, en);
        }
        return en;
    }
    
    public static EastNorth get(double lat, double lon) {
        return get(new LatLon(lat, lon));
    }
    
    /**
     * List of all available projections.
     */
    private static ArrayList<Projection> allProjections =
    new ArrayList<Projection>(Arrays.asList(new Projection[] {
        // global projections
        new Epsg4326(),
        new Mercator(),
        new UTM(),
        // regional - alphabetical order by country name
        new LambertEST(), // Still needs proper default zoom
        new Lambert(),    // Still needs proper default zoom
        new LambertCC9Zones(),    // Still needs proper default zoom
        new UTM_France_DOM(),
        new TransverseMercatorLV(),
        new Puwg(),
        new Epsg3008(), // SWEREF99 13 30
        new SwissGrid(),
    }));

    public static ArrayList<Projection> getProjections() {
        return allProjections;
    }

    /**
     * Adds a new projection to the list of known projections.
     * 
     * For Plugins authors: make sure your plugin is an early plugin, i.e. put
     * Plugin-Early=true in your Manifest.
     */
    public static void addProjection(Projection proj) {
        allProjections.add(proj);
    }
}
