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
import de.sub.kitodo.helper.Helper;
import de.sub.kitodo.helper.exceptions.InvalidImagesException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;

import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.exceptions.SwapException;
import org.kitodo.services.ServiceManager;

import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.dl.RomanNumeral;
import ugh.exceptions.ContentFileNotLinkedException;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;

public class MetadatenImagesHelper {
    private static final Logger logger = Logger.getLogger(MetadatenImagesHelper.class);
    private final Prefs myPrefs;
    private final DigitalDocument mydocument;
    private int myLastImage = 0;
    private final ServiceManager serviceManager = new ServiceManager();

    public MetadatenImagesHelper(Prefs inPrefs, DigitalDocument inDocument) {
        this.myPrefs = inPrefs;
        this.mydocument = inDocument;
    }

    /**
     * Markus baut eine Seitenstruktur aus den vorhandenen Images --- Steps -
     * ---- Validation of images compare existing number images with existing
     * number of page DocStructs if it is the same don't do anything if
     * DocStructs are less add new pages to physicalDocStruct if images are less
     * delete pages from the end of pyhsicalDocStruct.
     */
    public void createPagination(Process process, String directory)
            throws TypeNotAllowedForParentException, IOException, InterruptedException, SwapException, DAOException {
        DocStruct physicaldocstruct = this.mydocument.getPhysicalDocStruct();

        DocStruct log = this.mydocument.getLogicalDocStruct();
        while (log.getType().getAnchorClass() != null && log.getAllChildren() != null
                && log.getAllChildren().size() > 0) {
            log = log.getAllChildren().get(0);
        }

        /*
         * der physische Baum wird nur angelegt, wenn er noch nicht existierte
         */
        if (physicaldocstruct == null) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName("BoundBook");
            physicaldocstruct = this.mydocument.createDocStruct(dst);

            /*
             * Probleme mit dem FilePath
             */
            MetadataType MDTypeForPath = this.myPrefs.getMetadataTypeByName("pathimagefiles");
            try {
                Metadata mdForPath = new Metadata(MDTypeForPath);
                if (SystemUtils.IS_OS_WINDOWS) {
                    mdForPath.setValue(
                            "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process));
                } else {
                    mdForPath.setValue(
                            "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process));
                }
                physicaldocstruct.addMetadata(mdForPath);
            } catch (MetadataTypeNotAllowedException e1) {
            } catch (DocStructHasNoTypeException e1) {
            }
            this.mydocument.setPhysicalDocStruct(physicaldocstruct);
        }

        if (directory == null) {
            checkIfImagesValid(process.getTitle(),
                    serviceManager.getProcessService().getImagesTifDirectory(true, process));
        } else {
            checkIfImagesValid(process.getTitle(),
                    serviceManager.getProcessService().getImagesDirectory(process) + directory);
        }

        /*
         * retrieve existing pages/images
         */
        DocStructType newPage = this.myPrefs.getDocStrctTypeByName("page");
        List<DocStruct> oldPages = physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*");
        if (oldPages == null) {
            oldPages = new ArrayList<DocStruct>();
        }

        /*
         * add new page/images if necessary
         */

        if (oldPages.size() == this.myLastImage) {
            return;
        }

        String defaultPagination = ConfigCore.getParameter("MetsEditorDefaultPagination", "uncounted");
        Map<String, DocStruct> assignedImages = new HashMap<String, DocStruct>();
        List<DocStruct> pageElementsWithoutImages = new ArrayList<DocStruct>();
        List<String> imagesWithoutPageElements = new ArrayList<String>();

        if (physicaldocstruct.getAllChildren() != null && !physicaldocstruct.getAllChildren().isEmpty()) {
            for (DocStruct page : physicaldocstruct.getAllChildren()) {
                if (page.getImageName() != null) {
                    File imageFile = null;
                    if (directory == null) {
                        imageFile = new File(serviceManager.getProcessService().getImagesTifDirectory(true, process),
                                page.getImageName());
                    } else {
                        imageFile = new File(serviceManager.getProcessService().getImagesDirectory(process) + directory,
                                page.getImageName());
                    }
                    if (imageFile.exists()) {
                        assignedImages.put(page.getImageName(), page);
                    } else {
                        try {
                            page.removeContentFile(page.getAllContentFiles().get(0));
                            pageElementsWithoutImages.add(page);
                        } catch (ContentFileNotLinkedException e) {
                            logger.error(e);
                        }
                    }
                } else {
                    pageElementsWithoutImages.add(page);

                }
            }

        }
        try {
            List<String> imageNamesInMediaFolder = getDataFiles(process);
            if (imageNamesInMediaFolder != null) {
                for (String imageName : imageNamesInMediaFolder) {
                    if (!assignedImages.containsKey(imageName)) {
                        imagesWithoutPageElements.add(imageName);
                    }
                }
            }
        } catch (InvalidImagesException e1) {
            logger.error(e1);
        }

        // handle possible cases

        // case 1: existing pages but no images (some images are removed)
        if (!pageElementsWithoutImages.isEmpty() && imagesWithoutPageElements.isEmpty()) {
            for (DocStruct pageToRemove : pageElementsWithoutImages) {
                physicaldocstruct.removeChild(pageToRemove);
                List<Reference> refs = new ArrayList<Reference>(pageToRemove.getAllFromReferences());
                for (ugh.dl.Reference ref : refs) {
                    ref.getSource().removeReferenceTo(pageToRemove);
                }
            }
        }
        // case 2: no page docs but images (some images are added)
        else if (pageElementsWithoutImages.isEmpty() && !imagesWithoutPageElements.isEmpty()) {
            int currentPhysicalOrder = assignedImages.size();
            for (String newImage : imagesWithoutPageElements) {
                DocStruct dsPage = this.mydocument.createDocStruct(newPage);
                try {
                    // physical page no
                    physicaldocstruct.addChild(dsPage);
                    MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
                    Metadata mdTemp = new Metadata(mdt);
                    mdTemp.setValue(String.valueOf(++currentPhysicalOrder));
                    dsPage.addMetadata(mdTemp);

                    // logical page no
                    mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
                    mdTemp = new Metadata(mdt);

                    if (defaultPagination.equalsIgnoreCase("arabic")) {
                        mdTemp.setValue(String.valueOf(currentPhysicalOrder));
                    } else if (defaultPagination.equalsIgnoreCase("roman")) {
                        RomanNumeral roman = new RomanNumeral();
                        roman.setValue(currentPhysicalOrder);
                        mdTemp.setValue(roman.getNumber());
                    } else {
                        mdTemp.setValue("uncounted");
                    }

                    dsPage.addMetadata(mdTemp);
                    log.addReferenceTo(dsPage, "logical_physical");

                    // image name
                    ContentFile cf = new ContentFile();
                    if (SystemUtils.IS_OS_WINDOWS) {
                        cf.setLocation("file:/"
                                + serviceManager.getProcessService().getImagesTifDirectory(false, process) + newImage);
                    } else {
                        cf.setLocation("file://"
                                + serviceManager.getProcessService().getImagesTifDirectory(false, process) + newImage);
                    }
                    dsPage.addContentFile(cf);

                } catch (TypeNotAllowedAsChildException e) {
                    logger.error(e);
                } catch (MetadataTypeNotAllowedException e) {
                    logger.error(e);
                }
            }
        }

        // case 3: empty page docs and unassinged images
        else {
            for (DocStruct page : pageElementsWithoutImages) {
                if (!imagesWithoutPageElements.isEmpty()) {
                    // assign new image name to page
                    String newImageName = imagesWithoutPageElements.get(0);
                    imagesWithoutPageElements.remove(0);
                    ContentFile cf = new ContentFile();
                    if (SystemUtils.IS_OS_WINDOWS) {
                        cf.setLocation(
                                "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                        + newImageName);
                    } else {
                        cf.setLocation(
                                "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                        + newImageName);
                    }
                    page.addContentFile(cf);
                } else {
                    // remove page
                    physicaldocstruct.removeChild(page);
                    List<Reference> refs = new ArrayList<Reference>(page.getAllFromReferences());
                    for (ugh.dl.Reference ref : refs) {
                        ref.getSource().removeReferenceTo(page);
                    }
                }
            }
            if (!imagesWithoutPageElements.isEmpty()) {
                // create new page elements

                int currentPhysicalOrder = physicaldocstruct.getAllChildren().size();
                for (String newImage : imagesWithoutPageElements) {
                    DocStruct dsPage = this.mydocument.createDocStruct(newPage);
                    try {
                        // physical page no
                        physicaldocstruct.addChild(dsPage);
                        MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
                        Metadata mdTemp = new Metadata(mdt);
                        mdTemp.setValue(String.valueOf(++currentPhysicalOrder));
                        dsPage.addMetadata(mdTemp);

                        // logical page no
                        mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
                        mdTemp = new Metadata(mdt);

                        if (defaultPagination.equalsIgnoreCase("arabic")) {
                            mdTemp.setValue(String.valueOf(currentPhysicalOrder));
                        } else if (defaultPagination.equalsIgnoreCase("roman")) {
                            RomanNumeral roman = new RomanNumeral();
                            roman.setValue(currentPhysicalOrder);
                            mdTemp.setValue(roman.getNumber());
                        } else {
                            mdTemp.setValue("uncounted");
                        }

                        dsPage.addMetadata(mdTemp);
                        log.addReferenceTo(dsPage, "logical_physical");

                        // image name
                        ContentFile cf = new ContentFile();
                        if (SystemUtils.IS_OS_WINDOWS) {
                            cf.setLocation(
                                    "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                            + newImage);
                        } else {
                            cf.setLocation(
                                    "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                            + newImage);
                        }
                        dsPage.addContentFile(cf);

                    } catch (TypeNotAllowedAsChildException e) {
                        logger.error(e);
                    } catch (MetadataTypeNotAllowedException e) {
                        logger.error(e);
                    }
                }

            }
        }
        int currentPhysicalOrder = 1;
        MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
        if (physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*") != null) {
            for (DocStruct page : physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*")) {
                List<? extends Metadata> pageNoMetadata = page.getAllMetadataByType(mdt);
                if (pageNoMetadata == null || pageNoMetadata.size() == 0) {
                    currentPhysicalOrder++;
                    break;
                }
                for (Metadata pageNo : pageNoMetadata) {
                    pageNo.setValue(String.valueOf(currentPhysicalOrder));
                }
                currentPhysicalOrder++;
            }
        }
    }

    /**
     * scale given image file to png using internal embedded content server.
     */
    public void scaleFile(String inFileName, String outFileName, int inSize, int intRotation)
            throws ImageManagerException, IOException, ImageManipulatorException {
        logger.trace("start scaleFile");
        int tmpSize = inSize / 3;
        if (tmpSize < 1) {
            tmpSize = 1;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("tmpSize: " + tmpSize);
        }
        if (ConfigCore.getParameter("goobiContentServerUrl", "").equals("")) {
            logger.trace("api");
            ImageManager im = new ImageManager(new File(inFileName).toURI().toURL());
            logger.trace("im");
            RenderedImage ri = im.scaleImageByPixel(tmpSize, tmpSize, ImageManager.SCALE_BY_PERCENT, intRotation);
            logger.trace("ri");
            JpegInterpreter pi = new JpegInterpreter(ri);
            logger.trace("pi");
            FileOutputStream outputFileStream = new FileOutputStream(outFileName);
            logger.trace("output");
            pi.writeToStream(null, outputFileStream);
            logger.trace("write stream");
            outputFileStream.close();
            logger.trace("close stream");
        } else {
            String cs = ConfigCore.getParameter("goobiContentServerUrl") + inFileName + "&scale=" + tmpSize + "&rotate="
                    + intRotation + "&format=jpg";
            cs = cs.replace("\\", "/");
            if (logger.isTraceEnabled()) {
                logger.trace("url: " + cs);
            }
            URL csUrl = new URL(cs);
            HttpClient httpclient = new HttpClient();
            GetMethod method = new GetMethod(csUrl.toString());
            logger.trace("get");
            Integer contentServerTimeOut = ConfigCore.getIntParameter("goobiContentServerTimeOut", 60000);
            method.getParams().setParameter("http.socket.timeout", contentServerTimeOut);
            int statusCode = httpclient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                return;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("statusCode: " + statusCode);
            }
            InputStream inStream = method.getResponseBodyAsStream();
            logger.trace("inStream");
            try (BufferedInputStream bis = new BufferedInputStream(inStream);
                    FileOutputStream fos = new FileOutputStream(outFileName);) {
                logger.trace("BufferedInputStream");
                logger.trace("FileOutputStream");
                byte[] bytes = new byte[8192];
                int count = bis.read(bytes);
                while (count != -1 && count <= 8192) {
                    fos.write(bytes, 0, count);
                    count = bis.read(bytes);
                }
                if (count != -1) {
                    fos.write(bytes, 0, count);
                }
            }
            logger.trace("write");
        }
        logger.trace("end scaleFile");
    }

    // Add a method to validate the image files

    /**
     * Die Images eines Prozesses auf Vollständigkeit prüfen.
     */
    public boolean checkIfImagesValid(String title, String folder)
            throws IOException, InterruptedException, SwapException, DAOException {
        boolean isValid = true;
        this.myLastImage = 0;

        /*
         * alle Bilder durchlaufen und dafür die Seiten anlegen
         */
        File dir = new File(folder);
        if (dir.exists()) {
            String[] dateien = dir.list(Helper.dataFilter);
            if (dateien == null || dateien.length == 0) {
                Helper.setFehlerMeldung("[" + title + "] No objects found");
                return false;
            }

            this.myLastImage = dateien.length;
            if (ConfigCore.getParameter("ImagePrefix", "\\d{8}").equals("\\d{8}")) {
                List<String> filesDirs = Arrays.asList(dateien);
                Collections.sort(filesDirs);
                int counter = 1;
                int myDiff = 0;
                String curFile = null;
                try {
                    for (Iterator<String> iterator = filesDirs.iterator(); iterator.hasNext(); counter++) {
                        curFile = iterator.next();
                        int curFileNumber = Integer.parseInt(curFile.substring(0, curFile.indexOf(".")));
                        if (curFileNumber != counter + myDiff) {
                            Helper.setFehlerMeldung("[" + title + "] expected Image " + (counter + myDiff)
                                    + " but found File " + curFile);
                            myDiff = curFileNumber - counter;
                            isValid = false;
                        }
                    }
                } catch (NumberFormatException e1) {
                    isValid = false;
                    Helper.setFehlerMeldung(
                            "[" + title + "] Filename of image wrong - not an 8-digit-number: " + curFile);
                }
                return isValid;
            }
            return true;
        }
        Helper.setFehlerMeldung("[" + title + "] No image-folder found");
        return false;
    }

    public static class KitodoImageFileComparator implements Comparator<String> {

        @Override
        public int compare(String s1, String s2) {
            String imageSorting = ConfigCore.getParameter("ImageSorting", "number");
            s1 = s1.substring(0, s1.lastIndexOf("."));
            s2 = s2.substring(0, s2.lastIndexOf("."));

            if (imageSorting.equalsIgnoreCase("number")) {
                try {
                    Integer i1 = Integer.valueOf(s1);
                    Integer i2 = Integer.valueOf(s2);
                    return i1.compareTo(i2);
                } catch (NumberFormatException e) {
                    return s1.compareToIgnoreCase(s2);
                }
            } else if (imageSorting.equalsIgnoreCase("alphanumeric")) {
                return s1.compareToIgnoreCase(s2);
            } else {
                return s1.compareToIgnoreCase(s2);
            }
        }

    }

    /**
     * Get image files.
     *
     * @param myProcess
     *            current process
     * @return sorted list with strings representing images of process
     */

    public ArrayList<String> getImageFiles(Process myProcess) throws InvalidImagesException {
        File dir;
        try {
            dir = new File(serviceManager.getProcessService().getImagesTifDirectory(true, myProcess));
        } catch (Exception e) {
            throw new InvalidImagesException(e);
        }
        /* Verzeichnis einlesen */
        String[] dateien = dir.list(Helper.imageNameFilter);
        ArrayList<String> dataList = new ArrayList<String>();
        if (dateien != null && dateien.length > 0) {
            for (int i = 0; i < dateien.length; i++) {
                String s = dateien[i];
                dataList.add(s);
            }
            /* alle Dateien durchlaufen */
            if (dataList.size() != 0) {
                Collections.sort(dataList, new KitodoImageFileComparator());
            }
            return dataList;
        } else {
            return null;
        }
    }

    /**
     * Get image files.
     *
     * @param myProcess
     *            current process
     * @param directory
     *            current folder
     * @return sorted list with strings representing images of process
     */
    public List<String> getImageFiles(Process myProcess, String directory) throws InvalidImagesException {
        File dir;
        try {
            dir = new File(serviceManager.getProcessService().getImagesDirectory(myProcess) + directory);
        } catch (Exception e) {
            throw new InvalidImagesException(e);
        }
        /* Verzeichnis einlesen */
        String[] dateien = dir.list(Helper.imageNameFilter);
        List<String> dataList = new ArrayList<String>();
        if (dateien != null && dateien.length > 0) {
            for (int i = 0; i < dateien.length; i++) {
                String s = dateien[i];
                dataList.add(s);
            }
            /* alle Dateien durchlaufen */
        }
        List<String> orderedFilenameList = new ArrayList<String>();
        if (dataList.size() != 0) {
            List<DocStruct> pagesList = mydocument.getPhysicalDocStruct().getAllChildren();
            if (pagesList != null) {
                for (DocStruct page : pagesList) {
                    String filename = page.getImageName();
                    String filenamePrefix = filename.replace(Metadaten.getFileExtension(filename), "");
                    for (String currentImage : dataList) {
                        String currentImagePrefix = currentImage.replace(Metadaten.getFileExtension(currentImage), "");
                        if (currentImagePrefix.equals(filenamePrefix)) {
                            orderedFilenameList.add(currentImage);
                            break;
                        }
                    }
                }
                // orderedFilenameList.add(page.getImageName());
            }

            if (orderedFilenameList.size() == dataList.size()) {
                return orderedFilenameList;

            } else {
                Collections.sort(dataList, new KitodoImageFileComparator());
                return dataList;
            }
        } else {
            return null;
        }
    }

    /**
     * Get image files.
     *
     * @param physical
     *            DocStruct object
     * @return list of Strings
     */
    public List<String> getImageFiles(DocStruct physical) {
        List<String> orderedFileList = new ArrayList<String>();
        List<DocStruct> pages = physical.getAllChildren();
        if (pages != null) {
            for (DocStruct page : pages) {
                String filename = page.getImageName();
                if (filename != null) {
                    orderedFileList.add(filename);
                } else {
                    logger.error("cannot find image");
                }
            }
        }
        return orderedFileList;
    }

    /**
     * Get data files.
     *
     * @param myProcess
     *            Process object
     * @return list of Strings
     */
    public List<String> getDataFiles(Process myProcess) throws InvalidImagesException {
        File dir;
        try {
            dir = new File(serviceManager.getProcessService().getImagesTifDirectory(true, myProcess));
        } catch (Exception e) {
            throw new InvalidImagesException(e);
        }
        /* Verzeichnis einlesen */
        String[] dateien = dir.list(Helper.dataFilter);
        ArrayList<String> dataList = new ArrayList<String>();
        if (dateien != null && dateien.length > 0) {
            for (int i = 0; i < dateien.length; i++) {
                String s = dateien[i];
                dataList.add(s);
            }
            /* alle Dateien durchlaufen */
            if (dataList.size() != 0) {
                Collections.sort(dataList, new KitodoImageFileComparator());
            }
            return dataList;
        } else {
            return null;
        }
    }

}
