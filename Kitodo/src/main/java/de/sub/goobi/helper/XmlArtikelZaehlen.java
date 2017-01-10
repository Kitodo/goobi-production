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

package de.sub.goobi.helper;

import de.sub.goobi.beans.Prozess;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.ProzessDAO;

import org.apache.log4j.Logger;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;

public class XmlArtikelZaehlen {
	private static final Logger logger = Logger.getLogger(XmlArtikelZaehlen.class);

	public enum CountType {
		METADATA, DOCSTRUCT;
	}

	/**
	 * Anzahl der Strukturelemente ermitteln
	 * @param myProzess add description
	 */
	public int getNumberOfUghElements(Prozess myProzess, CountType inType) {
		int rueckgabe = 0;

		/*
		 * Dokument einlesen
		 */
		Fileformat gdzfile;
		try {
			gdzfile = myProzess.readMetadataFile();
		} catch (Exception e) {
			Helper.setFehlerMeldung("xml error", e.getMessage());
			return -1;
		}

		/*
		 * DocStruct rukursiv durchlaufen
		 */
		DigitalDocument mydocument = null;
		try {
			mydocument = gdzfile.getDigitalDocument();
			DocStruct logicalTopstruct = mydocument.getLogicalDocStruct();
			rueckgabe += getNumberOfUghElements(logicalTopstruct, inType);
		} catch (PreferencesException e1) {
			Helper.setFehlerMeldung("[" + myProzess.getId() + "] Can not get DigitalDocument: ", e1.getMessage());
			logger.error(e1);
			rueckgabe = 0;
		}

		/*
		 * die ermittelte Zahl im Prozess speichern
		 */
		myProzess.setSortHelperArticles(Integer.valueOf(rueckgabe));
		try {
			new ProzessDAO().save(myProzess);
		} catch (DAOException e) {
			logger.error(e);
		}
		return rueckgabe;
	}

	/**
	 * Anzahl der Strukturelemente oder der Metadaten ermitteln, die ein Band hat, rekursiv durchlaufen
	 *
	 * @param inStruct add description
	 * @param inType add description
	 */
	public int getNumberOfUghElements(DocStruct inStruct, CountType inType) {
		int rueckgabe = 0;
		if (inStruct != null) {
			/*
			 * increment number of docstructs, or add number of metadata elements
			 */
			if (inType == CountType.DOCSTRUCT) {
				rueckgabe++;
			} else {
				/* count non-empty persons */
				if (inStruct.getAllPersons() != null) {
					for (Person p : inStruct.getAllPersons()) {
						if (p.getLastname() != null && p.getLastname().trim().length() > 0) {
							rueckgabe++;
						}
					}
				}
				/* count non-empty metadata */
				if (inStruct.getAllMetadata() != null) {
					for (Metadata md : inStruct.getAllMetadata()) {
						if (md.getValue() != null && md.getValue().trim().length() > 0) {
							rueckgabe++;
						}
					}
				}
			}

			/*
			 * call children recursive
			 */
			if (inStruct.getAllChildren() != null) {
				for (DocStruct struct : inStruct.getAllChildren()) {
					rueckgabe += getNumberOfUghElements(struct, inType);
				}
			}
		}
		return rueckgabe;
	}

}
