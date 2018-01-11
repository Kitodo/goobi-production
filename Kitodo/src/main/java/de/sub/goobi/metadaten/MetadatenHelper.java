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

package de.sub.goobi.metadaten;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperComparator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import javax.faces.model.SelectItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.DocStructTypeInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.ReferenceInterface;
import org.kitodo.api.ugh.UghImplementation;
import org.kitodo.data.database.beans.Process;
import org.kitodo.services.ServiceManager;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;

public class MetadatenHelper implements Comparator<Object> {
    private static final Logger logger = LogManager.getLogger(MetadatenHelper.class);
    private static final int PAGENUMBER_FIRST = 0;
    private static final int PAGENUMBER_LAST = 1;
    private static ServiceManager serviceManager = new ServiceManager();
    private PrefsInterface prefsInterface;
    private DigitalDocumentInterface digitalDocumentInterface;

    public MetadatenHelper(PrefsInterface inPrefs, DigitalDocumentInterface inDocument) {
        this.prefsInterface = inPrefs;
        this.digitalDocumentInterface = inDocument;
    }

    /**
     * Getter for final value PAGENUMBER_FIRST.
     *
     * @return PAGENUMBER_FIRST
     */
    public static int getPageNumberFirst() {
        return PAGENUMBER_FIRST;
    }

    /**
     * Getter for final value PAGENUMBER_LAST.
     *
     * @return PAGENUMBER_LAST
     */
    public static int getPageNumberLast() {
        return PAGENUMBER_LAST;
    }

    /**
     * Change current document structure.
     *
     * @param inOldDocstruct
     *            DocStruct object
     * @param inNewType
     *            String
     * @return DocStruct object
     */
    public DocStructInterface changeCurrentDocstructType(DocStructInterface inOldDocstruct, String inNewType)
            throws DocStructHasNoTypeException, MetadataTypeNotAllowedException, TypeNotAllowedAsChildException,
            TypeNotAllowedForParentException {
        // inOldDocstruct.getType().getName()
        // + " soll werden zu " + inNewType);
        DocStructTypeInterface dst = this.prefsInterface.getDocStrctTypeByName(inNewType);
        DocStructInterface newDocstruct = this.digitalDocumentInterface.createDocStruct(dst);
        /*
         * alle Metadaten hinzufügen
         */
        if (inOldDocstruct.getAllMetadata() != null && inOldDocstruct.getAllMetadata().size() > 0) {
            for (MetadataInterface old : inOldDocstruct.getAllMetadata()) {
                boolean match = false;

                if (newDocstruct.getPossibleMetadataTypes() != null
                        && newDocstruct.getPossibleMetadataTypes().size() > 0) {
                    for (MetadataTypeInterface mt : newDocstruct.getPossibleMetadataTypes()) {
                        if (mt.getName().equals(old.getType().getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        try {
                            newDocstruct.addMetadata(old);
                        } catch (Exception e) {
                            Helper.setFehlerMeldung("Metadata " + old.getType().getName()
                                    + " is not allowed in new element " + newDocstruct.getType().getName());
                            return inOldDocstruct;
                        }
                    } else {
                        newDocstruct.addMetadata(old);
                    }
                } else {
                    Helper.setFehlerMeldung("Metadata " + old.getType().getName() + " is not allowed in new element "
                            + newDocstruct.getType().getName());
                    return inOldDocstruct;
                }
            }
        }
        /*
         * alle Personen hinzufügen
         */
        if (inOldDocstruct.getAllPersons() != null && inOldDocstruct.getAllPersons().size() > 0) {
            for (PersonInterface old : inOldDocstruct.getAllPersons()) {
                boolean match = false;
                if (newDocstruct.getPossibleMetadataTypes() != null
                        && newDocstruct.getPossibleMetadataTypes().size() > 0) {
                    for (MetadataTypeInterface mt : newDocstruct.getPossibleMetadataTypes()) {
                        if (mt.getName().equals(old.getType().getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        Helper.setFehlerMeldung("Person " + old.getType().getName() + " is not allowed in new element "
                                + newDocstruct.getType().getName());
                    } else {
                        newDocstruct.addPerson(old);
                    }
                } else {
                    Helper.setFehlerMeldung("Person " + old.getType().getName() + " is not allowed in new element "
                            + newDocstruct.getType().getName());
                    return inOldDocstruct;
                }
            }
        }
        /*
         * alle Seiten hinzufügen
         */
        if (inOldDocstruct.getAllToReferences() != null) {
            for (ReferenceInterface referenceInterface : inOldDocstruct.getAllToReferences()) {
                newDocstruct.addReferenceTo(referenceInterface.getTarget(), referenceInterface.getType());
            }
        }

        /*
         * alle Docstruct-Children hinzufügen
         */
        if (inOldDocstruct.getAllChildren() != null && inOldDocstruct.getAllChildren().size() > 0) {
            for (DocStructInterface old : inOldDocstruct.getAllChildren()) {
                if (newDocstruct.getType().getAllAllowedDocStructTypes() != null
                        && newDocstruct.getType().getAllAllowedDocStructTypes().size() > 0) {

                    if (!newDocstruct.getType().getAllAllowedDocStructTypes().contains(old.getType().getName())) {
                        Helper.setFehlerMeldung("Child element " + old.getType().getName()
                                + " is not allowed in new element " + newDocstruct.getType().getName());
                        return inOldDocstruct;
                    } else {
                        newDocstruct.addChild(old);
                    }
                } else {
                    Helper.setFehlerMeldung("Child element " + old.getType().getName()
                            + " is not allowed in new element " + newDocstruct.getType().getName());
                    return inOldDocstruct;
                }
            }
        }
        /*
         * neues Docstruct zum Parent hinzufügen und an die gleiche Stelle
         * schieben, wie den Vorg?nger
         */
        inOldDocstruct.getParent().addChild(newDocstruct);
        int i = 1;
        // TODO: get rid of Iterators, use a for Loop instead
        for (Iterator<DocStructInterface> iter = newDocstruct.getParent().getAllChildren().iterator(); iter.hasNext(); i++) {
            if (iter.next() == inOldDocstruct) {
                break;
            }
        }
        for (int j = newDocstruct.getParent().getAllChildren().size() - i; j > 0; j--) {
            moveNodeUp(newDocstruct);
        }

        /*
         * altes Docstruct vom Parent entfernen und neues als aktuelles nehmen
         */
        inOldDocstruct.getParent().removeChild(inOldDocstruct);
        return newDocstruct;
    }

    /**
     * Move around the document structure tree.
     *
     * @param inStruct
     *            DocStruct object
     */
    public void moveNodeUp(DocStructInterface inStruct) throws TypeNotAllowedAsChildException {
        DocStructInterface parent = inStruct.getParent();
        if (parent == null) {
            return;
        }
        List<DocStructInterface> alleDS = null;

        /* das erste Element kann man nicht nach oben schieben */
        if (parent.getAllChildren().get(0) == inStruct) {
            return;
        }

        /* alle Elemente des Parents durchlaufen */
        for (DocStructInterface tempDS : parent.getAllChildren()) {
            /*
             * wenn das folgende Element das zu verschiebende ist dabei die
             * Exception auffangen, falls es kein nächstes Kind gibt
             */
            try {
                if (parent.getNextChild(tempDS) == inStruct) {
                    alleDS = new ArrayList<>();
                }
            } catch (IndexOutOfBoundsException e) {
                logger.error(e);
            }

            /*
             * nachdem der Vorg?nger gefunden wurde, werden alle anderen
             * Elemente aus der Child-Liste entfernt und separat gesammelt
             */
            if (alleDS != null && tempDS != inStruct) {
                alleDS.add(tempDS);
            }
        }

        if (alleDS != null) {
            /* anschliessend die Childs entfernen */
            for (DocStructInterface child : alleDS) {
                parent.removeChild(child);
            }

            /* anschliessend die Childliste korrigieren */
            // parent.addChild(myStrukturelement);
            for (DocStructInterface child : alleDS) {
                parent.addChild(child);
            }
        }
    }

    /**
     * Move around the document structure tree.
     *
     * @param inStruct
     *            DocStruct object
     */
    public void moveNodeDown(DocStructInterface inStruct) throws TypeNotAllowedAsChildException {
        DocStructInterface parent = inStruct.getParent();
        if (parent == null) {
            return;
        }
        List<DocStructInterface> alleDS = new ArrayList<>();

        /* alle Elemente des Parents durchlaufen */
        for (Iterator<DocStructInterface> iter = parent.getAllChildren().iterator(); iter.hasNext();) {
            DocStructInterface tempDS = iter.next();

            /* wenn das aktuelle Element das zu verschiebende ist */
            if (tempDS != inStruct) {
                alleDS.add(tempDS);
            } else {
                if (iter.hasNext()) {
                    alleDS.add(iter.next());
                }
                alleDS.add(inStruct);
            }
        }

        /* anschliessend alle Children entfernen */
        for (DocStructInterface child : alleDS) {
            parent.removeChild(child);
        }

        /* anschliessend die neue Childliste anlegen */
        for (DocStructInterface child : alleDS) {
            parent.addChild(child);
        }
    }

    /**
     * die MetadatenTypen zurückgeben.
     */
    public SelectItem[] getAddableDocStructTypen(DocStructInterface inStruct, boolean checkTypesFromParent) {
        /*
         * zuerst mal die addierbaren Metadatentypen ermitteln
         */
        List<String> types;
        SelectItem[] myTypes = new SelectItem[0];

        try {
            if (!checkTypesFromParent) {
                types = inStruct.getType().getAllAllowedDocStructTypes();
            } else {
                types = inStruct.getParent().getType().getAllAllowedDocStructTypes();
            }
        } catch (RuntimeException e) {
            return myTypes;
        }

        if (types == null) {
            return myTypes;
        }

        List<DocStructTypeInterface> newTypes = new ArrayList<>();
        for (String tempTitel : types) {
            DocStructTypeInterface dst = this.prefsInterface.getDocStrctTypeByName(tempTitel);
            if (dst != null) {
                newTypes.add(dst);
            } else {
                Helper.setMeldung(null, "Regelsatz-Fehler: ", " DocstructType " + tempTitel + " nicht definiert");
                logger.error(
                    "getAddableDocStructTypen() - Regelsatz-Fehler: DocstructType " + tempTitel + " nicht definiert");
            }
        }

        /*
         * die Metadatentypen sortieren
         */
        HelperComparator c = new HelperComparator();
        c.setSortType("DocStructTypen");
        // TODO: Uses generics, if possible
        Collections.sort(newTypes, c);

        /*
         * nun ein Array mit der richtigen Größe anlegen
         */
        int zaehler = newTypes.size();
        myTypes = new SelectItem[zaehler];

        /*
         * und anschliessend alle Elemente in das Array packen
         */
        zaehler = 0;
        Iterator<DocStructTypeInterface> it = newTypes.iterator();
        while (it.hasNext()) {
            DocStructTypeInterface dst = it.next();
            String label = dst
                    .getNameByLanguage((String) Helper.getManagedBeanValue("#{LoginForm.myBenutzer.metadataLanguage}"));
            if (label == null) {
                label = dst.getName();
            }
            myTypes[zaehler] = new SelectItem(dst.getName(), label);
            zaehler++;
        }
        return myTypes;
    }

    /**
     * alle unbenutzen Metadaten des Docstruct löschen, Unterelemente rekursiv
     * aufrufen.
     */
    public void deleteAllUnusedElements(DocStructInterface inStruct) {
        inStruct.deleteUnusedPersonsAndMetadata();
        if (inStruct.getAllChildren() != null && inStruct.getAllChildren().size() > 0) {
            for (DocStructInterface child : inStruct.getAllChildren()) {
                deleteAllUnusedElements(child);
            }
        }
    }

    /**
     * die erste Imagenummer zurückgeben.
     */
    // FIXME: alphanumerisch
    public String getImageNumber(DocStructInterface inStrukturelement, int inPageNumber) {
        String rueckgabe = "";

        if (inStrukturelement == null) {
            return "";
        }
        List<ReferenceInterface> listReferenzen = inStrukturelement.getAllReferences("to");
        if (listReferenzen != null && listReferenzen.size() > 0) {
            /*
             * Referenzen sortieren
             */
            Collections.sort(listReferenzen, new Comparator<ReferenceInterface>() {
                @Override
                public int compare(final ReferenceInterface firstObject, final ReferenceInterface secondObject) {
                    Integer firstPage = 0;
                    Integer secondPage = 0;
                    final MetadataTypeInterface mdt = MetadatenHelper.this.prefsInterface.getMetadataTypeByName("physPageNumber");
                    List<? extends MetadataInterface> listMetadaten = firstObject.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        final MetadataInterface meineSeite = listMetadaten.get(0);
                        firstPage = Integer.parseInt(meineSeite.getValue());
                    }
                    listMetadaten = secondObject.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        final MetadataInterface meineSeite = listMetadaten.get(0);
                        secondPage = Integer.parseInt(meineSeite.getValue());
                    }
                    return firstPage.compareTo(secondPage);
                }
            });

            MetadataTypeInterface mdt = this.prefsInterface.getMetadataTypeByName("physPageNumber");
            List<? extends MetadataInterface> listSeiten = listReferenzen.get(0).getTarget().getAllMetadataByType(mdt);
            if (inPageNumber == PAGENUMBER_LAST) {
                listSeiten = listReferenzen.get(listReferenzen.size() - 1).getTarget().getAllMetadataByType(mdt);
            }
            if (listSeiten != null && listSeiten.size() > 0) {
                MetadataInterface meineSeite = listSeiten.get(0);
                rueckgabe += meineSeite.getValue();
            }
            mdt = this.prefsInterface.getMetadataTypeByName("logicalPageNumber");
            listSeiten = listReferenzen.get(0).getTarget().getAllMetadataByType(mdt);
            if (inPageNumber == PAGENUMBER_LAST) {
                listSeiten = listReferenzen.get(listReferenzen.size() - 1).getTarget().getAllMetadataByType(mdt);
            }
            if (listSeiten != null && listSeiten.size() > 0) {
                MetadataInterface meineSeite = listSeiten.get(0);
                rueckgabe += ":" + meineSeite.getValue();
            }
        }
        return rueckgabe;
    }

    /**
     * vom übergebenen DocStruct alle Metadaten ermitteln und um die fehlenden
     * DefaultDisplay-Metadaten ergänzen.
     */
    @SuppressWarnings("deprecation")
    public List<? extends MetadataInterface> getMetadataInclDefaultDisplay(DocStructInterface inStruct, String inLanguage,
            boolean inIsPerson, Process inProzess) {
        List<MetadataTypeInterface> displayMetadataTypes = inStruct.getDisplayMetadataTypes();
        /* sofern Default-Metadaten vorhanden sind, diese ggf. ergänzen */
        if (displayMetadataTypes != null) {
            for (MetadataTypeInterface mdt : displayMetadataTypes) {
                // check, if mdt is already in the allMDs Metadata list, if not
                // - add it
                if (!(inStruct.getAllMetadataByType(mdt) != null && inStruct.getAllMetadataByType(mdt).size() != 0)) {
                    try {
                        if (mdt.getIsPerson()) {
                            PersonInterface p = UghImplementation.INSTANCE.createPerson(mdt);
                            p.setRole(mdt.getName());
                            inStruct.addPerson(p);
                        } else {
                            MetadataInterface md = UghImplementation.INSTANCE.createMetadata(mdt);
                            inStruct.addMetadata(md); // add this new metadata
                            // element
                        }
                    } catch (DocStructHasNoTypeException | MetadataTypeNotAllowedException e) {
                    }
                }
            }
        }

        /*
         * wenn keine Sortierung nach Regelsatz erfolgen soll, hier alphabetisch
         * sortieren
         */
        if (inIsPerson) {
            List<PersonInterface> personInterfaces = inStruct.getAllPersons();
            if (personInterfaces != null && !inProzess.getRuleset().isOrderMetadataByRuleset()) {
                Collections.sort(personInterfaces, new MetadataComparator(inLanguage));
            }
            return personInterfaces;
        } else {
            List<MetadataInterface> metadataInterface = inStruct.getAllMetadata();
            if (metadataInterface != null && !inProzess.getRuleset().isOrderMetadataByRuleset()) {
                Collections.sort(metadataInterface, new MetadataComparator(inLanguage));
            }
            return getAllVisibleMetadataHack(inStruct);

        }
    }

    /** TODO: Replace it, after Maven is kicked :). */
    private List<MetadataInterface> getAllVisibleMetadataHack(DocStructInterface inStruct) {

        // Start with the list of all metadata.
        List<MetadataInterface> result = new LinkedList<>();

        // Iterate over all metadata.
        if (inStruct.getAllMetadata() != null) {
            for (MetadataInterface md : inStruct.getAllMetadata()) {
                // If the metadata has some value and it does not start with the
                // HIDDEN_METADATA_CHAR, add it to the result list.
                if (!md.getType().getName().startsWith("_")) {
                    result.add(md);
                }
            }
        }
        if (result.isEmpty()) {
            result = null;
        }
        return result;
    }

    /**
     * prüfen, ob es sich hier um eine rdf- oder um eine mets-Datei handelt.
     */
    public static String getMetaFileType(URI file) throws IOException {
        /*
         * Typen und Suchbegriffe festlegen
         */
        HashMap<String, String> types = new HashMap<>();
        types.put("metsmods", "ugh.fileformats.mets.MetsModsImportExport".toLowerCase());
        types.put("mets", "www.loc.gov/METS/".toLowerCase());
        types.put("rdf", "<RDF:RDF ".toLowerCase());
        types.put("xstream", "<ugh.dl.DigitalDocument>".toLowerCase());

        try (InputStreamReader input = new InputStreamReader(serviceManager.getFileService().read((file)),
                StandardCharsets.UTF_8); BufferedReader bufRead = new BufferedReader(input)) {
            char[] buffer = new char[200];
            while (bufRead.read(buffer) >= 0) {
                String temp = new String(buffer).toLowerCase();
                for (Entry<String, String> entry : types.entrySet()) {
                    if (temp.contains(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            }
        }
        return "-";
    }

    /**
     * Get Metadata type language.
     *
     * @param inMdt
     *            MetadataType object
     * @return localized Title of metadata type
     */
    public String getMetadatatypeLanguage(MetadataTypeInterface inMdt) {
        String label = inMdt
                .getLanguage((String) Helper.getManagedBeanValue("#{LoginForm.myBenutzer.metadataLanguage}"));
        if (label == null) {
            label = inMdt.getName();
        }
        return label;
    }

    /**
     * Comparator für die Metadaten.
     */
    // TODO: Uses generics, if possible
    public static class MetadataComparator implements Comparator<Object> {
        private String language = "de";

        public MetadataComparator(String inLanguage) {
            this.language = inLanguage;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @Override
        public int compare(Object firstObject, Object secondObject) {
            MetadataInterface firstMetadata = (MetadataInterface) firstObject;
            MetadataInterface secondMetadata = (MetadataInterface) secondObject;
            if (firstMetadata == null) {
                return -1;
            }
            if (secondMetadata == null) {
                return 1;
            }
            String firstName = "";
            String secondName = "";
            try {
                MetadataTypeInterface firstMetadataType = firstMetadata.getType();
                MetadataTypeInterface secondMetadataType = secondMetadata.getType();
                firstName = firstMetadataType.getNameByLanguage(this.language);
                secondName = secondMetadataType.getNameByLanguage(this.language);
            } catch (java.lang.NullPointerException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Language " + language + " for metadata " + firstMetadata.getType() + " or "
                            + secondMetadata.getType() + " is missing in ruleset");
                }
                return 0;
            }
            if (firstName == null || firstName.length() == 0) {
                firstName = firstMetadata.getType().getName();
                if (firstName == null) {
                    return -1;
                }
            }
            if (secondName == null || secondName.length() == 0) {
                secondName = secondMetadata.getType().getName();
                if (secondName == null) {
                    return 1;
                }
            }

            return firstName.compareToIgnoreCase(secondName);
        }
    }

    /**
     * Alle Rollen ermitteln, die für das übergebene Strukturelement erlaubt
     * sind.
     *
     * @param myDocStruct
     *            DocStruct object
     * @param inRoleName
     *            der aktuellen Person, damit diese ggf. in die Liste mit
     *            übernommen wird
     */
    public ArrayList<SelectItem> getAddablePersonRoles(DocStructInterface myDocStruct, String inRoleName) {
        ArrayList<SelectItem> myList = new ArrayList<>();
        /*
         * zuerst mal alle addierbaren Metadatentypen ermitteln
         */
        List<MetadataTypeInterface> types = myDocStruct.getPossibleMetadataTypes();
        if (types == null) {
            types = new ArrayList<>();
        }
        if (inRoleName != null && inRoleName.length() > 0) {
            boolean addRole = true;
            for (MetadataTypeInterface mdt : types) {
                if (mdt.getName().equals(inRoleName)) {
                    addRole = false;
                }
            }

            if (addRole) {
                types.add(this.prefsInterface.getMetadataTypeByName(inRoleName));
            }
        }
        /*
         * alle Metadatentypen, die keine Person sind, oder mit einem
         * Unterstrich anfangen rausnehmen
         */
        for (MetadataTypeInterface mdt : new ArrayList<>(types)) {
            if (!mdt.getIsPerson()) {
                types.remove(mdt);
            }
        }

        /*
         * die Metadatentypen sortieren
         */
        HelperComparator c = new HelperComparator();
        c.setSortType("MetadatenTypen");
        Collections.sort(types, c);

        for (MetadataTypeInterface mdt : types) {
            myList.add(new SelectItem(mdt.getName(), getMetadatatypeLanguage(mdt)));
        }
        return myList;
    }

    @Override
    public int compare(Object firstObject, Object secondObject) {
        String imageSorting = ConfigCore.getParameter("ImageSorting", "number");
        String firstString = (String) firstObject;
        String secondString = (String) secondObject;
        // comparing only prefixes of files:
        firstString = firstString.substring(0, firstString.lastIndexOf("."));
        secondString = secondString.substring(0, secondString.lastIndexOf("."));

        if (imageSorting.equalsIgnoreCase("number")) {
            try {
                Integer firstIterator = Integer.valueOf(firstString);
                Integer secondIterator = Integer.valueOf(secondString);
                return firstIterator.compareTo(secondIterator);
            } catch (NumberFormatException e) {
                return firstString.compareToIgnoreCase(secondString);
            }
        } else if (imageSorting.equalsIgnoreCase("alphanumeric")) {
            return firstString.compareToIgnoreCase(secondString);
        } else {
            return firstString.compareToIgnoreCase(secondString);
        }
    }

}
