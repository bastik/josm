// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.LinkSelector;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class Environment {

    public OsmPrimitive osm;
    /**
     * If not null, this is the matching parent object if an condition or an expression
     * is evaluated in a {@link LinkSelector}
     */
    public OsmPrimitive parent;
    public MultiCascade mc;
    public String layer;
    public StyleSource source;
    private Context context = Context.PRIMITIVE;

    /**
     * <p>When matching a child selector, the matching referrers will be stored.
     * They can be accessed in {@link Instruction#execute(Environment)} to access
     * tags from parent objects.</p>
     */
    private List<OsmPrimitive> matchingReferrers = null;

    /**
     * Creates a new uninitialized environment
     */
    public Environment() {}

    public Environment(OsmPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
        this.source = source;
    }

    /**
     * Creates a clone of the environment {@code other}
     * 
     * @param other the other environment. Must not be null.
     */
    public Environment(Environment other) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(other);
        this.osm = other.osm;
        this.mc = other.mc;
        this.layer = other.layer;
        this.parent = other.parent;
        this.source = other.source;
        if (other.matchingReferrers != null){
            this.matchingReferrers = new ArrayList<OsmPrimitive>(other.matchingReferrers);
        }
        this.context = other.getContext();
    }

    public Environment withChild(OsmPrimitive child) {
        Environment e = new Environment(this);
        e.osm = child;
        return e;
    }

    public Environment withParent(OsmPrimitive parent) {
        Environment e = new Environment(this);
        e.parent = parent;
        return e;
    }

    public Environment withContext(Context context) {
        Environment e = new Environment(this);
        e.context = context == null ? Context.PRIMITIVE : context;
        return e;
    }

    public Environment withLinkContext() {
        Environment e = new Environment(this);
        e.context = Context.LINK;
        return e;
    }

    public boolean isLinkContext() {
        return Context.LINK.equals(context);
    }

    public boolean hasParentRelation() {
        return parent != null && parent instanceof Relation;
    }

    public Collection<OsmPrimitive> getMatchingReferrers() {
        return matchingReferrers;
    }

    public void setMatchingReferrers(List<OsmPrimitive> refs) {
        matchingReferrers = refs;
    }

    public void clearMatchingReferrers() {
        matchingReferrers = null;
    }

    /**
     * Replies the current context.
     * 
     * @return the current context
     */
    public Context getContext() {
        return context == null ? Context.PRIMITIVE : context;
    }
}
