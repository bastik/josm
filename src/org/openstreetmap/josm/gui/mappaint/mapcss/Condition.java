// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;

abstract public class Condition {

    protected final Context context;

    abstract public boolean applies(Environment e);

    public static Condition create(String k, String v, Op op, Context context) {
        return new KeyValueCondition(k, v, op, context);
    }

    public static Condition create(String k, boolean not, boolean yes, Context context) {
        return new KeyCondition(k, not, yes, context);
    }

    public static Condition create(String id, boolean not, Context context) {
        return new PseudoClassCondition(id, not, context);
    }

    public static Condition create(Expression e, Context context) {
        return new ExpressionCondition(e, context);
    }

    public Condition(Context context) {
        this.context = context;
    }

    public static enum Op { EQ, NEQ, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL, LESS,
        REGEX, ONE_OF, BEGINS_WITH, ENDS_WITH, CONTAINS }

    /**
     * context, where the condition applies
     */
    public static enum Context {
        /**
         * normal primitive selector, e.g. way[highway=residential]
         */
        PRIMITIVE,

        /**
         * link between primitives, e.g. relation >[role=outer] way
         */
        LINK
    }

    public final static EnumSet<Op> COMPARISON_OPERATERS =
        EnumSet.of(Op.GREATER_OR_EQUAL, Op.GREATER, Op.LESS_OR_EQUAL, Op.LESS);

    /**
     * <p>Represents a key/value condition which is either applied to a primitive or to
     * a "link" between two primities, i.e. to a role of a relation member.</p>
     * 
     */
    public static class KeyValueCondition extends Condition {

        public String k;
        public String v;
        public Op op;
        private float v_float;

        /**
         * <p>Creates a key/value-condition.</p>
         * 
         * <p>Note, that only the key <tt>role</tt> (case-insensitive) is allowed if this
         * condition is evaluated in a {@link Context#LINK link context}.
         * 
         * @param k the key
         * @param v the value
         * @param op the operation
         * @param context the context
         */
        public KeyValueCondition(String k, String v, Op op, Context context) throws MapCSSException {
            super(context);
            this.k = k;
            this.v = v;
            this.op = op;
            if (COMPARISON_OPERATERS.contains(op)) {
                v_float = Float.parseFloat(v);
            }
            if (Context.LINK.equals(context) && ! ("role".equalsIgnoreCase(k) || "index".equalsIgnoreCase(k)))
                throw new MapCSSException(
                        MessageFormat.format("Expected key ''role'' in link context. Got ''{0}''.", k)
                );
        }

        protected boolean matchesValue(String val){
            if (val == null && op != Op.NEQ)
                return false;
            switch (op) {
            case EQ:
                return equal(val, v);
            case NEQ:
                return !equal(val, v);
            case REGEX:
                Pattern p = Pattern.compile(v);
                Matcher m = p.matcher(val);
                return m.find();
            case ONE_OF:
                String[] parts = val.split(";");
                for (String part : parts) {
                    if (equal(v, part.trim()))
                        return true;
                }
                return false;
            case BEGINS_WITH:
                return val.startsWith(v);
            case ENDS_WITH:
                return val.endsWith(v);
            case CONTAINS:
                return val.contains(v);
            }
            float val_float;
            try {
                val_float = Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return false;
            }
            switch (op) {
            case GREATER_OR_EQUAL:
                return val_float >= v_float;
            case GREATER:
                return val_float > v_float;
            case LESS_OR_EQUAL:
                return val_float <= v_float;
            case LESS:
                return val_float < v_float;
            default:
                throw new AssertionError();
            }
        }

        @Override
        public boolean applies(Environment env) {
            switch(env.getContext()) {
            case PRIMITIVE:
                return matchesValue(env.osm.get(k));
            case LINK:
                if ("role".equalsIgnoreCase(k)) {
                    if (!env.hasParentRelation()) return false;
                    Relation r = (Relation)env.parent;
                    return matchesValue(r.getMember(env.index).getRole());
                } else if ("index".equalsIgnoreCase(k)) {
                    if (env.index == null) return false;
                    return matchesValue(Integer.toString(env.index + 1));
                }
            default: throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return "[" + k + "'" + op + "'" + v + "]"+context;
        }
    }

    /**
     * <p>KeyCondition represent one of the following conditions in either the link or the
     * primitive context:</p>
     * <pre>
     *     ["a label"]  PRIMITIVE:   the primitive has a tag "a label"
     *                  LINK:        the parent is a relation it has at least one member with the role
     *                               "a label" referring to the child
     * 
     *     [!"a label"]  PRIMITIVE:  the primitive doesn't have a tag "a label"
     *                   LINK:       the parent is a relation but doesn't have a member with the role
     *                               "a label" referring to the child
     *
     *     ["a label"?]  PRIMITIVE:  the primitive has a tag "a label" whose value evaluates to a true-value
     *                   LINK:       not supported
     * </pre>
     */
    public static class KeyCondition extends Condition {

        private String label;
        private boolean exclamationMarkPresent;
        private boolean questionMarkPresent;

        /**
         * 
         * @param label
         * @param exclamationMarkPresent
         * @param questionMarkPresent
         * @param context
         * @throws MapCSSException thrown if {@code questionMarkPresent} is true in a {@link Context#LINK link context}
         */
        public KeyCondition(String label, boolean exclamationMarkPresent, boolean questionMarkPresent, Context context) throws MapCSSException{
            super(context);
            this.label = label;
            this.exclamationMarkPresent = exclamationMarkPresent;
            this.questionMarkPresent = questionMarkPresent;
            if (Context.LINK.equals(context) && questionMarkPresent)
                throw new MapCSSException(
                        "Question mark operator ''?'' not supported in LINK context"
                );
        }

        @Override
        public boolean applies(Environment e) {
            switch(e.getContext()) {
            case PRIMITIVE:
                if (questionMarkPresent)
                    return OsmUtils.isTrue(e.osm.get(label)) ^ exclamationMarkPresent;
                else
                    return e.osm.hasKey(label) ^ exclamationMarkPresent;
            case LINK:
                if (!e.hasParentRelation()) return false;
                return equal(label, ((Relation)e.parent).getMember(e.index).getRole()) ^ exclamationMarkPresent;
            default: throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return "[" + (exclamationMarkPresent ? "!" : "") + label + "]";
        }
    }

    public static class PseudoClassCondition extends Condition {

        String id;
        boolean not;

        public PseudoClassCondition(String id, boolean not, Context context) {
            super(context);
            this.id = id;
            this.not = not;
        }

        @Override
        public boolean applies(Environment e) {
            return not ^ appliesImpl(e);
        }

        public boolean appliesImpl(Environment e) {
            if (equal(id, "closed")) {
                if (e.osm instanceof Way && ((Way) e.osm).isClosed())
                    return true;
                if (e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon())
                    return true;
                return false;
            } else if (equal(id, "modified"))
                return e.osm.isModified() || e.osm.isNewOrUndeleted();
            else if (equal(id, "new"))
                return e.osm.isNew();
            else if (equal(id, "connection") && (e.osm instanceof Node))
                return ((Node) e.osm).isConnectionNode();
            else if (equal(id, "tagged"))
                return e.osm.isTagged();
            return true;
        }

        @Override
        public String toString() {
            return ":" + (not ? "!" : "") + id;
        }
    }

    public static class ExpressionCondition extends Condition {

        private Expression e;

        public ExpressionCondition(Expression e, Context context) {
            super(context);
            this.e = e;
        }

        @Override
        public boolean applies(Environment env) {
            Boolean b = Cascade.convertTo(e.evaluate(env), Boolean.class);
            return b != null && b;
        }

        @Override
        public String toString() {
            return "[" + e + "]";
        }
    }

}
