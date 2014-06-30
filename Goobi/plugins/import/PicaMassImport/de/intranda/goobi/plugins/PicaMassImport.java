package de.intranda.goobi.plugins;

/**
 * Copyright by intranda GmbH 2013. All rights reserved.
 * 
 * Visit the websites for more information. 
 * 			- http://www.intranda.com
 * 			- http://digiverso.com 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.properties.ImportProperty;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;
import de.intranda.goobi.plugins.sru.SRUHelper;
import de.sub.goobi.beans.Prozesseigenschaft;
import de.sub.goobi.beans.Vorlageeigenschaft;
import de.sub.goobi.beans.Werkstueckeigenschaft;
import de.sub.goobi.helper.UghUtils;
import de.sub.goobi.helper.exceptions.ImportPluginException;

@PluginImplementation
public class PicaMassImport implements IImportPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(PicaMassImport.class);

	private static final String NAME = "intranda Pica Massenimport";
	private String data = "";
	private String importFolder = "";
	private File importFile;
	private Prefs prefs;
	private String currentIdentifier;
	private List<String> currentCollectionList;
	private String opacCatalogue;
	private String configDir;
	private static final String PPN_PATTERN = "\\d+X?";

	protected String ats;
	protected List<Prozesseigenschaft> processProperties = new ArrayList<Prozesseigenschaft>();
	protected List<Werkstueckeigenschaft> workProperties = new ArrayList<Werkstueckeigenschaft>();
	protected List<Vorlageeigenschaft> templateProperties = new ArrayList<Vorlageeigenschaft>();

	protected String currentTitle;
	protected String docType;
	protected String author = "";
	protected String volumeNumber = "";

	public String getId() {
		return NAME;
	}

	@Override
	public PluginType getType() {
		return PluginType.Import;
	}

	@Override
	public String getTitle() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return NAME;
	}

	@Override
	public void setPrefs(Prefs prefs) {
		this.prefs = prefs;
	}

	@Override
	public void setData(Record r) {
		this.data = r.getData();
	}

	@Override
	public Fileformat convertData() throws ImportPluginException {

		currentIdentifier = data;

		logger.debug("retrieving pica record for " + currentIdentifier  + " with server address: " + this.getOpacAddress());
		String search = SRUHelper.search(currentIdentifier, this.getOpacAddress());
		logger.trace(search);
		try {
			Node pica = SRUHelper.parseResult(search);
			if (pica == null) {
				logger.error("pica record for " + currentIdentifier + " does not exist in catalogue");
				throw new ImportPluginException("pica record for " + currentIdentifier + " does not exist in catalogue");

			}
			Fileformat ff = SRUHelper.parsePicaFormat(pica, prefs);
			if (ff != null) {
				DigitalDocument dd = ff.getDigitalDocument();
				boolean multivolue = false;
				DocStruct logicalDS = dd.getLogicalDocStruct();
				DocStruct child = null;
				if (logicalDS.getType().isAnchor()) {
					child = logicalDS.getAllChildren().get(0);
					multivolue = true;
				}
				// reading title
				MetadataType titleType = prefs.getMetadataTypeByName("TitleDocMain");
				List<? extends Metadata> mdList = logicalDS.getAllMetadataByType(titleType);
				if (mdList != null && mdList.size() > 0) {
					Metadata title = mdList.get(0);
					currentTitle = title.getValue();

				}
				// reading identifier
				MetadataType identifierType = prefs.getMetadataTypeByName("CatalogIDDigital");
				mdList = logicalDS.getAllMetadataByType(identifierType);
				if (mdList != null && mdList.size() > 0) {
					Metadata identifier = mdList.get(0);
					currentIdentifier = identifier.getValue();
				} else {
					currentIdentifier = String.valueOf(System.currentTimeMillis());
				}

				// reading author

				MetadataType authorType = prefs.getMetadataTypeByName("Author");
				List<Person> personList = logicalDS.getAllPersonsByType(authorType);
				if (personList != null && personList.size() > 0) {
					Person authorMetadata = personList.get(0);
					author = authorMetadata.getDisplayname();

				}

				// reading volume number
				if (child != null) {
					MetadataType mdt = prefs.getMetadataTypeByName("CurrentNoSorting");
					mdList = child.getAllMetadataByType(mdt);
					if (mdList != null && mdList.size() > 0) {
						Metadata md = mdList.get(0);
						volumeNumber = md.getValue();
					} else {
						mdt = prefs.getMetadataTypeByName("DateIssuedSort");
						mdList = child.getAllMetadataByType(mdt);
						if (mdList != null && mdList.size() > 0) {
							Metadata md = mdList.get(0);
							volumeNumber = md.getValue();
						}
					}
				}

				// reading ats
				MetadataType atsType = prefs.getMetadataTypeByName("TSL_ATS");
				mdList = logicalDS.getAllMetadataByType(atsType);
				if (mdList != null && mdList.size() > 0) {
					Metadata atstsl = mdList.get(0);
					ats = atstsl.getValue();
				} else {
					// generating ats
					ats = createAtstsl(currentTitle, author);
					Metadata atstsl = new Metadata(atsType);
					atstsl.setValue(ats);
					logicalDS.addMetadata(atstsl);
				}

				{
				    Vorlageeigenschaft prop = new Vorlageeigenschaft();
					prop.setTitel("Titel");
					prop.setWert(currentTitle);
					templateProperties.add(prop);
				}
				{
					if (StringUtils.isNotBlank(volumeNumber) && multivolue) {
					    Vorlageeigenschaft prop = new Vorlageeigenschaft();
						prop.setTitel("Bandnummer");
						prop.setWert(volumeNumber);
						templateProperties.add(prop);
					}
				}
				{
					MetadataType identifierAnalogType = prefs.getMetadataTypeByName("CatalogIDSource");
					mdList = logicalDS.getAllMetadataByType(identifierAnalogType);
					if (mdList != null && mdList.size() > 0) {
						String analog = mdList.get(0).getValue();

						Vorlageeigenschaft prop = new Vorlageeigenschaft();
						prop.setTitel("Identifier");
						prop.setWert(analog);
						templateProperties.add(prop);

					}
				}

				{
					if (child != null) {
						mdList = child.getAllMetadataByType(identifierType);
						if (mdList != null && mdList.size() > 0) {
							Metadata identifier = mdList.get(0);
							Werkstueckeigenschaft prop = new Werkstueckeigenschaft();
							prop.setTitel("Identifier Band");
							prop.setWert(identifier.getValue());
							workProperties.add(prop);
						}

					}
				}
				{
				    Werkstueckeigenschaft prop = new Werkstueckeigenschaft();
					prop.setTitel("Artist");
					prop.setWert(author);
					workProperties.add(prop);
				}
				{
				    Werkstueckeigenschaft prop = new Werkstueckeigenschaft();
					prop.setTitel("ATS");
					prop.setWert(ats);
					workProperties.add(prop);
				}
				{
				    Werkstueckeigenschaft prop = new Werkstueckeigenschaft();
					prop.setTitel("Identifier");
					prop.setWert(currentIdentifier);
					workProperties.add(prop);
				}

				try {
					// pathimagefiles
					MetadataType mdt = prefs.getMetadataTypeByName("pathimagefiles");
					Metadata newmd = new Metadata(mdt);
					newmd.setValue("/images/");
					dd.getPhysicalDocStruct().addMetadata(newmd);

					// collections
					if (this.currentCollectionList != null) {
						MetadataType mdTypeCollection = this.prefs.getMetadataTypeByName("singleDigCollection");
						for (String collection : this.currentCollectionList) {
							Metadata mdCollection = new Metadata(mdTypeCollection);
							mdCollection.setValue(collection);
							dd.getLogicalDocStruct().addMetadata(mdCollection);
						}
					}

				} catch (MetadataTypeNotAllowedException e) {
					logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
				}

				return ff;
			} else {
				logger.error("pica record for " + currentIdentifier + " is empty");
				throw new ImportPluginException("pica record for " + currentIdentifier + " is empty");
			}
		} catch (ReadException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (PreferencesException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (TypeNotAllowedForParentException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (IOException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (JDOMException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (ParserConfigurationException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (MetadataTypeNotAllowedException e) {
			logger.error(this.currentIdentifier + ": " + e.getMessage(), e);
			throw new ImportPluginException(e);
		}

	}

	@Override
	public String getImportFolder() {
		return this.importFolder;
	}

	@Override
	public String getProcessTitle() {
		String answer = "";
		if (StringUtils.isNotBlank(this.ats)) {
			answer = ats.toLowerCase() + "_" + this.currentIdentifier;
		} else {
			answer = this.currentIdentifier;
		}
		if (StringUtils.isNotBlank(volumeNumber)) {
			answer = answer + "_" + volumeNumber;
		}
		return answer;
	}

	@Override
	public List<ImportObject> generateFiles(List<Record> records){
		List<ImportObject> answer = new ArrayList<ImportObject>();

		for (Record r : records) {
			this.data = r.getData();
			this.currentCollectionList = r.getCollections();
			ImportObject io = new ImportObject();
			Fileformat ff = null;
			try {
				ff = convertData();
			} catch (ImportPluginException e1) {
				io.setErrorMessage(e1.getMessage());
			}
			io.setProcessTitle(getProcessTitle());
			if (ff != null) {
				r.setId(this.currentIdentifier);
				try {
					MetsMods mm = new MetsMods(this.prefs);
					mm.setDigitalDocument(ff.getDigitalDocument());
					String fileName = getImportFolder() + getProcessTitle() + ".xml";
					logger.debug("Writing '" + fileName + "' into given folder...");
					mm.write(fileName);
					io.setMetsFilename(fileName);
					io.setImportReturnValue(ImportReturnValue.ExportFinished);

				} catch (PreferencesException e) {
					logger.error(currentIdentifier + ": " + e.getMessage(), e);
					io.setImportReturnValue(ImportReturnValue.InvalidData);
				} catch (WriteException e) {
					logger.error(currentIdentifier + ": " + e.getMessage(), e);
					io.setImportReturnValue(ImportReturnValue.WriteError);
				}
			} else {
				io.setImportReturnValue(ImportReturnValue.InvalidData);
			}
			answer.add(io);
		}

		return answer;
	}

	@Override
	public void setImportFolder(String folder) {
		this.importFolder = folder;
	}

	@Override
	public List<Record> splitRecords(String records) {
		return new ArrayList<Record>();
	}

	@Override
	public List<Record> generateRecordsFromFile() {
		List<Record> records = new ArrayList<Record>();

		try {
			InputStream myxls = new FileInputStream(importFile);
			if (importFile.getName().endsWith(".xlsx")) {
				XSSFWorkbook wb = new XSSFWorkbook(myxls);
				XSSFSheet sheet = wb.getSheetAt(0); // first sheet
				// loop over all rows
				for (int j = 0; j <= sheet.getLastRowNum(); j++) {
					// loop over all cells
					XSSFRow row = sheet.getRow(j);

					if (row != null) {
						for (int i = 0; i < row.getLastCellNum(); i++) {
							XSSFCell cell = row.getCell(i);
							// changing all cell types to String
							cell.setCellType(HSSFCell.CELL_TYPE_STRING);
							if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
								int value = (int) cell.getNumericCellValue();
								Record r = new Record();
								r.setId(String.valueOf(value));
								r.setData(String.valueOf(value));
								records.add(r);
								// logger.debug("found content " + value + " in row " + j + " cell " + i);

							} else if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
								String value = cell.getStringCellValue();
								if (value.trim().matches(PPN_PATTERN)) {
									// remove date and time from list
									if (value.length() > 6) {
										logger.debug("matched: " + value + " in row " + (j + 1) + " cell " + i);
										// found numbers and character 'X' as last sign
										Record r = new Record();
										r.setId(value.trim());
										r.setData(value.trim());
										records.add(r);
									}
								}
							}
						}
					}
				}
			} else {
				HSSFWorkbook wb = new HSSFWorkbook(myxls);
				HSSFSheet sheet = wb.getSheetAt(0); // first sheet
				// loop over all rows
				for (int j = 0; j <= sheet.getLastRowNum(); j++) {
					// loop over all cells
					HSSFRow row = sheet.getRow(j);

					if (row != null) {
						for (int i = 0; i < row.getLastCellNum(); i++) {
							HSSFCell cell = row.getCell(i);
							// changing all cell types to String
							if (cell != null) {
								cell.setCellType(HSSFCell.CELL_TYPE_STRING);
								if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
									int value = (int) cell.getNumericCellValue();
									Record r = new Record();
									r.setId(String.valueOf(value));
									r.setData(String.valueOf(value));
									records.add(r);
									// logger.debug("found content " + value + " in row " + j + " cell " + i);

								} else if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
									String value = cell.getStringCellValue();
									if (value.trim().matches(PPN_PATTERN)) {
										// remove date and time from list
										if (value.length() > 6) {
											logger.debug("matched: " + value + " in row " + (j + 1) + " cell " + i);
											// found numbers and character 'X' as last sign
											Record r = new Record();
											r.setId(value.trim());
											r.setData(value.trim());
											records.add(r);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}

		//
		// Workbook w = Workbook.getWorkbook(importFile);
		// // Get the first sheet
		// Sheet sheet = w.getSheet(0);
		// // loop over all rows in first column
		// for (int i = 0; i < sheet.getRows(); i++) {
		// Cell cell = sheet.getCell(0, i);
		// // get content
		// String value = cell.getContents();
		// // test if content is a valid PPN
		// if (cell.getType().equals(CellType.NUMBER)) {
		// // found numbers only
		// Record r = new Record();
		// r.setId(value.trim());
		// r.setData(value.trim());
		// records.add(r);
		// } else if (cell.getType().equals(CellType.LABEL)) {
		// // found letters in it
		// if (value != null && value.trim().matches(PPN_PATTERN)) {
		// logger.debug("matched: " + value);
		// // found numbers and character 'X' as last sign
		// Record r = new Record();
		// r.setId(value.trim());
		// r.setData(value.trim());
		// records.add(r);
		// }
		// }
		// }
		// } catch (BiffException e) {
		// logger.error(e);
		// } catch (IOException e) {
		// logger.error(e);
		// }
		return records;
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		PicaMassImport pmi = new PicaMassImport();
		pmi.setFile(new File("/home/robert/workspace-git/PicaMassImportPlugins/example/ppn_Beispiele.xls"));
		List<Record> answer = pmi.generateRecordsFromFile();
		for (Record r : answer) {
			System.out.println(r.getData());
		}
	}

	@Override
	public List<Record> generateRecordsFromFilenames(List<String> filenames) {
		return new ArrayList<Record>();
	}

	@Override
	public void setFile(File importFile) {
		this.importFile = importFile;
	}

	@Override
	public List<String> splitIds(String ids) {
		return new ArrayList<String>();
	}

	@Override
	public List<ImportType> getImportTypes() {
		List<ImportType> answer = new ArrayList<ImportType>();
		answer.add(ImportType.FILE);
		return answer;
	}

	@Override
	public List<ImportProperty> getProperties() {
		return new ArrayList<ImportProperty>();
	}

	@Override
	public List<String> getAllFilenames() {
		return new ArrayList<String>();
	}

	@Override
	public void deleteFiles(List<String> selectedFilenames) {
	}

	@Override
	public List<DocstructElement> getCurrentDocStructs() {
		return null;
	}

	@Override
	public String deleteDocstruct() {
		return null;
	}

	@Override
	public String addDocstruct() {
		return null;
	}

	@Override
	public List<String> getPossibleDocstructs() {
		return null;
	}

	@Override
	public DocstructElement getDocstruct() {
		return null;
	}

	@Override
	public void setDocstruct(DocstructElement dse) {
	}

	private String createAtstsl(String myTitle, String autor) {
		String myAtsTsl = "";
		if (autor != null && !autor.equals("")) {
			/* autor */
			if (autor.length() > 4) {
				myAtsTsl = autor.substring(0, 4);
			} else {
				myAtsTsl = autor;
				/* titel */
			}

			if (myTitle.length() > 4) {
				myAtsTsl += myTitle.substring(0, 4);
			} else {
				myAtsTsl += myTitle;
			}
		}

		/*
		 * -------------------------------- bei Zeitschriften Tsl berechnen --------------------------------
		 */
		// if (gattung.startsWith("ab") || gattung.startsWith("ob")) {
		if (autor == null || autor.equals("")) {
			myAtsTsl = "";
			StringTokenizer tokenizer = new StringTokenizer(myTitle);
			int counter = 1;
			while (tokenizer.hasMoreTokens()) {
				String tok = tokenizer.nextToken();
				if (counter == 1) {
					if (tok.length() > 4) {
						myAtsTsl += tok.substring(0, 4);
					} else {
						myAtsTsl += tok;
					}
				}
				if (counter == 2 || counter == 3) {
					if (tok.length() > 2) {
						myAtsTsl += tok.substring(0, 2);
					} else {
						myAtsTsl += tok;
					}
				}
				if (counter == 4) {
					if (tok.length() > 1) {
						myAtsTsl += tok.substring(0, 1);
					} else {
						myAtsTsl += tok;
					}
				}
				counter++;
			}
		}
		/* im ATS-TSL die Umlaute ersetzen */
		if (FacesContext.getCurrentInstance() != null) {
			new UghUtils();
			myAtsTsl = UghUtils.convertUmlaut(myAtsTsl);
		}
		myAtsTsl = myAtsTsl.replaceAll("[\\W]", "");
		return myAtsTsl;
	}
	
	/**
	 * @param the opac catalogue
	 */	
	@Override
	public void setOpacCatalogue(String opacCatalogue) {
		this.opacCatalogue = opacCatalogue ;
	}
	
	/**
	* @return the opac catalogue
	*/	
	private String getOpacCatalogue() {
		return this.opacCatalogue;
	}
	
	/**
	* @param the goobi config directory
	*/	
	@Override
	public void setGoobiConfigDirectory(String configDir) {
		this.configDir = configDir ;
	}
	
	/**
	* @return the goobi config directory
	*/	
	private String getGoobiConfigDirectory() {
		return configDir ;
	}	
	
	/**
	* @return the address of the opac catalogue
	* @throws ImportPluginException 
	*/	
	private String getOpacAddress() throws ImportPluginException {
		
		String address = "";
		
		try {
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document xmlDocument = builder.parse(new FileInputStream(FilenameUtils.concat(this.getGoobiConfigDirectory(), "goobi_opac.xml")));
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			
			Node node = (Node) xPath.compile("/opacCatalogues/catalogue[@title='" + this.getOpacCatalogue() + "']/config").evaluate(xmlDocument, XPathConstants.NODE);	

			address = node.getAttributes().getNamedItem("address").getNodeValue();

		} catch (ParserConfigurationException e) {
			logger.error(e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (IOException e) {
			logger.error( e.getMessage(), e);
			throw new ImportPluginException(e);
		} catch (XPathExpressionException e) {
			logger.error(e.getMessage(), e);
			throw new ImportPluginException(e);
		}
		
		return address;
	}	
}