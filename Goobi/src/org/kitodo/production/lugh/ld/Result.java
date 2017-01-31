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

import java.nio.BufferOverflowException;
import java.util.*;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.*;

/**
 * The results returned by a call to a getter on a node.
 *
 * @author Matthias Ronge
 */
public class Result extends HashSet<ObjectType> {
    /**
     * Constant defining an empty result.
     */
    static final Result EMPTY = new Result() {
        private static final String IMMUTABILITY = "The empty Result is immutable.";
        private static final long serialVersionUID = 1L;

        @Override
        public boolean add(ObjectType e) {
            throw new UnsupportedOperationException(IMMUTABILITY);
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException(IMMUTABILITY);
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException(IMMUTABILITY);
        }
    };

    @SuppressWarnings("javadoc")
    private static final long serialVersionUID = 1L;

    /**
     * Calculate the initial capacity of the hash set.
     *
     * @param capacity
     *            current number of objects
     * @return the initial capacity of the hash set
     */
    private static int cap(int capacity) {
        long result = (long) Math.ceil((capacity + 12) / 0.75);
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < 16) {
            return 16;
        }
        return (int) result;
    }

    /**
     * Creates nodes from a Jena model. Returns all nodes not referenced from
     * anywhere (the “top nodes”), or all nodes if there aren’t any “top nodes”.
     *
     * @param model
     *            model to read out
     * @param alwaysAll
     *            always returns all nodes from the model, independent of
     *            whether there are “top nodes” or not
     */
    public static Result createFrom(Model model, boolean alwaysAll) {
        HashMap<String, Node> result = new HashMap<>();
        HashMap<String, Node> resolver = new HashMap<>();

        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement statement = iter.nextStatement();

            Resource subject = statement.getSubject();
            String subjectIdentifier = subject.toString();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();

            if (!resolver.containsKey(subjectIdentifier)) {
                Node newNode = subject.asNode().isBlank() ? new Node() : new NamedNode(subjectIdentifier);
                resolver.put(subjectIdentifier, newNode);
                result.put(subjectIdentifier, newNode);
            }

            Node subjectNode = resolver.get(subjectIdentifier);
            ObjectType objectNode;
            if (object.isResource()) {
                String objectIdentifier = object.toString();
                if (!resolver.containsKey(objectIdentifier)) {
                    Node newNode = object.asNode().isBlank() ? new Node() : new NamedNode(objectIdentifier);
                    resolver.put(objectIdentifier, newNode);
                    result.put(objectIdentifier, newNode);
                }
                objectNode = resolver.get(objectIdentifier);
                result.remove(objectIdentifier);
            } else {
                com.hp.hpl.jena.rdf.model.Literal literalObject = object.asLiteral();
                if (literalObject.isWellFormedXML()) {
                    objectNode = new Literal(literalObject.toString(), RDF.XML_LITERAL);
                } else if (!literalObject.getLanguage().isEmpty()) {
                    objectNode = new LangString(literalObject.getValue().toString(), literalObject.getLanguage());
                } else if (literalObject.getDatatype() != null) {
                    objectNode = new Literal(literalObject.getValue().toString(), literalObject.getDatatypeURI());
                } else {
                    objectNode = new Literal(literalObject.getValue().toString(), RDF.PLAIN_LITERAL);
                }
            }
            subjectNode.put(predicate.toString(), objectNode);
        }

        for (Entry<String, Node> entries : resolver.entrySet()) {
            entries.getValue().replaceAllNamedNodesWithNoDataByNodeReferences(false);
        }

        return new Result(alwaysAll || result.isEmpty() ? resolver.values() : result.values());
    }

    /**
     * All nodes. This will be populated if the first request for nodes occurs
     * and reset if the result is modified.
     */
    private Set<NodeType> nodeTypes;

    /**
     * Create an empty result.
     */
    Result() {
        super();
    }

    /**
     * Create a result with elements.
     *
     * @param arg0
     *            result elements
     */
    Result(Collection<? extends ObjectType> arg0) {
        super(arg0 != null ? arg0 : Collections.<ObjectType>emptyList());
    }

    /**
     * Create a result for the given amount of elements.
     *
     * @param capacity
     *            elements to be added
     */
    Result(int capacity) {
        super(cap(capacity));
    }

    /**
     * Create a result with an element.
     *
     * @param element
     *            result element
     */
    Result(ObjectType element) {
        this(Arrays.asList(new ObjectType[] { element }));
    }

    /**
     * Add an element to the result.
     */
    @Override
    public boolean add(ObjectType e) {
        boolean result = super.add(e);
        nodeTypes = null;
        return result;
    }

    /**
     * Empty the result.
     */
    @Override
    public void clear() {
        super.clear();
        nodeTypes = null;

    }

    /**
     * Returns the identifiable node. This should only be called if its presence
     * was previously checked by {@link #isUniqueIdentifiableNode()}.
     *
     * @return the accessible node
     * @throws NoSuchElementException
     *             if there is no named node
     * @throws BufferOverflowException
     *             if there are several possible answers
     */
    public IdentifiableNode identifiableNodeExpectable() {
        switch (super.size()) {
        case 0:
            throw new NoSuchElementException();
        case 1:
            NodeType node = nodeTypes().iterator().next();
            if (node instanceof IdentifiableNode) {
                return (IdentifiableNode) node;
            } else {
                throw new NoSuchElementException();
            }
        default:
            throw new BufferOverflowException();
        }
    }

    /**
     * Returns all accessible nodes.
     *
     * @return
     */
    public Set<IdentifiableNode> identifiableNodes() {
        return subset(IdentifiableNode.class);
    }


    /**
     * True if there is exactly one node in the result. If this is true,
     * {@link #identifiableNodeExpectable()} can be called.
     *
     * @return whether there is exactly one node
     */
    public boolean isUniqueIdentifiableNode() {
        return super.size() == 1 && super.iterator().next() instanceof IdentifiableNode;
    }
    
    /**
     * Returns the accessible node.
     *
     * @return the accessible node
     * @throws NoDataException
     *             if there is no accessible node
     * @throws AmbiguousDataException
     *             if there are several possible answers
     */
    public Node node() throws LinkedDataException {
        Set<NodeType> nodes = nodeTypes();
        switch (nodes.size()) {
        case 0:
            throw new NoDataException();
        case 1:
            if (super.size() > 1) {
                throw new AmbiguousDataException();
            }
            NodeType node = nodes.iterator().next();
            if (node instanceof Node) {
                return (Node) node;
            } else {
                throw new NoDataException();
            }
        default:
            throw new AmbiguousDataException();
        }
    }

    /**
     * Returns all accessible nodes.
     *
     * @return
     */
    public Set<Node> nodes() {
        return subset(Node.class);
    }

    /**
     * Returns all the nodes.
     *
     * @return all nodes
     */
    private Set<NodeType> nodeTypes() {
        if (nodeTypes == null) {
            nodeTypes = subset(NodeType.class);
        }
        return nodeTypes;
    }

    /**
     * Delete a node from the result.
     */
    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        nodeTypes = null;
        return result;
    }

    /**
     * Returns all the literals as strings. References to other nodes are not
     * returned.
     *
     * @return the literal
     */
    public Set<String> strings() {
        return strings(false);
    }

    /**
     * Returns all the literals as strings.
     *
     * @param allLeaves
     *            if true, references to other nodes are returned as well
     *
     * @return the literal
     */
    public Set<String> strings(boolean allLeaves) {
        HashSet<String> result = new HashSet<>((int) Math.ceil(super.size() / 0.75));
        for (ObjectType literal : this) {
            if (literal instanceof Literal) {
                result.add(((Literal) literal).getValue());
            } else if (allLeaves && (literal instanceof NodeReference)) {
                result.add(((NodeReference) literal).getIdentifier());
            }
        }
        return result;
    }

    /**
     * Returns all literals from this node, joined by the given separator.
     *
     * @param separator
     *            separator to use
     * @param allLeaves
     *            return all leaves
     * @return all literals from this node as String
     */
    public String strings(String separator, boolean allLeaves) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (ObjectType literal : this) {
            if (literal instanceof Literal) {
                if (first) {
                    first = false;
                } else {
                    result.append(separator);
                }
                result.append(((Literal) literal).getValue());
            } else if (allLeaves && (literal instanceof NodeReference)) {
                if (first) {
                    first = false;
                } else {
                    result.append(separator);
                }
                result.append(((NodeReference) literal).getIdentifier());
            }
        }
        return result.toString();
    }

    /**
     * Creates a subset by type check. Used for filtering.
     *
     * @param clazz
     *            Subclass to create a subset of
     * @param <T>
     *            subclass type
     * @return a subset of a given type
     */
    @SuppressWarnings("unchecked") // The compiler does not understand that
                                   // isAssignableFrom() does the type check
    private <T> Set<T> subset(Class<T> clazz) {
        Set<T> result = new HashSet<>();
        for (ObjectType entry : this) {
            if (clazz.isAssignableFrom(entry.getClass())) {
                result.add((T) entry);
            }
        }
        return result;
    }

}
