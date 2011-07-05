package de.sub.goobi.Beans;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;

import de.sub.goobi.Persistence.HibernateUtil;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.encryption.DesEncrypter;
import de.sub.goobi.helper.ldap.Ldap;

public class Benutzer implements Serializable {
	private static final long serialVersionUID = -7482853955996650586L;
	private Integer id;
	private String vorname;
	private String nachname;
	private String login;
	private String passwort;
	private boolean istAktiv = true;
	private String isVisible;
	private String standort;
	private Integer tabellengroesse = Integer.valueOf(10);
	private Integer sessiontimeout = 7200;
	private boolean confVorgangsdatumAnzeigen = false;
	private String metadatenSprache;
	private Set<Benutzergruppe> benutzergruppen;
	private Set<Schritt> schritte;
	private Set<Schritt> bearbeitungsschritte;
	private Set<Projekt> projekte;
	private boolean mitMassendownload = false;
	private LdapGruppe ldapGruppe;
	private String css;

	public Benutzer() {
		benutzergruppen = new HashSet<Benutzergruppe>();
		projekte = new HashSet<Projekt>();
		schritte = new HashSet<Schritt>();
	}

	/*=======================================================
	
	                   Getter und Setter
	                   
	========================================================*/

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getNachname() {
		return nachname;
	}

	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public String getPasswort() {
		return passwort;
	}

	public void setPasswort(String inpasswort) {
		this.passwort = inpasswort;
	}

	public String getPasswortCrypt() {
		DesEncrypter encrypter = new DesEncrypter();
		String decrypted = encrypter.decrypt(passwort);
		return decrypted;
	}

	public void setPasswortCrypt(String inpasswort) {
		DesEncrypter encrypter = new DesEncrypter();
		String encrypted = encrypter.encrypt(inpasswort);
		this.passwort = encrypted;
	}

	public boolean isIstAktiv() {
		return istAktiv;
	}

	public void setIstAktiv(boolean istAktiv) {
		this.istAktiv = istAktiv;
	}

	public void setIsVisible(String isVisible) {
		this.isVisible = isVisible;
	}

	public String getIsVisible() {
		return isVisible;
	}

	public String getStandort() {
		return standort;
	}

	public void setStandort(String instandort) {
		standort = instandort;
	}

	public String getVorname() {
		return vorname;
	}

	public void setVorname(String vorname) {
		this.vorname = vorname;
	}

	public Integer getTabellengroesse() {
		if (this.tabellengroesse == null) {
			return Integer.valueOf(10);
		} else if (this.tabellengroesse > 100) {
			return Integer.valueOf(100);
		}

		return tabellengroesse;
	}

	public void setTabellengroesse(Integer tabellengroesse) {
		if (tabellengroesse > 100) {
			tabellengroesse = Integer.valueOf(100);
		}
		this.tabellengroesse = tabellengroesse;
	}

	public boolean isMitMassendownload() {
		return mitMassendownload;
	}

	public void setMitMassendownload(boolean mitMassendownload) {
		this.mitMassendownload = mitMassendownload;
	}

	public LdapGruppe getLdapGruppe() {
		return ldapGruppe;
	}

	public void setLdapGruppe(LdapGruppe ldapGruppe) {
		this.ldapGruppe = ldapGruppe;
	}

	/*---------------------------------------------------------------------------------------------------------
	 Datum: 24.06.2005, 23:34:10
	 Zweck: Set für Benutzergruppen
	 ---------------------------------------------------------------------------------------------------------*/
	public Set<Benutzergruppe> getBenutzergruppen() {
		return benutzergruppen;
	}

	public void setBenutzergruppen(Set<Benutzergruppe> benutzergruppen) {
		this.benutzergruppen = benutzergruppen;
	}

	public int getBenutzergruppenSize() {
		if (benutzergruppen == null)
			return 0;
		else
			return benutzergruppen.size();
	}

	public List<Benutzergruppe> getBenutzergruppenList() {
		if (benutzergruppen == null)
			return new ArrayList<Benutzergruppe>();
		else
			return new ArrayList<Benutzergruppe>(benutzergruppen);
	}

	/*---------------------------------------------------------------------------------------------------------
	 Datum: 24.06.2005, 23:34:10
	 Zweck: Set für Schritte
	 ---------------------------------------------------------------------------------------------------------*/

	public Set<Schritt> getSchritte() {
		return schritte;
	}

	public void setSchritte(Set<Schritt> schritte) {
		this.schritte = schritte;
	}

	public int getSchritteSize() {
		if (schritte == null)
			return 0;
		else
			return schritte.size();
	}

	public List<Schritt> getSchritteList() {
		if (schritte == null)
			return new ArrayList<Schritt>();
		else
			return new ArrayList<Schritt>(schritte);
	}

	/*---------------------------------------------------------------------------------------------------------
	 Datum: 24.06.2005, 23:34:10
	 Zweck: Set für BearbeitungsSchritte
	 ---------------------------------------------------------------------------------------------------------*/
	public Set<Schritt> getBearbeitungsschritte() {
		return bearbeitungsschritte;
	}

	public void setBearbeitungsschritte(Set<Schritt> bearbeitungsschritte) {
		this.bearbeitungsschritte = bearbeitungsschritte;
	}

	public int getBearbeitungsschritteSize() {
		if (bearbeitungsschritte == null)
			return 0;
		else
			return bearbeitungsschritte.size();
	}

	public List<Schritt> getBearbeitungsschritteList() {
		if (bearbeitungsschritte == null)
			bearbeitungsschritte = new HashSet<Schritt>();
		return new ArrayList<Schritt>(bearbeitungsschritte);
	}

	/*---------------------------------------------------------------------------------------------------------
	 Datum: 24.02.2006, 23:34:10
	 Zweck: Set für Projekte
	 ---------------------------------------------------------------------------------------------------------*/

	public Set<Projekt> getProjekte() {
		return projekte;
	}

	public void setProjekte(Set<Projekt> projekte) {
		this.projekte = projekte;
	}

	public int getProjekteSize() {
		if (projekte == null)
			return 0;
		else
			return projekte.size();
	}

	public List<Projekt> getProjekteList() {
		if (projekte == null)
			return new ArrayList<Projekt>();
		else {
			//         Hibernate.initialize(projekte);
			//         System.out.println(projekte.size());
			//         for (Iterator iter = projekte.iterator(); iter.hasNext();) {
			//            Projekt p = (Projekt) iter.next();
			//            System.out.println(p.getTitel());
			//         }
			//         System.out.println("projekte durchlaufen");
			return new ArrayList<Projekt>(projekte);
		}
	}

	public boolean isConfVorgangsdatumAnzeigen() {
		return confVorgangsdatumAnzeigen;
	}

	public void setConfVorgangsdatumAnzeigen(boolean confVorgangsdatumAnzeigen) {
		this.confVorgangsdatumAnzeigen = confVorgangsdatumAnzeigen;
	}

	public String getMetadatenSprache() {
		return metadatenSprache;
	}

	public void setMetadatenSprache(String metadatenSprache) {
		this.metadatenSprache = metadatenSprache;
	}

	/*#####################################################
	 #####################################################
	 ##																															 
	 ##																Helper									
	 ##                                                   															    
	 #####################################################
	 ####################################################*/

	public boolean istPasswortKorrekt(String inPasswort) {
		if (inPasswort == null || inPasswort.length() == 0) {

			return false;
		} else {

			/* Verbindung zum LDAP-Server aufnehmen und Login prüfen, wenn LDAP genutzt wird */
		
			if (ConfigMain.getBooleanParameter("ldap_use")) {
				Ldap myldap = new Ldap();
				return myldap.isUserPasswordCorrect(this, inPasswort);
			} else {
				// return passwort.equals(inPasswort);
				DesEncrypter encrypter = new DesEncrypter();
				String encoded = encrypter.encrypt(inPasswort);
				return passwort.equals(encoded);
			}
		}
	}

	public String getNachVorname() {
		return nachname + ", " + vorname;
	}

	/**
	 * BenutzerHome ermitteln und zurückgeben (entweder aus dem LDAP oder direkt aus der Konfiguration)
	 * @return Path as String
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String getHomeDir() throws IOException, InterruptedException {
		String rueckgabe = "";
		/* wenn LDAP genutzt wird, HomeDir aus LDAP ermitteln, ansonsten aus der Konfiguration */
	
		if (ConfigMain.getBooleanParameter("ldap_use")) {
			Ldap myldap = new Ldap();
			rueckgabe = myldap.getUserHomeDirectory(this);
		} else {
			rueckgabe = ConfigMain.getParameter("dir_Users") + login;
		}

		if (rueckgabe.equals(""))
			return "";

		if (!rueckgabe.endsWith(File.separator))
			rueckgabe += File.separator;
		/* wenn das Verzeichnis nicht "" ist, aber noch nicht existiert, dann jetzt anlegen */
		File homePath = new File(rueckgabe);
		if (!homePath.exists())
			new Helper().createUserDirectory(rueckgabe, login);
		return rueckgabe;
	}

	public Integer getSessiontimeout() {
		if (sessiontimeout == null)
			sessiontimeout = 7200;
		return sessiontimeout;
	}

	public void setSessiontimeout(Integer sessiontimeout) {
		this.sessiontimeout = sessiontimeout;
	}

	public Integer getSessiontimeoutInMinutes() {
		return getSessiontimeout() / 60;
	}

	public void setSessiontimeoutInMinutes(Integer sessiontimeout) {
		if (sessiontimeout.intValue() < 5)
			this.sessiontimeout = 5 * 60;
		else
			this.sessiontimeout = sessiontimeout * 60;
	}

	public String getCss() {
		if (css == null || css.length() == 0)
			css = "/css/default.css";
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

}
