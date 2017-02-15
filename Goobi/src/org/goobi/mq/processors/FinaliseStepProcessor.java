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

package org.goobi.mq.processors;

import java.util.List;
import java.util.Map;

import org.goobi.mq.ActiveMQProcessor;
import org.goobi.mq.MapMessageObjectReader;
import org.goobi.production.properties.AccessCondition;
import org.goobi.production.properties.ProcessProperty;

import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.forms.AktuelleSchritteForm;
import de.sub.goobi.persistence.SchrittDAO;

/**
 * This is a web service interface to close steps. You have to provide the step
 * id as “id”; you can add a field “message” which will be added to the wiki
 * field.
 * 
 * @author Matthias Ronge <matthias.ronge@zeutschel.de>
 */
public class FinaliseStepProcessor extends ActiveMQProcessor {

	/**
	 * The default constructor looks up the queue name to use in
	 * kitodo_config.properties. If that is not configured and “null” is passed to
	 * the super constructor, this will prevent
	 * ActiveMQDirector.registerListeners() from starting this service.
	 */
	public FinaliseStepProcessor() {
		super(ConfigMain.getParameter("activeMQ.finaliseStep.queue", null));
	}

	/**
	 * This is the main routine processing incoming tickets. It gets an
	 * AktuelleSchritteForm object, sets it to the appropriate step which is
	 * retrieved from the database, appends the message − if any − to the wiki
	 * field, and executes the form’s the step close function.
	 * 
	 * @param ticket
	 *            the incoming message
	 * 
	 * @see org.goobi.mq.ActiveMQProcessor#process(org.goobi.mq.MapMessageObjectReader)
	 */
	@Override
	protected void process(MapMessageObjectReader ticket) throws Exception {
		AktuelleSchritteForm dialog = new AktuelleSchritteForm();
		Integer stepID = ticket.getMandatoryInteger("id");
		dialog.setMySchritt(new SchrittDAO().get(stepID));
		if (ticket.hasField("properties")) updateProperties(dialog, ticket.getMapOfStringToString("properties"));
		if (ticket.hasField("message"))
			dialog.getMySchritt().getProzess().addToWikiField(ticket.getString("message"));
		dialog.SchrittDurchBenutzerAbschliessen();
	}

	/**
	 * The method updateProperties() transfers the properties to set into
	 * Goobi’s data model.
	 * 
	 * @param dialog
	 *            The AktuelleSchritteForm that we work with
	 * @param propertiesToSet
	 *            A Map with the properties to set
	 */
	protected void updateProperties(AktuelleSchritteForm dialog, Map<String, String> propertiesToSet) {
		List<ProcessProperty> availableProperties = dialog.getProcessProperties();
		for (int position = 0; position < availableProperties.size(); position++) {
			ProcessProperty propertyAtPosition = availableProperties.get(position);
			String key = propertyAtPosition.getName();
			if (propertiesToSet.containsKey(key)) {
				String desiredValue = propertiesToSet.get(key);
				AccessCondition permissions = propertyAtPosition.getCurrentStepAccessCondition();
				if (AccessCondition.WRITE.equals(permissions) || AccessCondition.WRITEREQUIRED.equals(permissions)) {
					propertyAtPosition.setValue(desiredValue);
					if (dialog.getContainer() == null || dialog.getContainer() == 0) {
						dialog.setProcessProperty(propertyAtPosition);
					} else
						availableProperties.set(position, propertyAtPosition);
					dialog.saveCurrentProperty();
				}
			}
		}
	}
}
