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

package de.sub.goobi.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.kitodo.data.database.beans.Process;
import org.kitodo.exceptions.WrongImportFileException;
import org.kitodo.helper.Helper;
import org.kitodo.legacy.UghImplementation;
import org.kitodo.services.ServiceManager;

/**
 * Die Klasse Schritt ist ein Bean für einen einzelnen Schritt mit dessen
 * Eigenschaften und erlaubt die Bearbeitung der Schrittdetails.
 *
 * @author Steffen Hankiewicz
 * @version 1.00 - 10.01.2005
 */
public class ImportRussland {
    private static final Logger logger = LogManager.getLogger(ImportRussland.class);
    private DocStructInterface logicalTopstruct;
    private Process process;
    private final ServiceManager serviceManager = new ServiceManager();

    /**
     * Parse.
     *
     * @param reader
     *            BufferedReader object
     * @param inProzess
     *            Process object
     */
    protected void parse(BufferedReader reader, Process inProzess) throws IOException, WrongImportFileException,
            MetadataTypeNotAllowedException, ReadException, PreferencesException, WriteException {

        // check if the import file is correct and really belongs to the process
        this.process = inProzess;
        String processId = String.valueOf(this.process.getId().intValue());
        String line = reader.readLine();
        if (line == null) {
            throw new WrongImportFileException("Importfehler: ungültige Importdatei oder falsche Kodierung");
        }

        if (!line.equals("+ " + processId + " (ProzessID)")) {
            throw new WrongImportFileException(
                    "Importfehler: Importdatei gehört zu einem anderen Werk ('" + processId + "' <> '" + line + "')");
        }

        // import the XML file and determine the main element
        FileformatInterface gdzfile = serviceManager.getProcessService().readMetadataFile(this.process);
        DigitalDocumentInterface digitalDocument = gdzfile.getDigitalDocument();
        this.logicalTopstruct = digitalDocument.getLogicalDocStruct();
        deleteRussianData(this.logicalTopstruct);

        // go through all lines
        List<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            // logger.info(line);
            if (line.length() == 0) {

                /*
                 * always when the line is empty, the collected data from the
                 * collected list can be evaluated
                 */
                analyzeParagraph(lines);
                // reset list
                lines = new ArrayList<>();
            } else if (!line.substring(0, 1).equals("+") && line.length() > 3) {
                // if line is not a comment, accept the line in the list for
                // evaluation
                lines.add(line);
            }
        }

        // finally save the file again
        serviceManager.getFileService().writeMetadataFile(gdzfile, inProzess);
        logger.debug("ParsenRussland() - Ende");
    }

    private void analyzeParagraph(List<String> list) throws MetadataTypeNotAllowedException, WrongImportFileException {
        if (list.isEmpty()) {
            return;
        }

        String detail = list.get(0);
        String detailNr = detail.substring(0, 3);

        switch (detailNr) {
            case "080":
            case "090":
                setArticleDetails(list);
                break;
            case "010":
                setJournalDetails(list);
                break;
            case "050":
                setBandDetails(list);
                break;
            default:
                throw new WrongImportFileException("Parsingfehler: Neuer Block mit ungültigem ersten Identifier ('" + detail
                        + "'), möglicherweise sind an einer falschen Stelle Zeilenumbrüche eingefügt worden.");
        }
    }

    private void setJournalDetails(List<String> list) throws MetadataTypeNotAllowedException {
        // go through all the details and add to the journal
        for (String journalDetail : list) {
            String detailNr = journalDetail.substring(0, 3);

            // Zeitschrift Titel russisch
            if (detailNr.equals("020")) {
                addMetadata(this.logicalTopstruct, "RUSMainTitle", journalDetail);
            }

            // Zeitschrift Herausgeber (wiederholbar)
            if (detailNr.equals("030")) {
                addMetadata(this.logicalTopstruct, "RUSPublisher", journalDetail);
            }

            // Zeitschrift Ort (wiederholbar)
            if (detailNr.equals("040")) {
                addMetadata(this.logicalTopstruct, "RUSPlaceOfPublication", journalDetail);
            }

            // Verlag / Publishing house - russisch
            if (detailNr.equals("042")) {
                addMetadata(this.logicalTopstruct, "RUSPublicationHouse", journalDetail);
            }
        }
    }

    private void setBandDetails(List<String> list) throws MetadataTypeNotAllowedException {
        DocStructInterface ds = this.logicalTopstruct.getAllChildren().get(0);

        // go through all the details and add it to the band
        for (String bandDetail : list) {
            String detailNr = bandDetail.substring(0, 3);

            // Band Herausgeber (wiederholbar)
            if (detailNr.equals("060")) {
                addMetadata(ds, "RUSPublisher", bandDetail);
            }

            // Band Ort (wiederholbar)
            if (detailNr.equals("070")) {
                addMetadata(ds, "RUSPlaceOfPublication", bandDetail);
            }
        }
    }

    private void setArticleDetails(List<String> list) throws MetadataTypeNotAllowedException, WrongImportFileException {
        String zblID = getZBLID(list);

        /*
         * alle Hefte und Artikel durchlaufen und den richtigen Artikel mit der
         * selben ZBL-ID finden
         */
        PrefsInterface preferences = serviceManager.getRulesetService().getPreferences(this.process.getRuleset());
        MetadataTypeInterface metadataTypeId = preferences.getMetadataTypeByName("ZBLIdentifier");
        MetadataTypeInterface metadataTypeTempId = preferences.getMetadataTypeByName("ZBLTempID");
        DocStructInterface band = this.logicalTopstruct.getAllChildren().get(0);

        List<DocStructInterface> listHefte = band.getAllChildren();
        if (listHefte != null) {
            for (Iterator<DocStructInterface> iter = listHefte.iterator(); iter.hasNext();) {
                DocStructInterface heft = iter.next();
                List<DocStructInterface> listArticle = heft.getAllChildren();
                if (listArticle != null) {
                    // go through all the articles until the right article is
                    // found
                    for (DocStructInterface article : listArticle) {
                        List<? extends MetadataInterface> allMetadataByType = article.getAllMetadataByType(metadataTypeId);
                        List<? extends MetadataInterface> allMetadataByTempType = article.getAllMetadataByType(metadataTypeTempId);
                        if (!allMetadataByType.isEmpty() || !allMetadataByTempType.isEmpty()) {
                            MetadataInterface md;
                            if (!allMetadataByType.isEmpty()) {
                                md = allMetadataByType.get(0);
                            } else {
                                md = allMetadataByTempType.get(0);
                            }
                            // logger.debug(md.getValue());
                            if (md.getValue().equals(zblID)) {
                                iterateOverDetailsAndAddToArticle(list, article);
                                return;
                            }
                        }
                    }
                }

                if (!iter.hasNext()) {
                    throw new WrongImportFileException(
                            "Parsingfehler: Artikel mit der ZBL-ID wurde nicht gefunden ('" + zblID + "')");
                }
            }
        } else {
            throw new WrongImportFileException(
                    "Parsingfehler: Es sind bisher keine Artikel angelegt worden, zu denen Daten ergänzt werden könnten");
        }
    }

    /**
     * Go through all the details and determine the ZBL-ID of the article.
     *
     * @param list
     *            of Strings
     * @return ZBL-ID
     */
    private String getZBLID(List<String> list) {
        String zblID = "";
        for (String articleDetail : list) {
            if (articleDetail.substring(0, 3).equals("090")) {
                zblID = articleDetail.substring(4).trim();
                break;
            }
        }
        return zblID;
    }

    private void iterateOverDetailsAndAddToArticle(List<String> list, DocStructInterface article)
            throws MetadataTypeNotAllowedException, WrongImportFileException {
        for (String detail : list) {
            String detailNr = detail.substring(0, 3);

            // article author Russian (repeatable)
            if (detailNr.equals("120")) {
                addPerson(article, "Author", detail);
            }

            // article author variation (repeatable)
            if (detailNr.equals("130")) {
                addPerson(article, "AuthorVariation", detail);
            }

            // article author contributor (repeatable)
            if (detailNr.equals("140")) {
                addPerson(article, "Contributor", detail);
            }

            // article person as subject of the article (repeatable)
            if (detailNr.equals("150")) {
                addMetadata(article, "PersonAsSubject", detail);
            }

            // article title Russian
            if (detailNr.equals("170")) {
                addMetadata(article, "RUSMainTitle", detail);
            }

            // article classification UDK (repeatable)
            if (detailNr.equals("190")) {
                addMetadata(article, "ClassificationUDK", detail);
            }

            // article keywords Russian
            if (detailNr.equals("210")) {
                addMetadata(article, "RUSKeyword", detail);
            }
        }
    }

    private void deleteRussianData(DocStructInterface docStruct) {
        /*
         * von dem aktuellen Stukturelement alle Metadaten durchlaufen und das
         * gesuchte löschen
         */
        if (docStruct.getAllVisibleMetadata() != null) {
            LinkedList<MetadataInterface> listMetas = new LinkedList<>(docStruct.getAllMetadata());
            for (MetadataInterface meta : listMetas) {
                String metaName = meta.getMetadataType().getName();

                /*
                 * wenn die Metadatentypen die russischen sind, werden sie aus
                 * der Liste entfernt
                 */
                if (metaName.equals("PersonAsSubject") || metaName.equals("RUSMainTitle")
                        || metaName.equals("ClassificationUDK") || metaName.equals("RUSKeyword")
                        || metaName.equals("RUSPublisher") || metaName.equals("RUSPlaceOfPublication")
                        || metaName.equals("RUSPublicationHouse")) {
                    docStruct.removeMetadata(meta);
                }
            }
        }

        /*
         * von dem aktuellen Stukturelement alle Personen durchlaufen und die
         * gesuchten löschen
         */
        if (docStruct.getAllPersons() != null) {
            List<PersonInterface> listPersons = new ArrayList<>(docStruct.getAllPersons());
            for (PersonInterface p : listPersons) {
                if (p.getRole().equals("Author")) {
                    docStruct.removePerson(p);
                }
            }
        }

        deleteRussianDataForChildren(docStruct);
    }

    /**
     * Traversing all children from the current structure element
     * and iterating through recursively.
     */
    private void deleteRussianDataForChildren(DocStructInterface docStruct) {
        List<DocStructInterface> children = docStruct.getAllChildren();
        if (Objects.nonNull(children)) {
            // there are children's structural elements, so go through them
            for (DocStructInterface child : children) {
                deleteRussianData(child);
            }
        }
    }

    private void addMetadata(DocStructInterface inStruct, String inMdtName, String inDetail)
            throws MetadataTypeNotAllowedException {
        MetadataTypeInterface mdt = serviceManager.getRulesetService().getPreferences(this.process.getRuleset())
                .getMetadataTypeByName(inMdtName);
        MetadataInterface md = UghImplementation.INSTANCE.createMetadata(mdt);
        try {
            md.setStringValue(inDetail.substring(4).trim());
            inStruct.addMetadata(md);
        } catch (RuntimeException e) {
            logger.error("Import fehlgeschlagen: " + inDetail, e);
        }
    }

    private void addPerson(DocStructInterface inStruct, String inRole, String inDetail)
            throws MetadataTypeNotAllowedException, WrongImportFileException {
        PersonInterface p = UghImplementation.INSTANCE.createPerson(
            serviceManager.getRulesetService().getPreferences(this.process.getRuleset()).getMetadataTypeByName(inRole));
        String pName = inDetail.substring(4).trim();
        if (pName.length() == 0) {
            return;
        }
        if (!pName.contains(",")) {
            String message = Helper.getTranslation("errorParsingName", Collections.singletonList(inDetail));
            throw new WrongImportFileException(message);
        }
        p.setLastName(pName.substring(0, pName.indexOf(',')).trim());
        p.setFirstName(pName.substring(pName.indexOf(',') + 1, pName.length()).trim());
        p.setRole(inRole);
        // TODO: should be this data inserted?
        // MetadataType mdt =
        // process.getRegelsatz().getPreferences().getMetadataTypeByName(inRole);
        // p.setType(mdt);
        inStruct.addPerson(p);
    }
}
