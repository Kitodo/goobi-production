/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.plugin.catalogue;

import java.util.List;

/**
 * The class QueryBuilder provides methods to create query strings to be passed
 * to library catalog access plug-in implementation objects as argument to
 * their find() function. The semantics of the created query is as follows, in
 * descending precedence of the operators:
 *
 * <p>
 * <b>Double quotes</b> ({@code ""}) may be used to embrace a sequence of
 * tokens that have to appear in exactly that order in the search result (aka.
 * string search). They must appear in pairs.
 * 
 * <p>
 * <b>Round brackets</b> ({@code ()}) may be used for logical reordering.
 * They must appear in pairs.
 *
 * <p>
 * <b>Blanks</b> (<code> </code>) are used to separate tokens. They imply a
 * conjunction, i.e. {@code cat dog} shall return only hits that contain
 * both tokens.
 *
 * <p>
 * A <b>vertical dash</b> ({@code |}) indicates the freedom of choice, i.e.
 * {@code cat | dog} may return hits that contain the token “cat” but not
 * necessarily the token “dog” and the other way round and of course hits that
 * contain both tokens as well.
 *
 * <p>
 * A <b>minus sign</b> ({@code -}) indicates the exclusion of a search
 * term, i.e. that the hits shall not contain the given term. (Example:
 * {@code track -running})<br>
 * In combination with <i>colon</i>, the minus sign shall prefix the whole
 * expression, not the search term (i.e. {@code track -4:running}, not
 * {@code track 4:-running}).
 *
 * <p>
 * A <b>colon</b> ({@code :}) indicates fielded search, so that the term
 * must be found—or, in combination with a minus sign, must not be found—in the
 * prepended field of the database (i.e. {@code 4:beagle} to search for the
 * term “beagle” in the field “4”). Search fields are referenced by the integers
 * used to reference them in PICA library catalogs (i.e. “4” = title, “7” =
 * ISBN, “8” = ISSN, “12” = Record identifier, …; for a list of supported fields
 * see /Kitodo/src/main/webapp/pages/NewProcess/inc_process.xhtml)
 */
public class QueryBuilder {

    /**
     * Private constructor to hide the implicit public one.
     */
    private QueryBuilder() {

    }

    /**
     * Appends a list of query tokens to an initial query.
     *
     * @param query
     *            query to append to
     * @param tokens
     *            tokens to append
     * @return complete query
     */
    public static String appendAll(String query, List<String> tokens) {
        if (tokens.isEmpty()) {
            return query;
        }
        int capacity = query.length();
        for (String token : tokens) {
            capacity += token.length() + 1;
        }
        StringBuilder completeQuery = new StringBuilder(capacity);
        completeQuery.append(query);
        for (String token : tokens) {
            completeQuery.append(' ');
            completeQuery.append(token);
        }
        return completeQuery.toString();
    }

    /**
     * Prefixes the tokens of a query by the
     * given search field.
     *
     * @param field
     *            search field to prepend
     * @param query
     *            query to process
     * @return a query whose tokens are prefixed by the given search field
     */
    public static String restrictToField(String field, String query) {
        StringBuilder restrictedQuery = new StringBuilder(2 * query.length());
        String prefix = field.concat(":");
        boolean appendField = true;
        boolean stringLiteral = false;
        for (int index = 0; index < query.length(); index++) {
            int codePoint = query.charAt(index);
            switch (codePoint) {
                case ' ':
                case '(':
                case ')':
                case '|':
                    if (!stringLiteral) {
                        appendField = true;
                    }
                    restrictedQuery.appendCodePoint(codePoint);
                    break;
                case '"':
                    if (stringLiteral && appendField) {
                        restrictedQuery.append(prefix);
                    }
                    stringLiteral = !stringLiteral;
                    appendField = stringLiteral;
                    restrictedQuery.appendCodePoint(codePoint);
                    break;
                case '-':
                    restrictedQuery.appendCodePoint(codePoint);
                    if (appendField) {
                        restrictedQuery.append(prefix);
                    }
                    appendField = false;
                    break;
                default:
                    if (appendField) {
                        restrictedQuery.append(prefix);
                    }
                    appendField = false;
                    restrictedQuery.appendCodePoint(codePoint);
                    break;
            }

        }
        return restrictedQuery.toString();
    }
}
