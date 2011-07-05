package de.sub.goobi.helper;
//TODO: Check if this can be moved into UGH
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Person;
import ugh.exceptions.PreferencesException;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.helper.exceptions.DAOException;

public class XmlArtikelZaehlen {

	public enum CountType {
		METADATA, DOCSTRUCT;
	}

	

	/**
	 * Anzahl der Strukturelemente ermitteln
	 * @param myProzess
	 */
	public int getNumberOfUghElements(Prozess myProzess, CountType inType) {
		int rueckgabe = 0;

		/* --------------------------------
		 * Dokument einlesen 
		 * --------------------------------*/
		Fileformat gdzfile;
		try {
			gdzfile = myProzess.readMetadataFile();
		} catch (Exception e) {
			new Helper().setFehlerMeldung("Ermittlung der Anzahl abgebrochen, xml-LeseFehler", e.getMessage());
			return -1;
		}

		/* --------------------------------
		 * DocStruct rukursiv durchlaufen
		 * --------------------------------*/
		DigitalDocument mydocument = null;
		try {
			mydocument = gdzfile.getDigitalDocument();
			DocStruct logicalTopstruct = mydocument.getLogicalDocStruct();
			rueckgabe += getNumberOfUghElements(logicalTopstruct, inType);
		} catch (PreferencesException e1) {
			new Helper().setFehlerMeldung("[" + myProzess.getId() + "] Can not get DigitalDocument: ", e1.getMessage());
			e1.printStackTrace();
			rueckgabe = 0;
		}

		/* --------------------------------
		 * die ermittelte Zahl im Prozess speichern
		 * --------------------------------*/
		myProzess.setSortHelperArticles(Integer.valueOf(rueckgabe));
		try {
			new ProzessDAO().save(myProzess);
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return rueckgabe;
	}

	

	/**
	 * Anzahl der Strukturelemente oder der Metadaten ermitteln, die ein Band hat, rekursiv durchlaufen
	 * @param myProzess
	 */
	public int getNumberOfUghElements(DocStruct inStruct, CountType inType) {
		int rueckgabe = 0;
		if (inStruct != null) {
			/* --------------------------------
			 * increment number of docstructs, or add number of metadata elements
			 * --------------------------------*/
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

			/* --------------------------------
			 * call children recursive
			 * --------------------------------*/
			if (inStruct.getAllChildren() != null) {
				for (DocStruct struct : inStruct.getAllChildren()) {
					rueckgabe += getNumberOfUghElements(struct, inType);
				}
			}
		}
		return rueckgabe;
	}

}