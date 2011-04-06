// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.LinkSelector;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class Environment {

    public OsmPrimitive osm;

    public MultiCascade mc;
    public String layer;
    public StyleSource source;
    private Context context = Context.PRIMITIVE;

    /**
     * If not null, this is the matching parent object if an condition or an expression
     * is evaluated in a {@link LinkSelector}
     */
    public OsmPrimitive parent;

    /**
     * index of node in parent way or member in parent relation
     */
    public Integer index = null;
    
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
        this.index = other.index;
        this.context = other.getContext();
    }

    public Environment withPrimitive(OsmPrimitive child) {
        Environment e = new Environment(this);
        e.osm = child;
        return e;
    }

    public Environment withParent(OsmPrimitive parent) {
        Environment e = new Environment(this);
        e.parent = parent;
        return e;
    }
    
    public Environment withIndex(int index) {
        Environment e = new Environment(this);
        e.index = index;
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

    /**
     * Replies the current context.
     * 
     * @return the current context
     */
    public Context getContext() {
        return context == null ? Context.PRIMITIVE : context;
    }
    
    public void clearSelectorMatchingInformation() {
        parent = null;
        index = null;
    }
}
