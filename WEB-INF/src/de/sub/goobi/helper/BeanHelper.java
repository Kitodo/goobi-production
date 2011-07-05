package de.sub.goobi.helper;

//TODO: Get rid of the Iterators, use for loops
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.sub.goobi.Beans.Benutzer;
import de.sub.goobi.Beans.Benutzergruppe;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Beans.Prozesseigenschaft;
import de.sub.goobi.Beans.Schritt;
import de.sub.goobi.Beans.Schritteigenschaft;
import de.sub.goobi.Beans.Vorlage;
import de.sub.goobi.Beans.Vorlageeigenschaft;
import de.sub.goobi.Beans.Werkstueck;
import de.sub.goobi.Beans.Werkstueckeigenschaft;

//TODO: Document this
public class BeanHelper {

	

	public void EigenschaftHinzufuegen(Prozess inProzess, String inTitel, String inWert) {
		Prozesseigenschaft eig = new Prozesseigenschaft();
		eig.setTitel(inTitel);
		eig.setWert(inWert);
		eig.setProzess(inProzess);
		Set<Prozesseigenschaft> eigenschaften = inProzess.getEigenschaften();
		if (eigenschaften == null)
			eigenschaften = new HashSet<Prozesseigenschaft>();
		eigenschaften.add(eig);
	}

	

	public void EigenschaftHinzufuegen(Schritt inSchritt, String inTitel, String inWert) {
		Schritteigenschaft eig = new Schritteigenschaft();
		eig.setTitel(inTitel);
		eig.setWert(inWert);
		eig.setSchritt(inSchritt);
		Set<Schritteigenschaft> eigenschaften = inSchritt.getEigenschaften();
		if (eigenschaften == null)
			eigenschaften = new HashSet<Schritteigenschaft>();
		eigenschaften.add(eig);
	}

	

	public void EigenschaftHinzufuegen(Vorlage inVorlage, String inTitel, String inWert) {
		Vorlageeigenschaft eig = new Vorlageeigenschaft();
		eig.setTitel(inTitel);
		eig.setWert(inWert);
		eig.setVorlage(inVorlage);
		Set<Vorlageeigenschaft> eigenschaften = inVorlage.getEigenschaften();
		if (eigenschaften == null)
			eigenschaften = new HashSet<Vorlageeigenschaft>();
		eigenschaften.add(eig);
	}

	

	public void EigenschaftHinzufuegen(Werkstueck inWerkstueck, String inTitel, String inWert) {
		Werkstueckeigenschaft eig = new Werkstueckeigenschaft();
		eig.setTitel(inTitel);
		eig.setWert(inWert);
		eig.setWerkstueck(inWerkstueck);
		Set<Werkstueckeigenschaft> eigenschaften = inWerkstueck.getEigenschaften();
		if (eigenschaften == null)
			eigenschaften = new HashSet<Werkstueckeigenschaft>();
		eigenschaften.add(eig);
	}

	

	public void SchritteKopieren(Prozess prozessVorlage, Prozess prozessKopie) {
		HashSet<Schritt> mySchritte = new HashSet<Schritt>();
		for (Schritt step : prozessVorlage.getSchritteList()) {

			/* --------------------------------
			 * Details des Schritts
			 * --------------------------------*/
			//TODO: Maybe we just need a clone method for a process?
			Schritt stepneu = new Schritt();
			stepneu.setTypAutomatisch(step.isTypAutomatisch());
			stepneu.setAllScripts(step.getAllScripts());
			stepneu.setTypBeimAnnehmenAbschliessen(step.isTypBeimAnnehmenAbschliessen());
			stepneu.setTypBeimAnnehmenModul(step.isTypBeimAnnehmenModul());
			stepneu.setTypBeimAnnehmenModulUndAbschliessen(step.isTypBeimAnnehmenModulUndAbschliessen());
			stepneu.setTypExportDMS(step.isTypExportDMS());
			stepneu.setTypExportRus(step.isTypExportRus());
			stepneu.setTypImagesLesen(step.isTypImagesLesen());
			stepneu.setTypImagesSchreiben(step.isTypImagesSchreiben());
			stepneu.setTypImportFileUpload(step.isTypImportFileUpload());
			stepneu.setTypMetadaten(step.isTypMetadaten());
			stepneu.setPrioritaet(step.getPrioritaet());
			stepneu.setBearbeitungsstatusEnum(step.getBearbeitungsstatusEnum());
			stepneu.setReihenfolge(step.getReihenfolge());
			stepneu.setTitel(step.getTitel());
			//         stepneu.setTyp(step.getTyp());
			stepneu.setHomeverzeichnisNutzen(step.getHomeverzeichnisNutzen());
			stepneu.setProzess(prozessKopie);
			
			//Fixing a bug found by Holger Busse (Berlin)
			stepneu.setTypBeimAbschliessenVerifizieren(step.isTypBeimAbschliessenVerifizieren());

			/* --------------------------------
			 * Eigenschaften des Schritts
			 * --------------------------------*/
			HashSet<Schritteigenschaft> myEigenschaften = new HashSet<Schritteigenschaft>();
			for (Schritteigenschaft eig : step.getEigenschaftenList()) {
				Schritteigenschaft eigneu = new Schritteigenschaft();
				eigneu.setIstObligatorisch(eig.isIstObligatorisch());
				eigneu.setType(eig.getType());
				eigneu.setTitel(eig.getTitel());
				eigneu.setWert(eig.getWert());
				eigneu.setSchritt(stepneu);
				myEigenschaften.add(eigneu);
			}
			stepneu.setEigenschaften(myEigenschaften);

			/* --------------------------------
			 * Benutzer übernehmen
			 * --------------------------------*/
			HashSet<Benutzer> myBenutzer = new HashSet<Benutzer>();
			for (Benutzer benneu : step.getBenutzer()) {
				myBenutzer.add(benneu);
			}
			stepneu.setBenutzer(myBenutzer);

			/* --------------------------------
			 * Benutzergruppen übernehmen
			 * --------------------------------*/
			HashSet<Benutzergruppe> myBenutzergruppen = new HashSet<Benutzergruppe>();
			for (Benutzergruppe grupneu : step.getBenutzergruppen()) {
				myBenutzergruppen.add(grupneu);
			}
			stepneu.setBenutzergruppen(myBenutzergruppen);

			/* Schritt speichern */
			mySchritte.add(stepneu);
		}
		prozessKopie.setSchritte(mySchritte);
	}

	public void WerkstueckeKopieren(Prozess prozessVorlage, Prozess prozessKopie) {
		HashSet<Werkstueck> myWerkstuecke = new HashSet<Werkstueck>();
		for (Werkstueck werk : prozessVorlage.getWerkstuecke()) {
			/* --------------------------------
			 * Details des Werkstücks
			 * --------------------------------*/
			Werkstueck werkneu = new Werkstueck();
			werkneu.setProzess(prozessKopie);

			/* --------------------------------
			 * Eigenschaften des Schritts
			 * --------------------------------*/
			HashSet<Werkstueckeigenschaft> myEigenschaften = new HashSet<Werkstueckeigenschaft>();
			for (Iterator<Werkstueckeigenschaft> iterator = werk.getEigenschaften().iterator(); iterator.hasNext();) {
				Werkstueckeigenschaft eig = iterator.next();
				Werkstueckeigenschaft eigneu = new Werkstueckeigenschaft();
				eigneu.setIstObligatorisch(eig.isIstObligatorisch());
				eigneu.setDatentyp(eig.getDatentyp());
				eigneu.setTitel(eig.getTitel());
				eigneu.setWert(eig.getWert());
				eigneu.setWerkstueck(werkneu);
				myEigenschaften.add(eigneu);
			}
			werkneu.setEigenschaften(myEigenschaften);

			/* Schritt speichern */
			myWerkstuecke.add(werkneu);
		}
		prozessKopie.setWerkstuecke(myWerkstuecke);
	}

	public void EigenschaftenKopieren(Prozess prozessVorlage, Prozess prozessKopie) {
		HashSet<Prozesseigenschaft> myEigenschaften = new HashSet<Prozesseigenschaft>();
		for (Iterator<Prozesseigenschaft> iterator = prozessVorlage.getEigenschaften().iterator(); iterator.hasNext();) {
			Prozesseigenschaft eig = iterator.next();
			Prozesseigenschaft eigneu = new Prozesseigenschaft();
			eigneu.setIstObligatorisch(eig.isIstObligatorisch());
			eigneu.setDatentyp(eig.getDatentyp());
			eigneu.setTitel(eig.getTitel());
			eigneu.setWert(eig.getWert());
			eigneu.setProzess(prozessKopie);
			myEigenschaften.add(eigneu);
		}
		prozessKopie.setEigenschaften(myEigenschaften);
	}

	public void ScanvorlagenKopieren(Prozess prozessVorlage, Prozess prozessKopie) {
		HashSet<Vorlage> myVorlagen = new HashSet<Vorlage>();
		for (Vorlage vor : prozessVorlage.getVorlagen()) {
			/* --------------------------------
			 * Details der Vorlage
			 * --------------------------------*/
			Vorlage vorneu = new Vorlage();
			vorneu.setHerkunft(vor.getHerkunft());
			vorneu.setProzess(prozessKopie);

			/* --------------------------------
			 * Eigenschaften des Schritts
			 * --------------------------------*/
			HashSet<Vorlageeigenschaft> myEigenschaften = new HashSet<Vorlageeigenschaft>();
			for (Iterator<Vorlageeigenschaft> iterator = vor.getEigenschaften().iterator(); iterator.hasNext();) {
				Vorlageeigenschaft eig = iterator.next();
				Vorlageeigenschaft eigneu = new Vorlageeigenschaft();
				eigneu.setIstObligatorisch(eig.isIstObligatorisch());
				eigneu.setDatentyp(eig.getDatentyp());
				eigneu.setTitel(eig.getTitel());
				eigneu.setWert(eig.getWert());
				eigneu.setVorlage(vorneu);
				myEigenschaften.add(eigneu);
			}
			vorneu.setEigenschaften(myEigenschaften);

			/* Schritt speichern */
			myVorlagen.add(vorneu);
		}
		prozessKopie.setVorlagen(myVorlagen);
	}

	public String WerkstueckEigenschaftErmitteln(Prozess myProzess, String inEigenschaft) {
		String Eigenschaft = "";
		for (Werkstueck myWerkstueck : myProzess.getWerkstueckeList()) {
			for (Werkstueckeigenschaft eigenschaft : myWerkstueck.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft))
					Eigenschaft = eigenschaft.getWert();
			}
		}
		return Eigenschaft;
	}

	public String ScanvorlagenEigenschaftErmitteln(Prozess myProzess, String inEigenschaft) {
		String Eigenschaft = "";
		for (Vorlage myVorlage : myProzess.getVorlagenList()) {
			for (Vorlageeigenschaft eigenschaft : myVorlage.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft))
					Eigenschaft = eigenschaft.getWert();
			}
		}
		return Eigenschaft;
	}

	public void WerkstueckEigenschaftAendern(Prozess myProzess, String inEigenschaft, String inWert) {
		for (Werkstueck myWerkstueck : myProzess.getWerkstueckeList()) {
			for (Werkstueckeigenschaft eigenschaft : myWerkstueck.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft))
					eigenschaft.setWert(inWert);
			}
		}
	}

	public void ScanvorlagenEigenschaftAendern(Prozess myProzess, String inEigenschaft, String inWert) {
		for (Vorlage myVorlage : myProzess.getVorlagenList()) {
			for (Vorlageeigenschaft eigenschaft : myVorlage.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft))
					eigenschaft.setWert(inWert);
			}
		}
	}

	public void WerkstueckEigenschaftLoeschen(Prozess myProzess, String inEigenschaft, String inWert) {
		for (Werkstueck myWerkstueck : myProzess.getWerkstueckeList()) {
			for (Werkstueckeigenschaft eigenschaft : myWerkstueck.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft) && eigenschaft.getWert().equals(inWert))
					myWerkstueck.getEigenschaften().remove(eigenschaft);
			}
		}
	}

	public void ScanvorlagenEigenschaftLoeschen(Prozess myProzess, String inEigenschaft, String inWert) {
		for (Vorlage myVorlage : myProzess.getVorlagenList()) {
			for (Vorlageeigenschaft eigenschaft : myVorlage.getEigenschaftenList()) {
				if (eigenschaft.getTitel().equals(inEigenschaft) && eigenschaft.getWert().equals(inWert))
					myVorlage.getEigenschaften().remove(eigenschaft);
			}
		}
	}

	public void WerkstueckEigenschaftDoppelteLoeschen(Prozess myProzess) {
		for (Werkstueck myWerkstueck : myProzess.getWerkstueckeList()) {
			List<String> einzelstuecke = new ArrayList<String>();
			for (Werkstueckeigenschaft eigenschaft : myWerkstueck.getEigenschaftenList()) {
				/* prüfen, ob die Eigenschaft doppelt, wenn ja, löschen */
				if (einzelstuecke.contains(eigenschaft.getTitel() + "|" + eigenschaft.getWert()))
					myWerkstueck.getEigenschaften().remove(eigenschaft);
				else
					einzelstuecke.add(eigenschaft.getTitel() + "|" + eigenschaft.getWert());
			}
		}
	}

	public void ScanvorlageEigenschaftDoppelteLoeschen(Prozess myProzess) {
		for (Vorlage myVorlage : myProzess.getVorlagenList()) {
			List<String> einzelstuecke = new ArrayList<String>();
			for (Vorlageeigenschaft eigenschaft : myVorlage.getEigenschaftenList()) {
				/* prüfen, ob die Eigenschaft doppelt, wenn ja, löschen */
				if (einzelstuecke.contains(eigenschaft.getTitel() + "|" + eigenschaft.getWert()))
					myVorlage.getEigenschaften().remove(eigenschaft);
				else
					einzelstuecke.add(eigenschaft.getTitel() + "|" + eigenschaft.getWert());
			}
		}
	}
}
