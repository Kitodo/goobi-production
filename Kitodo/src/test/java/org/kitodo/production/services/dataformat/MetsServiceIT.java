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

package org.kitodo.production.services.dataformat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.kitodo.api.dataformat.ExistingOrLinkedStructure;
import org.kitodo.api.dataformat.LinkedStructure;
import org.kitodo.api.dataformat.MediaUnit;
import org.kitodo.api.dataformat.Workpiece;

public class MetsServiceIT {

    /**
     * Tests loading a workpiece from a METS file.
     */
    @Test
    public void testReadXML() throws Exception {
        Workpiece workpiece;
        try (InputStream in = new FileInputStream(new File("../Kitodo-DataFormat/src/test/resources/meta.xml"))) {
            workpiece = MetsService.getInstance().load(in, null);
        }

        // METS file has 183 associated images
        assertEquals(183, workpiece.getMediaUnits().size());

        // all pages are linked to the root element
        assertEquals(workpiece.getMediaUnits().size(), workpiece.getStructure().getViews().size());

        // root node has 16 children
        assertEquals(16, workpiece.getStructure().getChildren().size());

        // root node has 11 meta-data entries
        assertEquals(11, workpiece.getStructure().getMetadata().size());

        // file URIs can be read
        assertEquals(new URI("images/ThomPhar_644901748_media/00000001.tif"),
            workpiece.getMediaUnits().get(0).getMediaFiles().entrySet().iterator().next().getValue());

        // pagination can be read
        assertEquals(
            Arrays.asList("uncounted", "uncounted", "uncounted", "uncounted", "[I]", "[II]", "[III]", "[IV]", "V", "VI",
                "[VII]", "[VIII]", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
                "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32",
                "33", "34", "35", "36", "37", "38", "39", "[40]", "uncounted", "uncounted", "41", "42", "43", "44",
                "45", "46", "uncounted", "uncounted", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56",
                "[57]", "[58]", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "[72]",
                "73", "74", "[75]", "[76]", "[77]", "[78]", "79", "80", "uncounted", "uncounted", "uncounted",
                "uncounted", "81", "82", "uncounted", "uncounted", "83", "84", "uncounted", "uncounted", "85", "86",
                "[87]", "uncounted", "[88]", "uncounted", "89", "90", "91", "92", "uncounted", "[93]", "[94]",
                "uncounted", "95", "96", "uncounted", "uncounted", "97", "uncounted", "[98]", "uncounted", "99", "100",
                "uncounted", "uncounted", "uncounted", "uncounted", "101", "102", "103", "104", "105", "106",
                "uncounted", "uncounted", "107", "108", "109", "[110]", "111", "112", "uncounted", "uncounted",
                "uncounted", "uncounted", "113", "114", "115", "116", "117", "118", "uncounted", "uncounted", "119",
                "120", "uncounted", "uncounted", "121", "122", "123", "124", "125", "126", "127", "128", "129", "130",
                "131", "132", "133", "134", "uncounted", "uncounted", "uncounted"),
            workpiece.getMediaUnits().stream().map(MediaUnit::getOrderlabel).collect(Collectors.toList()));
    }

    @Test
    public void testReadingHierarchy() throws Exception {
        Workpiece workpiece = MetsService.getInstance()
                .load(new FileInputStream(new File("../Kitodo-DataFormat/src/test/resources/between.xml")),
                    (uri, couldHaveToBeWrittenInTheFuture) -> {
                        try {
                            return new FileInputStream(
                                    new File("../Kitodo-DataFormat/src/test/resources/" + uri.getPath()));
                        } catch (FileNotFoundException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

        ExistingOrLinkedStructure downlink = workpiece.getStructure().getChildren().get(0);
        assertTrue(downlink.isLinked());
        assertEquals("Leaf METS file", downlink.getLabel());
        assertEquals(BigInteger.valueOf(1), ((LinkedStructure) downlink).getOrder());
        assertEquals("leaf", downlink.getType());
        assertEquals("leaf.xml", ((LinkedStructure) downlink).getUri().getPath());

        List<LinkedStructure> uplinks = workpiece.getUplinks();
        assertEquals(2, uplinks.size());
        LinkedStructure top = uplinks.get(0);
        assertEquals("Top METS file", top.getLabel());
        assertEquals(null, top.getOrder());
        assertEquals("top", top.getType());
        assertEquals("top.xml", top.getUri().getPath());

        LinkedStructure second = uplinks.get(1);
        assertEquals("My branch", second.getLabel());
        assertEquals(BigInteger.valueOf(10), second.getOrder());
        assertEquals("two", second.getType());
        assertEquals("top.xml", second.getUri().getPath());
    }
}
