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

package de.sub.kitodo.importer;

import de.sub.kitodo.helper.exceptions.WrongImportFileException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.exceptions.SwapException;
import org.kitodo.services.ServiceManager;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

/**
 * Die Klasse Schritt ist ein Bean für einen einzelnen Schritt mit dessen
 * Eigenschaften und erlaubt die Bearbeitung der Schrittdetails.
 *
 * @author Steffen Hankiewicz
 * @version 1.00 - 10.01.2005
 */
public class ImportRussland {
    private static final Logger myLogger = Logger.getLogger(ImportRussland.class);
    private DocStruct logicalTopstruct;
    private Process prozess;
    private final ServiceManager serviceManager = new ServiceManager();

    /**
     * Allgemeiner Konstruktor ().
     */
    public ImportRussland() {
    }

    /**
     * Parsen.
     *
     * @param reader
     *            BufferedReader object
     * @param inProzess
     *            Process object
     */
    protected void Parsen(BufferedReader reader, Process inProzess) throws IOException, WrongImportFileException,
            TypeNotAllowedForParentException, TypeNotAllowedAsChildException, MetadataTypeNotAllowedException,
            ReadException, InterruptedException, PreferencesException, SwapException, DAOException, WriteException {

        /*
         * prüfen, ob die Importdatei korrekt ist und wirklich zu dem Prozess
         * gehört
         */
        this.prozess = inProzess;
        String prozessID = String.valueOf(inProzess.getId().intValue());
        String line = reader.readLine();
        line = reader.readLine();
        line = reader.readLine();
        // myLogger.info(line + " : " + myProzesseID);
        if (line == null) {
            throw new WrongImportFileException("Importfehler: ungültige Importdatei oder falsche Kodierung");
        }

        if (!line.equals("+ " + prozessID + " (ProzessID)")) {
            throw new WrongImportFileException(
                    "Importfehler: Importdatei gehört zu einem anderen Werk ('" + prozessID + "' <> '" + line + "')");
        }

        /*
         * xml-Datei einlesen und Hauptelement ermitteln
         */
        Fileformat gdzfile = serviceManager.getProcessService().readMetadataFile(inProzess);
        DigitalDocument mydocument;
        mydocument = gdzfile.getDigitalDocument();
        this.logicalTopstruct = mydocument.getLogicalDocStruct();
        RussischeDatenLoeschen(this.logicalTopstruct);
        // if (1 == 1) {
        // gdzfile.Write(help.metadatenverzeichnis() + myProzesseID +
        // "/meta.xml");
        // return;
        // }

        /*
         * alle Zeilen durchlaufen
         */
        List<String> listeDaten = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            // myLogger.info(line);
            if (line.length() == 0) {

                /*
                 * immer wenn die Zeile leer ist, können die gesammelten Daten
                 * aus der gesammelten Liste ausgewertet werden
                 */
                AbsatzAuswerten(listeDaten);
                /* Liste wieder zurücksetzen */
                listeDaten = new ArrayList<String>();
            } else if (!line.substring(0, 1).equals("+")) {
                /*
                 * wenn zeile kein Kommentar ist, Zeile in Liste für Auswertung
                 * übernehmen
                 */
                if (line.length() > 3) {
                    listeDaten.add(line);
                }
            }
        }

        /*
         * Datei abschliessend wieder speichern
         */
        serviceManager.getProcessService().writeMetadataFile(gdzfile, inProzess);
        myLogger.debug("ParsenRussland() - Ende");
    }

    private void AbsatzAuswerten(List<String> inListe)
            throws ugh.exceptions.MetadataTypeNotAllowedException, WrongImportFileException {
        if (inListe.size() == 0) {
            return;
        }

        String detail = inListe.get(0);
        String meineDetailNr = detail.substring(0, 3);

        if (meineDetailNr.equals("080") || meineDetailNr.equals("090")) {
            ArtikelDetails(inListe);
        } else if (meineDetailNr.equals("010")) {
            ZeitschriftDetails(inListe);
        } else if (meineDetailNr.equals("050")) {
            BandDetails(inListe);
        } else {
            throw new WrongImportFileException("Parsingfehler: Neuer Block mit ungültigem ersten Identifier ('" + detail
                    + "'), möglicherweise sind an einer falschen Stelle Zeilenumbrüche eingefügt worden.");
        }
    }

    private void ZeitschriftDetails(List<String> inListe) throws MetadataTypeNotAllowedException {
        /* zunächst alle Details durchlaufen und der Zeitschrift hinzufügenl */
        for (Iterator<String> iter = inListe.iterator(); iter.hasNext();) {
            String meinDetail = iter.next();
            String meineDetailNr = meinDetail.substring(0, 3);
            // myLogger.debug("---- " + meinDetail);

            /* Zeitschrift Titel russisch */
            if (meineDetailNr.equals("020")) {
                MetadatumHinzufuegen(this.logicalTopstruct, "RUSMainTitle", meinDetail);
            }

            /* Zeitschrift Herausgeber (wiederholbar) */
            if (meineDetailNr.equals("030")) {
                MetadatumHinzufuegen(this.logicalTopstruct, "RUSPublisher", meinDetail);
            }

            /* Zeitschrift Ort (wiederholbar) */
            if (meineDetailNr.equals("040")) {
                MetadatumHinzufuegen(this.logicalTopstruct, "RUSPlaceOfPublication", meinDetail);
            }

            /* Verlag / Publishing house - russisch */
            if (meineDetailNr.equals("042")) {
                MetadatumHinzufuegen(this.logicalTopstruct, "RUSPublicationHouse", meinDetail);
            }
        }
    }

    private void BandDetails(List<String> inListe) throws MetadataTypeNotAllowedException {
        DocStruct ds = this.logicalTopstruct.getAllChildren().get(0);
        // myLogger.info(ds.getType().getName());
        /* zunächst alle Details durchlaufen und dem Band hinzufügenl */
        for (Iterator<String> iter = inListe.iterator(); iter.hasNext();) {
            String meinDetail = iter.next();
            String meineDetailNr = meinDetail.substring(0, 3);

            /* Band Herausgeber (wiederholbar) */
            if (meineDetailNr.equals("060")) {
                MetadatumHinzufuegen(ds, "RUSPublisher", meinDetail);
            }

            /* Band Ort (wiederholbar) */
            if (meineDetailNr.equals("070")) {
                MetadatumHinzufuegen(ds, "RUSPlaceOfPublication", meinDetail);
            }
        }
    }

    private void ArtikelDetails(List<String> inListe) throws MetadataTypeNotAllowedException, WrongImportFileException {
        boolean artikelGefunden = false;

        /*
         * zunächst alle Details durchlaufen und die ZBL-ID des Artikels
         * ermitteln
         */
        String zblID = "";
        for (Iterator<String> iter = inListe.iterator(); iter.hasNext();) {
            String meinDetail = iter.next();
            if (meinDetail.substring(0, 3).equals("090")) {
                // myLogger.info("ZBL-Identifier ist " +
                // meinDetail.substring(4).trim());
                zblID = meinDetail.substring(4).trim();
                break;
            }
        }

        /* für das Debugging bei Problemen */
        // if (zblID.equals("0843.11050"))
        // myLogger.warn("gesuchte ID");

        /*
         * alle Hefte und Artikel durchlaufen und den richtigen Artikel mit der
         * selben ZBL-ID finden
         */
        MetadataType mdt_id = serviceManager.getRulesetService().getPreferences(this.prozess.getRuleset())
                .getMetadataTypeByName("ZBLIdentifier");
        MetadataType mdt_tempId = serviceManager.getRulesetService().getPreferences(this.prozess.getRuleset())
                .getMetadataTypeByName("ZBLTempID");
        DocStruct band = this.logicalTopstruct.getAllChildren().get(0);
        // myLogger.info(band.getType().getName());
        List<DocStruct> listHefte = band.getAllChildren();
        if (listHefte != null) {
            for (Iterator<DocStruct> iter = listHefte.iterator(); iter.hasNext();) {
                DocStruct heft = iter.next();
                List<DocStruct> listArtikel = heft.getAllChildren();
                if (listArtikel != null) {
                    /*
                     * jetzt alle Artikel durchlaufen, bis der richtige Artikel
                     * gefunden wurde
                     */
                    for (Iterator<DocStruct> iter1 = listArtikel.iterator(); iter1.hasNext();) {
                        DocStruct artikel = iter1.next();
                        // myLogger.info(artikel.getType().getName());
                        if (artikel.getAllMetadataByType(mdt_id).size() > 0
                                || artikel.getAllMetadataByType(mdt_tempId).size() > 0) {
                            Metadata md;
                            if (artikel.getAllMetadataByType(mdt_id).size() > 0) {
                                md = artikel.getAllMetadataByType(mdt_id).get(0);
                            } else {
                                md = artikel.getAllMetadataByType(mdt_tempId).get(0);
                            }
                            // myLogger.debug(md.getValue());
                            if (md.getValue().equals(zblID)) {
                                // myLogger.info("------------ Artikel gefunden
                                // -------------");
                                artikelGefunden = true;
                                /*
                                 * jetzt alle Details durchlaufen und dem
                                 * Artikel hinzufügenl
                                 */
                                for (Iterator<String> iter2 = inListe.iterator(); iter2.hasNext();) {
                                    String meinDetail = iter2.next();
                                    String meineDetailNr = meinDetail.substring(0, 3);

                                    /* Artikel Autor russisch (wiederholbar) */
                                    if (meineDetailNr.equals("120")) {
                                        PersonHinzufuegen(artikel, "Author", meinDetail);
                                    }

                                    /* Artikel Autor-Variation (wiederholbar) */
                                    if (meineDetailNr.equals("130")) {
                                        PersonHinzufuegen(artikel, "AuthorVariation", meinDetail);
                                    }

                                    /*
                                     * Artikel Autor-Kontributor (wiederholbar)
                                     */
                                    if (meineDetailNr.equals("140")) {
                                        PersonHinzufuegen(artikel, "Contributor", meinDetail);
                                    }

                                    /*
                                     * Artikel Person als Subjekt des Artikels
                                     * (wiederholbar)
                                     */
                                    if (meineDetailNr.equals("150")) {
                                        MetadatumHinzufuegen(artikel, "PersonAsSubject", meinDetail);
                                    }

                                    /* Artikel Titel russisch */
                                    if (meineDetailNr.equals("170")) {
                                        MetadatumHinzufuegen(artikel, "RUSMainTitle", meinDetail);
                                    }

                                    /*
                                     * Artikel Klassifikation UDK (wiederholbar)
                                     */
                                    if (meineDetailNr.equals("190")) {
                                        MetadatumHinzufuegen(artikel, "ClassificationUDK", meinDetail);
                                    }

                                    /* Artikel Keywords russisch */
                                    if (meineDetailNr.equals("210")) {
                                        MetadatumHinzufuegen(artikel, "RUSKeyword", meinDetail);
                                    }
                                }
                                return;
                            }
                        }
                    }
                }

                if (!iter.hasNext() && !artikelGefunden) {
                    throw new WrongImportFileException(
                            "Parsingfehler: Artikel mit der ZBL-ID wurde nicht gefunden ('" + zblID + "')");
                }
            }
        } else {
            throw new WrongImportFileException(
                    "Parsingfehler: Es sind bisher keine Artikel angelegt worden, zu denen Daten ergänzt werden könnten");
        }
    }

    private void RussischeDatenLoeschen(DocStruct inStrukturelement) {
        /*
         * von dem aktuellen Stukturelement alle Metadaten durchlaufen und das
         * gesuchte löschen
         */
        if (inStrukturelement.getAllVisibleMetadata() != null) {
            LinkedList<Metadata> listMetas = new LinkedList<Metadata>(inStrukturelement.getAllMetadata());
            for (Iterator<Metadata> iter = listMetas.iterator(); iter.hasNext();) {
                Metadata meta = iter.next();
                String myMetaName = meta.getType().getName();

                /*
                 * wenn die Metadatentypen die russischen sind, werden sie aus
                 * der Liste entfernt
                 */
                if (myMetaName.equals("PersonAsSubject") || myMetaName.equals("RUSMainTitle")
                        || myMetaName.equals("ClassificationUDK") || myMetaName.equals("RUSKeyword")
                        || myMetaName.equals("RUSPublisher") || myMetaName.equals("RUSPlaceOfPublication")
                        || myMetaName.equals("RUSPublicationHouse") || myMetaName.equals("RUSPublisher")) {
                    inStrukturelement.removeMetadata(meta);
                }
            }
        }

        /*
         * von dem aktuellen Stukturelement alle Personen durchlaufen und die
         * gesuchten löschen
         */
        if (inStrukturelement.getAllPersons() != null) {
            List<Person> listPersons = new ArrayList<Person>(inStrukturelement.getAllPersons());
            for (Person p : listPersons) {
                if (p.getRole().equals("Author")) {
                    inStrukturelement.removePerson(p);
                }
            }
        }

        /*
         * von dem aktuellen Stukturelement alle Kinder durchlaufen und rekursiv
         * durchlaufen
         */
        List<DocStruct> listKinder = inStrukturelement.getAllChildren();
        if (listKinder != null) {
            /* es gibt Kinder-Strukturelemente, also alle Kinder durchlaufen */
            for (DocStruct kind : listKinder) {
                RussischeDatenLoeschen(kind);
            }
        }
    }

    private void MetadatumHinzufuegen(DocStruct inStruct, String inMdtName, String inDetail)
            throws MetadataTypeNotAllowedException {
        MetadataType mdt = serviceManager.getRulesetService().getPreferences(this.prozess.getRuleset())
                .getMetadataTypeByName(inMdtName);
        Metadata md = new Metadata(mdt);
        try {
            md.setValue(inDetail.substring(4).trim());

            /*
             * prüfen, ob das Metadatum schon existiert, wenn nein, neu anlegen
             */

            // LinkedList list = inStruct.getAllChildren();
            // if (list != null) {
            //
            // /* jetzt alle Artikel durchlaufen, bis der richtige Artikel
            // gefunden wurde */
            // for (Iterator iter1 = listArtikel.iterator(); iter1.hasNext();) {
            // DocStruct artikel = (DocStruct) iter1.next();
            // Metadata md = (Metadata)
            // artikel.getAllMetadataByType(mdt).getFirst();
            // myLogger.debug(md.getValue());
            // if (md.getValue().equals(zblID)) {
            // myLogger.info("------------ Artikel gefunden -------------");
            inStruct.addMetadata(md);
        } catch (Exception e) {
            myLogger.error("Import fehlgeschlagen: " + inDetail, e);
        }
    }

    private void PersonHinzufuegen(DocStruct inStruct, String inRole, String inDetail)
            throws MetadataTypeNotAllowedException, WrongImportFileException {
        Person p = new Person(serviceManager.getRulesetService().getPreferences(this.prozess.getRuleset())
                .getMetadataTypeByName(inRole));
        String pName = inDetail.substring(4).trim();
        if (pName.length() == 0) {
            return;
        }
        if (pName.indexOf(",") == -1) {
            throw new WrongImportFileException(
                    "Parsingfehler: Vorname nicht mit Komma vom Nachnamen getrennt ('" + inDetail + "')");
        }
        p.setLastname(pName.substring(0, pName.indexOf(",")).trim());
        p.setFirstname(pName.substring(pName.indexOf(",") + 1, pName.length()).trim());
        p.setRole(inRole);
        // MetadataType mdt =
        // prozess.getRegelsatz().getPreferences().getMetadataTypeByName(inRole);
        // p.setType(mdt);
        inStruct.addPerson(p);
    }
}
