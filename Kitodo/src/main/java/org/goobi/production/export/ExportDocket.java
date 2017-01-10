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

package org.goobi.production.export;

import de.sub.goobi.beans.Prozess;
import de.sub.goobi.helper.exceptions.ExportFileException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import org.goobi.production.IProcessDataExport;

/**
 * This class provides generating a run note based on the generated xml log
 *
 * @author Steffen Hankiewicz
 */
public class ExportDocket implements IProcessDataExport {

	/**
	 * This method exports the production metadata as run note to a given stream. the docket.xsl has to be in the
	 * config-folder
	 *
	 * @param process the process to export
	 * @param os the OutputStream to write the contents to
	 * @throws IOException add description
	 * @throws ExportFileException add description
	 */
	@Override
	public void startExport(Prozess process, OutputStream os, String xsltfile) throws IOException {

		ExportXmlLog exl = new ExportXmlLog();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		exl.startExport(process, out, null);

		// generate pdf file
		StreamSource source = new StreamSource(new ByteArrayInputStream(out.toByteArray()));
		StreamSource transformSource = new StreamSource(xsltfile);
		FopFactory fopFactory = FopFactory.newInstance();
		FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		// transform xml
		Transformer xslfoTransformer = getTransformer(transformSource);
		try {
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outStream);
			Result res = new SAXResult(fop.getDefaultHandler());
			xslfoTransformer.transform(source, res);
		} catch (FOPException e) {
			throw new IOException("FOPException occurred", e);
		} catch (TransformerException e) {
			throw new IOException("TransformerException occurred", e);
		}

		// write the content to output stream
		byte[] pdfBytes = outStream.toByteArray();
		os.write(pdfBytes);
	}

	/**
	 * @param processList add description
	 * @param os add description
	 * @param xsltfile add description
	 * @throws IOException add description
	 */
	public void startExport(Iterable<Prozess> processList, OutputStream os, String xsltfile) throws IOException {

		ExportXmlLog exl = new ExportXmlLog();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		exl.startExport(processList, out, null);

		// generate pdf file
		StreamSource source = new StreamSource(new ByteArrayInputStream(out.toByteArray()));
		StreamSource transformSource = new StreamSource(xsltfile);
		FopFactory fopFactory = FopFactory.newInstance();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		// transform xml
		try {
			Transformer xslfoTransformer = TransformerFactory.newInstance().newTransformer(transformSource);
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, outStream);
			Result res = new SAXResult(fop.getDefaultHandler());
			xslfoTransformer.transform(source, res);
		} catch (FOPException e) {
			throw new IOException("FOPException occurred", e);
		} catch (TransformerException e) {
			throw new IOException("TransformerException occurred", e);
		}

		// write the content to output stream
		byte[] pdfBytes = outStream.toByteArray();
		os.write(pdfBytes);
	}

	private static Transformer getTransformer(StreamSource streamSource) {
		// setup the xslt transformer
		net.sf.saxon.TransformerFactoryImpl impl = new net.sf.saxon.TransformerFactoryImpl();
		try {
			return impl.newTransformer(streamSource);
		} catch (TransformerConfigurationException e) {
		}
		return null;
	}

}
