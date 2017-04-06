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

package de.sub.kitodo.metadaten;

import de.sub.kitodo.config.ConfigCore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Bean für die Sperrung der Metadaten.
 */
public class MetadatenSperrung implements Serializable {
    private static final long serialVersionUID = -8248209179063050307L;
    private static HashMap<Integer, HashMap<String, String>> sperrungen = new HashMap<Integer, HashMap<String, String>>();
    /*
     * Zeit, innerhalb der der Benutzer handeln muss, um seine Sperrung zu
     * behalten (30 min)
     */
    private static final long sperrzeit = ConfigCore.getLongParameter("MetsEditorLockingTime", 30 * 60 * 1000);

    /**
     * Metadaten eines bestimmten Prozesses wieder freigeben.
     */
    public void setFree(int ProzessID) {
        if (sperrungen.containsKey(ProzessID)) {
            sperrungen.remove(ProzessID);
        }
    }

    /**
     * Metadaten eines bestimmten Prozesses für einen Benutzer sperren.
     */
    public void setLocked(int ProzessID, String BenutzerID) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Benutzer", BenutzerID);
        map.put("Lebenszeichen", String.valueOf(System.currentTimeMillis()));
        sperrungen.put(ProzessID, map);
    }

    /**
     * prüfen, ob bestimmte Metadaten noch durch anderen Benutzer gesperrt sind.
     */
    public static boolean isLocked(int ProzessID) {
        HashMap<String, String> temp = sperrungen.get(Integer.valueOf(ProzessID));
        /* wenn der Prozess nicht in der Hashpmap ist, ist er nicht gesperrt */
        if (temp == null) {
            return false;
        } else {
            /* wenn er in der Hashmap ist, muss die Zeit geprüft werden */
            long lebenszeichen = Long.parseLong(temp.get("Lebenszeichen"));
            if (lebenszeichen < System.currentTimeMillis() - sperrzeit) {
                /*
                 * wenn die Zeit Größer ist als erlaubt, ist Metadatum nicht
                 * gesperrt
                 */
                return false;
            }
            /* wenn Zeit nicht Größer ist, ist er noch gesperrt */
            else {
                return true;
            }
        }
    }

    /**
     * Java doc.
     *
     * @param inBenutzerID
     *            Integer
     */
    public void alleBenutzerSperrungenAufheben(Integer inBenutzerID) {
        String inBenutzerString = String.valueOf(inBenutzerID.intValue());
        HashMap<Integer, HashMap<String, String>> temp = new HashMap<Integer, HashMap<String, String>>(sperrungen);
        for (Iterator<Integer> iter = temp.keySet().iterator(); iter.hasNext();) {
            Integer myKey = iter.next();
            HashMap<String, String> intern = sperrungen.get(myKey);
            if (intern.get("Benutzer").equals(inBenutzerString)) {
                sperrungen.remove(myKey);
            }
        }
    }

    /**
     * Benutzer zurückgeben, der Metadaten gesperrt hat.
     */
    public String getLockBenutzer(int ProzessID) {
        String rueckgabe = "-1";
        HashMap<String, String> temp = sperrungen.get(ProzessID);
        /*
         * wenn der Prozess nicht in der Hashpmap ist, gibt es keinen Benutzer
         */
        if (temp != null) {
            rueckgabe = temp.get("Benutzer");
        }
        return rueckgabe;
    }

    /**
     * Remove lock for process.
     *
     * @param ProzessID
     *            Id of process to unlock
     */
    public static void UnlockProcess(int ProzessID) {
        HashMap<String, String> temp = sperrungen.get(ProzessID);
        /* wenn der Prozess in der Hashpmap ist, dort rausnehmen */
        if (temp != null) {
            sperrungen.remove(ProzessID);
        }
    }

    /**
     * Sekunden zurückgeben, seit der letzten Bearbeitung der Metadaten.
     */
    public long getLockSekunden(int ProzessID) {
        HashMap<String, String> temp = sperrungen.get(ProzessID);
        /* wenn der Prozess nicht in der Hashmap ist, gibt es keine Zeit */
        if (temp == null) {
            return 0;
        } else {
            return (System.currentTimeMillis() - Long.parseLong(temp.get("Lebenszeichen"))) / 1000;
        }
    }
}
