/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General private License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.lugh.ld;

import java.util.*;
import java.util.regex.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * A linked data literal.
 *
 * @author Matthias Ronge
 */
public class Literal implements AccessibleObject {

    /**
     * Three relations allowed on a node describing a literal.
     */
    /*
     * Constants from `RDF.java` cannot be used here because the constants are
     * NodeReference objects, thus a subclass of this class and hence cannot yet
     * be accessed at creation time of this class.
     */
    protected static final Set<String> ALLOWED_RELATIONS = new HashSet<>(
            Arrays.asList(new String[] { "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                    "http://www.w3.org/XML/1998/namespace#lang", "http://www.w3.org/1999/02/22-rdf-syntax-ns#value" }));

    /**
     * Three literal types, not including RDF.LANG_STRING, that may be passed to
     * the Literal constructor when creating a common literal.
     */
    /*
     * Constants from `RDF.java` cannot be used here because the constants are
     * NodeReference objects, thus a subclass of this class and hence cannot yet
     * be accessed at creation time of this class.
     */
    private static final List<String> LITERAL_TYPES = Arrays
            .asList(new String[] { "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral",
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#HTML",
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral" });

    /**
     * Pattern to check whether a String starts with an URL scheme as specified
     * in RFC 1738.
     */
    private static final Pattern SCHEME_TEST = Pattern.compile("[+\\-\\.0-9A-Za-z]+:[^ ]+");

    static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     * Creates a literal object from a String. If the literal starts with
     * {@code http://}, a node reference is created, otherwise if a language is
     * given, a LangString will be created, otherwise a plain literal.
     *
     * @param value
     *            the literal value
     * @param lang
     *            language, may be {@code ""} but not {@code null}
     * @return the literal object
     */
    public static ObjectType create(String value, String lang) {
        if (SCHEME_TEST.matcher(value).matches()) {
            return new NodeReference(value);
        } else {
            return createLiteral(value, lang);
        }
    }

    /**
     * Creates a literal object from a String. If a language is given, a
     * LangString will be created, otherwise a plain literal.
     *
     * @param value
     *            the literal value
     * @param lang
     *            language, may be {@code ""} but not {@code null}
     * @return the literal object
     */
    public static Literal createLiteral(String value, String lang) {
        if ((lang == null) || lang.isEmpty()) {
            return new Literal(value, RDF.PLAIN_LITERAL);
        } else {
            return new LangString(value, lang);
        }
    }

    /**
     * Type of this literal.
     */
    private final String type;

    /**
     * The Literal value.
     */
    protected final String value;

    /**
     * Creates a new Literal with a value and a type.
     *
     * @param value
     *            literal value
     * @param type
     *            literal type, one of RDF.HTML, RDF.PLAIN_LITERAL,
     *            RDF.XML_LITERAL, or a literal type defined in XMLSchema.
     */
    public Literal(String value, NodeReference type) {
        this.value = value != null ? value : "";
        this.type = type != null ? type.getIdentifier() : RDF.PLAIN_LITERAL.getIdentifier();
        if (!LITERAL_TYPES.contains(this.type) && !XSD_NAMESPACE.equals(Namespace.namespaceOf(this.type))) {
            throw new IllegalArgumentException(type.getIdentifier());
        }
    }

    /**
     * Constructor for use in subclass {@link LangString}.
     *
     * @param value
     *            literal value
     * @param type
     *            literal type
     */
    protected Literal(String value, String type) {
        this.value = value != null ? value : "";
        assert URI_SCHEME.matcher(type).find() : "Illegal type."; 
        this.type = type;
    }

    /**
     * Compares this Literal against another object for equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Literal other = (Literal) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the semantic web type of this literal.
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Returns the value of this literal.
     *
     * @return the literal value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a hash code of the Literal.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (type == null ? 0 : type.hashCode());
        result = (prime * result) + (value == null ? 0 : value.hashCode());
        return result;
    }

    /**
     * Returns whether this literal is described by the condition node type.
     */
    @Override
    public boolean matches(ObjectType condition) {
        if (condition instanceof Literal) {
            Literal other = (Literal) condition;
            if ((other.type != null) && !other.type.equals(type)) {
                return false;
            }
            if ((other.value != null) && !other.value.isEmpty() && !other.value.equals(value)) {
                return false;
            }
            return true;
        } else if (condition instanceof Node) {
            Node filter = (Node) condition;
            if (!ALLOWED_RELATIONS.containsAll(filter.getRelations())) {
                return false;
            }

            Result expectedType = filter.get(RDF.TYPE);
            switch (expectedType.size()) {
            case 0:
                break;
            case 1:
                ObjectType checkType = expectedType.iterator().next();
                if (!(checkType instanceof IdentifiableNode)) {
                    return false;
                }
                if (!((IdentifiableNode) checkType).getIdentifier().equals(type)) {
                    return false;
                }
                break;
            default:
                return false;
            }

            Result expectedValue = filter.get(RDF.VALUE);
            switch (expectedValue.size()) {
            case 0:
                break;
            case 1:
                ObjectType checkType = expectedValue.iterator().next();
                if (!(checkType instanceof Literal)) {
                    return false;
                }
                if (!((Literal) checkType).getValue().equals(value)) {
                    return false;
                }
                break;
            default:
                return false;
            }
        }
        return true;
    }

    /**
     * Converts this literal to an RDFNode as part of a Jena model.
     *
     * @param model
     *            model to create objects in
     * @return an RDFNode representing this node
     */
    @Override
    public RDFNode toRDFNode(Model model) {
        if (type.equals(RDF.PLAIN_LITERAL.getIdentifier())) {
            return model.createLiteral(value);
        } else {
            return model.createTypedLiteral(value, type);
        }
    }

    /**
     * Returns a readable description of this literal to be seen in a debugger.
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer(value.length() + 2);
        boolean isXsdInteger = "http://www.w3.org/2001/XMLSchema#integer".equals(type);
        Matcher matcher = Pattern.compile("[\u0000-\u001F\\\\]").matcher(value);
        if (!isXsdInteger) {
            result.append('"');
        }
        while (matcher.find()) {
            matcher.appendReplacement(result, ""); // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=652315
            result.append('\\');
            if (matcher.group().equals("\\")) {
                result.append(matcher.group());
            } else {
                result.append(new String(Character.toChars(60 + matcher.group().codePointAt(0))));
            }
        }
        matcher.appendTail(result);
        if (!isXsdInteger) {
            result.append('"');
        }
        if (!isXsdInteger && !RDF.PLAIN_LITERAL.getIdentifier().equals(type)) {
            result.append("^^");
            String namespace = Namespace.namespaceOf(type);
            if (namespace.equals(XSD_NAMESPACE)) {
                result.append("xsd:");
                result.append(Namespace.localNameOf(type));
            } else if (namespace.equals(RDF.NAMESPACE)) {
                result.append("rdf:");
                result.append(Namespace.localNameOf(type));
            } else {
                result.append(type);
            }
        }
        return result.toString();
    }
}
