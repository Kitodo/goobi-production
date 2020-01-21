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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.production.enums.PluginType;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetsModsDigitalDocumentHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyPrefsHelper;
import org.kitodo.production.plugin.PluginLoader;
import org.kitodo.production.plugin.UnspecificPlugin;

/**
 * The class CataloguePlugin is a redirection class that takes a library
 * catalog access plug-in implementation object as argument. The plugin
 * implementation class can be a POJO that can be compiled without the necessity
 * to link it against the Production code, thus making it possible to provide
 * proprietary plug-ins that do not violate the GPL. The plugin class must
 * however implement the following public methods which it is checked for upon
 * instantiation. If one of the methods is missing a NoSuchMethodException will
 * be thrown.
 *
 * <p>
 * {@code void configure(Map)}<br>
 * See {@link UnspecificPlugin}.
 *
 * <p>
 * {@code Object find(String, long)}<br>
 * The function is to perform a search request in a library catalog.
 * See {@link QueryBuilder} for the semantics of the query. It may return an
 * arbitrary Object identifying (or—at the implementor’s choice—containing) the
 * search result. The method shall return null if the search performed normally,
 * but didn’t yield any result. The method shall ensure that it returns after
 * the given timeout and shall throw a javax.persistence.QueryTimeoutException
 * if it was cancelled by the timer. The method may throw exceptions.
 *
 * <p>
 * {@code String getDescription()}<br>
 * See {@link UnspecificPlugin}.
 *
 * <p>
 * {@code Map getHit(Object, long, long)}<br>
 * The function shall return the hit identified by its index. The hit
 * shall be a Map&lt;String, Object&gt; with the fields described in {@link Hit}
 * populated. The method shall ensure that it returns after the given timeout
 * and shall throw a javax.persistence.QueryTimeoutException if it was cancelled
 * by the timer. The method may throw exceptions.
 *
 * <p>
 * {@code long getNumberOfHits(Object, long)}<br>
 * The function shall return the number of hits scored by the
 * search represented by the given object. If the object isn’t the result of a
 * call to the find() function, the behavior of the function may be undefined.
 * The method shall ensure that it returns after the given timeout and shall
 * throw a javax.persistence.QueryTimeoutException if it was cancelled by the
 * timer. The method may throw exceptions.
 *
 * <p>
 * {@code String getTitle()}<br>
 * See {@link UnspecificPlugin}.
 *
 * <p>
 * {@code void setPreferences(Prefs)}<br>
 * The method is called before the first search request to the
 * plug-in and passes the UGH preferences the plug-in shall use.
 *
 * <p>
 * {@code boolean supportsCatalogue(String)}<br>
 * The function shall return whether the plug-in has
 * sufficient knowledge to query a catalog identified by the given String
 * literal or not.
 *
 * <p>
 * {@code void useCatalogue(String)}<br>
 * The function is called before the first search request to the
 * plug-in and shall tell it to use the catalog connection identified by the
 * given String literal. If the plug-in doesn’t support the given catalog
 * (supportsCatalogue() would return false) the behavior may be unspecified.
 */
public class CataloguePlugin extends UnspecificPlugin {

    /**
     * The field find holds a Method reference to the method find() of the
     * plug-in implementation class.
     */
    private final Method find;

    /**
     * The field getHit holds a Method reference to the method getHit() of the
     * plug-in implementation class.
     */
    private final Method getHit;

    /**
     * The field getNumberOfHits holds a Method reference to the method
     * getNumberOfHits() of the plug-in implementation class.
     */
    private final Method getNumberOfHits;

    /**
     * The field setPreferences holds a Method reference to the method
     * setPreferences() of the plug-in implementation class.
     */
    private final Method setPreferences;

    /**
     * The field supportsCatalogue holds a Method reference to the method
     * supportsCatalogue() of the plug-in implementation class.
     */
    private final Method supportsCatalogue;

    /**
     * The field useCatalogue holds a Method reference to the method
     * useCatalogue() of the plug-in implementation class.
     */
    private final Method useCatalogue;

    /**
     * CataloguePlugin constructor. The constructor takes a reference to the
     * plug-in implementation class, saves it in the final field plug-in and
     * inspects the class for existence of the methods configure,
     * getDescription, getTitle, find, getHit, getNumberOfHits, setPreferences,
     * supportsCatalogue and useCatalogue.
     *
     * @param implementation
     *            plug-in implementation class
     * @throws SecurityException
     *             If a security manager, is present and an invocation of its
     *             method checkMemberAccess(this, Member.DECLARED) denies access
     *             to the declared method or if the caller’s class loader is not
     *             the same as or an ancestor of the class loader for the
     *             current class and invocation of the security manager’s
     *             checkPackageAccess() denies access to the package of this
     *             class.
     * @throws NoSuchMethodException
     *             if a required method is not found on the plug-in
     */
    public CataloguePlugin(Object implementation) throws NoSuchMethodException {
        super(implementation);
        find = getDeclaredMethod("find", new Class[] {String.class, long.class }, Object.class);
        getHit = getDeclaredMethod("getHit", new Class[] {Object.class, long.class, long.class }, Map.class);
        getNumberOfHits = getDeclaredMethod("getNumberOfHits", new Class[] {Object.class, long.class }, long.class);
        setPreferences = getDeclaredMethod("setPreferences", LegacyPrefsHelper.class, Void.TYPE);
        supportsCatalogue = getDeclaredMethod("supportsCatalogue", String.class, boolean.class);
        useCatalogue = getDeclaredMethod("useCatalogue", String.class, Void.TYPE);
    }

    /**
     * The function is intended to send a search request to a library
     * catalog and to return an Object identifying (or—at the implementor’s
     * choice—containing) the search result. The method shall return null if the
     * search didn’t yield any result. The method shall ensure that it returns
     * after the given timeout and shall throw a
     * javax.persistence.QueryTimeoutException in that case. The method may
     * throw exceptions.
     *
     * @param query
     *            Query string
     * @param timeout
     *            timeout in milliseconds
     * @return an object identifying the result set
     */
    public Object find(String query, long timeout) {
        return invokeQuietly(plugin, find, new Object[] {query, timeout }, Object.class);
    }

    /**
     * Returns at random the first hit the catalog plug-in returns for a given
     * query. Making use of this utility method only makes sense if there is
     * only one result expected, which usually is the case if a valid identifier
     * is looked up in its respective column.
     *
     * @param catalogue
     *            catalog in question
     * @param query
     *            Query string
     * @param preferences
     *            Prefs object
     * @return UGH preferences
     */
    public static LegacyMetsModsDigitalDocumentHelper getFirstHit(String catalogue, String query, LegacyPrefsHelper preferences) {
        try {
            CataloguePlugin plugin = PluginLoader.getCataloguePluginForCatalogue(catalogue);
            if (Objects.nonNull(plugin)) {
                plugin.setPreferences(preferences);
                plugin.useCatalogue(catalogue);
                Object searchResult = plugin.find(query, getTimeout());
                long hits = plugin.getNumberOfHits(searchResult, getTimeout());
                if (hits > 0) {
                    return plugin.getHit(searchResult, 0, getTimeout()).getFileformat();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The function shall return the hit identified by its index. The
     * method shall ensure that it returns after the given timeout and shall
     * throw a javax.persistence.QueryTimeoutException in that case. The method
     * may throw exceptions.
     *
     * @param searchResult
     *            an object identifying the search result
     * @param index
     *            zero-based index of the object to retrieve
     * @param timeout
     *            timeout in milliseconds
     * @return A map with the fields of the hit
     */
    @SuppressWarnings("unchecked")
    public Hit getHit(Object searchResult, long index, long timeout) {
        Map<String, Object> data = invokeQuietly(plugin, getHit, new Object[] {searchResult, index, timeout },
            Map.class);
        return new Hit(data);
    }

    /**
     * The function shall return the hits scored by the search
     * represented by the given object. If the object isn’t the result of a call
     * to the find() function, the behavior may be undefined.
     *
     * @param searchResult
     *            search result object whose number of hits is to retrieve
     * @return the number of hits in this search
     */
    public long getNumberOfHits(Object searchResult, long timeout) {
        if (Objects.isNull(searchResult)) {
            return 0;
        }
        return invokeQuietly(plugin, getNumberOfHits, new Object[] {searchResult, timeout }, long.class);
    }

    /**
     * Returns the timeout to be used in catalog
     * access. This defaults to thirty minutes if no catalog timeout is set in
     * the configuration
     *
     * <p>
     * Note that on large, database-backed catalogs, searches for common title
     * terms may take more than 15 minutes, so 30 minutes may be a fair average
     * between that and the moment the sun will swallow up the earth.
     *
     * @return the timeout for catalog access
     */
    public static long getTimeout() {
        return ConfigCore.getLongParameterOrDefaultValue(ParameterCore.CATALOGUE_TIMEOUT);
    }

    /**
     * Returns the PluginType.Opac as it corresponds to
     * this class.
     *
     * @see org.kitodo.production.plugin.UnspecificPlugin#getType()
     */
    @Override
    public PluginType getType() {
        return PluginType.CATALOGUE;
    }

    /**
     * Must be used to set the UGH preferences the
     * plug-in shall use.
     *
     * @param preferences
     *            UGH preferences
     * @see org.kitodo.production.services.data.RulesetService#getPreferences(org.kitodo.data.database.beans.Ruleset)
     */
    public void setPreferences(LegacyPrefsHelper preferences) {
        invokeQuietly(plugin, setPreferences, preferences, null);
    }

    /**
     * Returns whether the plug-in has sufficient knowledge to query a catalog
     * identified by the given String literal.
     *
     * @param catalogue
     *            catalog in question
     * @return whether the plug-in supports that catalog
     */
    public boolean supportsCatalogue(String catalogue) {
        return invokeQuietly(plugin, supportsCatalogue, catalogue, boolean.class);
    }

    /**
     * The function shall tell the plug-in to use a catalog connection
     * identified by the given String literal. If the plug-in doesn’t support
     * the given catalog (supportsCatalogue() would return false) the behavior
     * is unspecified (throwing an unchecked exception is a good option).
     *
     * @param catalogue
     *            catalog in question
     */
    public void useCatalogue(String catalogue) {
        invokeQuietly(plugin, useCatalogue, catalogue, null);
    }
}
