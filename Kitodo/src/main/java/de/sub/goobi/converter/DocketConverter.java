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

package de.sub.goobi.converter;

import de.sub.goobi.beans.Docket;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.DocketDAO;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.log4j.Logger;

public class DocketConverter implements Converter {
	public static final String CONVERTER_ID = "DocketConverter";
	private static final Logger logger = Logger.getLogger(DocketConverter.class);

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		if (value == null || value.length() == 0) {
			return null;
		} else {
			try {
				return new DocketDAO().get(Integer.valueOf(value));
			} catch (NumberFormatException e) {
				logger.error(e);
				return "0";
			} catch (DAOException e) {
				logger.error(e);
				return "0";
			}
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		if (value == null) {
			return null;
		} else if (value instanceof Docket) {
			return String.valueOf(((Docket) value).getId().intValue());
		} else if (value instanceof String) {
			return (String) value;
		} else {
			throw new ConverterException("Falscher Typ: " + value.getClass() + " muss 'Docket' sein!");
		}
	}

}
