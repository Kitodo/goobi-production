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

package de.sub.goobi.persistence;

import de.sub.goobi.beans.LdapGruppe;
import de.sub.goobi.helper.exceptions.DAOException;

import java.util.List;

@SuppressWarnings("serial")
public class LdapGruppenDAO extends BaseDAO {

	public LdapGruppe save(LdapGruppe t) throws DAOException {
		storeObj(t);
		return (LdapGruppe) retrieveObj(LdapGruppe.class, t.getId());
	}

	/**
	 * @param id add description
	 * @return add description
	 * @throws DAOException add description
	 */
	public LdapGruppe get(Integer id) throws DAOException {
		LdapGruppe rueckgabe = (LdapGruppe) retrieveObj(LdapGruppe.class, id);
		if (rueckgabe == null) {
			throw new DAOException("Object can not be found in database");
		}
		return rueckgabe;
	}

	/**
	 * @param t add description
	 * @throws DAOException add description
	 */
	public void remove(LdapGruppe t) throws DAOException {
		if (t.getId() != null) {
			removeObj(t);
		}
	}

	public void remove(Integer id) throws DAOException {
		removeObj(LdapGruppe.class, id);
	}

	@SuppressWarnings("unchecked")
	public List<LdapGruppe> search(String query) throws DAOException {
		return retrieveObjs(query);
	}

}
