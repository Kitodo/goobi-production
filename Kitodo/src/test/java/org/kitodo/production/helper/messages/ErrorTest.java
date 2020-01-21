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

package org.kitodo.production.helper.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Enumeration;
import java.util.Locale;

import org.junit.Test;
import org.kitodo.FileLoader;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.file.FileService;

public class ErrorTest {

    private static final FileService fileService = ServiceManager.getFileService();
    private final Locale locale = new Locale("EN");
    private final String customBundle = "errors";
    private final String defaultBundle = "messages.errors";

    @Test
    public void shouldGetKeys() {
        Enumeration<String> keys = Message.getResourceBundle(defaultBundle, customBundle, locale).getKeys();

        boolean containsKey = false;
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.equals("error")) {
                containsKey = true;
                break;
            }
        }

        assertTrue("Keys set doesn't contain searched key!", containsKey);
    }

    @Test
    public void shouldGetStringFromDefaultBundle() {
        assertEquals("Error...", Message.getResourceBundle(defaultBundle, customBundle, locale).getString("error"));
    }

    @Test
    public void shouldGetStringFromCustomBundle() throws Exception {
        File messageDirectory = new File("src/test/resources/custom");

        if (messageDirectory.mkdir()) {
            FileLoader.createCustomErrors();

            String value = Message.getResourceBundle(defaultBundle, customBundle, locale).getString("error");
            assertEquals("Test custom error", value);

            FileLoader.deleteCustomErrors();
            messageDirectory.delete();
        } else {
            fail("Directory for custom messages was not created!");
        }
    }
}
