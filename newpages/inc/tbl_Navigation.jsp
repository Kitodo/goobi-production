<%@ page session="false" contentType="text/html;charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://jsftutorials.net/htmLib" prefix="htm"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="x"%>

<htm:td valign="top" height="100%" styleClass="layoutNavigation">

	<htm:table width="100%" style="height:100%" align="center" border="0"
		cellpadding="3" cellspacing="0">
		<htm:tr>
			<htm:td valign="top">
				<h:form id="naviform">
					<%-- ######################################## 
					
					Benutzerberechtigungen sind wie folgt:
					
					1: Administration - darf alles
					
					2: Prozessverwaltung - darf sehr viel (aber keine Benutzerverwaltung, kein Benutzerwechsel und auch kein Administrationsform)
					
					3: Benutzer und Prozesse - anscheinend nix anderes als 4
					
					4: nur Benutzer: aktuelle Schritte sehen
									
				#########################################--%>

					<%-- Startseite --%>
					<h:commandLink styleClass="mlink" action="newMain" id="main"
						style="#{NavigationForm.aktuell == '0' ? 'font-weight: bold;':'font-weight:normal ;'}">
						<h:panelGroup rendered="#{NavigationForm.aktuell == '0'}">
							<f:verbatim>&#8250; </f:verbatim>
						</h:panelGroup>
						<h:outputText value="#{msgs.startseite}" />
						<x:updateActionListener property="#{NavigationForm.aktuell}"
							value="0" />
					</h:commandLink>

					<%-- ################            allgemeines             ######################--%>

					<h:outputText styleClass="th_menu" value="- #{msgs.allgemeines} -" />
					<%-- Bedienungshinweise 
					<h:commandLink styleClass="mlink" action="Bedienung"
						style="#{NavigationForm.aktuell == '10' ? 'font-weight: bold;':'font-weight:normal ;'}">
						<h:panelGroup rendered="#{NavigationForm.aktuell == '10'}">
							<f:verbatim>&#8250; </f:verbatim>
						</h:panelGroup>
						<h:outputText value="#{msgs.bedienungshinweise}" />
						<x:updateActionListener property="#{NavigationForm.aktuell}"
							value="10" />
					</h:commandLink>--%>

					<%-- technischer Hintergrund --%>
					<h:commandLink styleClass="mlink" action="TechnischerHintergrund" id="technicalBackground"
						style="#{NavigationForm.aktuell == '11' ? 'font-weight: bold;':'font-weight:normal ;'}">
						<h:panelGroup rendered="#{NavigationForm.aktuell == '11'}">
							<f:verbatim>&#8250; </f:verbatim>
						</h:panelGroup>
						<h:outputText value="#{msgs.technischerHintergrund}" />
						<x:updateActionListener property="#{NavigationForm.aktuell}"
							value="11" />
					</h:commandLink>

					<%-- aktive Benutzer --%>
					<h:commandLink styleClass="mlink" action="aktiveBenutzerNeu" id="currentUsers"
						style="#{NavigationForm.aktuell == '12' ? 'font-weight: bold;':'font-weight:normal ;'}">
						<h:panelGroup rendered="#{NavigationForm.aktuell == '12'}">
							<f:verbatim>&#8250; </f:verbatim>
						</h:panelGroup>
						<h:outputText value="#{msgs.aktiveBenutzer}" />
						<x:updateActionListener property="#{NavigationForm.aktuell}"
							value="12" />
					</h:commandLink>

					<%-- ################            Workflow              ######################--%>

					<h:panelGroup rendered="#{LoginForm.maximaleBerechtigung > 0}">
						<h:outputText styleClass="th_menu" value="- #{msgs.workflow} -" />

						<%-- aktuelle Schritte --%>
						<h:commandLink styleClass="mlink" id="myTasks"
							action="#{AktuelleSchritteForm.FilterAlleStart}"
							style="#{NavigationForm.aktuell == '20' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '20'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.aktuelleSchritte}" />
							<x:updateActionListener property="#{AktuelleSchritteForm.filter}"
								value="" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="20" />
						</h:commandLink>

						<%-- Prozess suchen --%>
						<h:commandLink styleClass="mlink" action="ProzessverwaltungSuche" id="searchProcesses"
							style="#{NavigationForm.aktuell == '21' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '21'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.nachEinemBandSuchen}" />
							<x:updateActionListener
								property="#{ProzessverwaltungForm.filter}" value="" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="21" />
						</h:commandLink>

						<%-- Prozessübersicht --%>
						<h:commandLink styleClass="mlink" id="allProcesses"
							rendered="#{(LoginForm.maximaleBerechtigung == 1) || (LoginForm.maximaleBerechtigung == 2)}"
							action="#{ProzessverwaltungForm.FilterAktuelleProzesse}"
							style="#{NavigationForm.aktuell == '22' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '22'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.aktuelleProzesse}" />
							<x:updateActionListener
								property="#{ProzessverwaltungForm.filter}" value="" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="22" />
						</h:commandLink>

						<%-- neuen Vorgang anlegen --%>
						<h:commandLink styleClass="mlink" id="newProcess"
							rendered="#{(LoginForm.maximaleBerechtigung == 1) || (LoginForm.maximaleBerechtigung == 2)}"
							action="#{ProzessverwaltungForm.NeuenVorgangAnlegen}"
							style="#{NavigationForm.aktuell == '23' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '23'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.neuenVorgangAnlegen}" />
							<x:updateActionListener
								property="#{ProzessverwaltungForm.filter}" value="" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="23" />
						</h:commandLink>

						<%-- Prozessvorlagen --%>
						<h:commandLink styleClass="mlink" id="templates"
							rendered="#{LoginForm.maximaleBerechtigung == 1}"
							action="#{ProzessverwaltungForm.FilterVorlagen}"
							style="#{NavigationForm.aktuell == '24' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '24'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.prozessvorlagen}" />
							<x:updateActionListener
								property="#{ProzessverwaltungForm.filter}" value="" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="24" />
						</h:commandLink>

					</h:panelGroup>

					<%-- ################            Administration              ######################--%>

					<h:panelGroup rendered="#{LoginForm.maximaleBerechtigung == 1}">
						<h:outputText styleClass="th_menu"
							value="- #{msgs.administration} -" />

						<%-- Benutzerverwaltung --%>
						<h:commandLink styleClass="mlink" id="users"
							action="#{BenutzerverwaltungForm.FilterKein}"
							style="#{NavigationForm.aktuell == '30' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '30'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.benutzer}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="30" />
						</h:commandLink>

						<%-- Benutzergruppen --%>
						<h:commandLink styleClass="mlink" id="groups"
							action="#{BenutzergruppenForm.FilterKein}"
							style="#{NavigationForm.aktuell == '31' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '31'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.benutzergruppen}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="31" />
						</h:commandLink>

						<%-- Projekte --%>
						<h:commandLink styleClass="mlink" id="projects"
							action="#{ProjekteForm.FilterKein}"
							style="#{NavigationForm.aktuell == '32' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '32'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.projekte}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="32" />
						</h:commandLink>

						<%-- Regelsätze --%>
						<h:commandLink styleClass="mlink" id="rulesets"
							action="#{RegelsaetzeForm.FilterKein}"
							style="#{NavigationForm.aktuell == '33' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '33'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.regelsaetze}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="33" />
						</h:commandLink>

						<%-- Ldapgruppen --%>
						<h:commandLink styleClass="mlink" id="ldapgroups"
							action="#{LdapGruppenForm.FilterKein}"
							style="#{NavigationForm.aktuell == '34' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '34'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.ldapgruppen}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="34" />
						</h:commandLink>

                        <%-- aktive Module --%>
                        <h:commandLink styleClass="mlink" action="modulemanager" rendered="#{NavigationForm.showModuleManager}" 
                            style="#{NavigationForm.aktuell == '35' ? 'font-weight: bold;':'font-weight:normal ;'}"  id="modules">
                            <h:panelGroup rendered="#{NavigationForm.aktuell == '35'}">
                                <f:verbatim>&#8250; </f:verbatim>
                            </h:panelGroup>
                            <h:outputText value="#{msgs.modulemanager}" />
                            <x:updateActionListener property="#{NavigationForm.aktuell}"
                                value="35" />
                        </h:commandLink>

                        <%-- aktive Tasks --%>
                        <h:commandLink styleClass="mlink" action="taskmanager" rendered="#{NavigationForm.showTaskManager}" 
                            style="#{NavigationForm.aktuell == '36' ? 'font-weight: bold;':'font-weight:normal ;'}"  id="taskmanager">
                            <h:panelGroup rendered="#{NavigationForm.aktuell == '36'}">
                                <f:verbatim>&#8250; </f:verbatim>
                            </h:panelGroup>
                            <h:outputText value="#{msgs.taskmanager}" />
                            <x:updateActionListener property="#{NavigationForm.aktuell}"
                                value="36" />
                        </h:commandLink>

						<%-- Administrationsaufgaben --%>
						<h:commandLink styleClass="mlink" action="Administrationsaufgaben" id="admin"
							style="#{NavigationForm.aktuell == '37' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '37'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.scripte}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="37" />
						</h:commandLink>

					</h:panelGroup>

					<%-- ################            Benutzereinstellungen              ######################--%>

					<h:panelGroup rendered="#{LoginForm.maximaleBerechtigung > 0}">
						<h:outputText styleClass="th_menu"
							value="- #{msgs.benutzerdaten} -" />

						<%-- Benutzerkonfiguration --%>
						<h:commandLink styleClass="mlink" action="Benutzerkonfiguration"  id="userconfig"
							style="#{NavigationForm.aktuell == '40' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '40'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.benutzerkonfiguration}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="40" />
						</h:commandLink>

						<%-- Passwort ändern --%>
						<h:commandLink styleClass="mlink" action="newPasswortAendern" id="changePW"
							style="#{NavigationForm.aktuell == '41' ? 'font-weight: bold;':'font-weight:normal ;'}">
							<h:panelGroup rendered="#{NavigationForm.aktuell == '41'}">
								<f:verbatim>&#8250; </f:verbatim>
							</h:panelGroup>
							<h:outputText value="#{msgs.passwortAendern}" />
							<x:updateActionListener property="#{NavigationForm.aktuell}"
								value="41" />
						</h:commandLink>
					</h:panelGroup>

				</h:form>
			</htm:td>
		</htm:tr>
		<htm:tr valign="bottom">
			<htm:td height="5%" valign="bottom">
				<h:form id="loginform">
					<%@include file="Login.jsp"%>
				</h:form>
			</htm:td>
		</htm:tr>
	</htm:table>

</htm:td>
