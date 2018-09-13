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

package org.kitodo.data.database.beans;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.myfaces.util.FilenameUtils;
import org.kitodo.api.filemanagement.FileManagementInterface;
import org.kitodo.api.imagemanagement.ImageManagementInterface;
import org.kitodo.config.Config;
import org.kitodo.helper.LexicographicalOrder;
import org.kitodo.serviceloader.KitodoServiceLoader;
import org.kitodo.services.FolderGeneratorService;

/**
 * Stores configuration settings regarding a type of sub-folder in the process
 * directories of processes in a project.
 *
 * <p>
 * Typically, a folder has a corresponding sub-directory in the process
 * directory of each process and a {@code <fileGrp>} structure in the produced
 * METS file. The assumption is, that each folder contains the same number of
 * files with the same names, except for the file extension, which can vary.
 * This structure is used to represent different versions of the same object (a
 * small low quality JPEG thumbnail, a high quality JPEG, an OCR-processed PDF,
 * etc.) Each version is represented by one {@code Folder} object.
 *
 * <p>
 * The sub-directory can be located by appending the value of {@link #path} to
 * the path to the process directory. The {@code <fileGrp>} structure has the
 * {@code USE} attribute set to the value of {@link #fileGroup}. It contains
 * links to the files contained in the directory. The links are formed by
 * concatenating the {@link #urlStructure} with the simple name of the file.
 *
 * <p>
 * However, a {@code Folder} can also only exist on the drive without being
 * exported to METS. Or, it can exist only virtually without correspondence on a
 * drive, just to produce the METS {@code <fileGrp>} structure.
 */
@Entity
@Table(name = "folder")
public class Folder extends BaseBean {
    /**
     * Default {@code fileGrp}s supported by the DFG viewer. The list is used to
     * populate a combo box in the edit dialog.
     *
     * @see "https://www.zvdd.de/fileadmin/AGSDD-Redaktion/METS_Anwendungsprofil_2.0.pdf#page=12"
     */
    private static final List<String> DFG_VIEWER_FILEGRPS = Arrays.asList("DEFAULT", "MIN", "MAX", "THUMBS",
        "DOWNLOAD");
    private static final BinaryOperator<URI> LATEST_URI = (previous, latest) -> latest;
    private static final Supplier<TreeMap<String, URI>> MAP_FACTORY = () -> new TreeMap<>(new LexicographicalOrder());
    private static final long serialVersionUID = -627255829641460322L;

    /**
     * Whether the folder is copied to the hotfolder during DMS import.
     */
    @Column(name = "copyFolder")
    private boolean copyFolder = true;

    /**
     * Whether the folder is created empty when a new process is created.
     */
    @Column(name = "createFolder")
    private boolean createFolder = true;

    /**
     * If not null, images in this folder can be generated by the function
     * {@link ImageManagementInterface#createDerivative(java.net.URI, double,
     * java.net.URI, org.kitodo.api.imagemanagement.ImageFileFormat)}.
     * The value is the factor of scaling for the derivative, a value of 1.0
     * indicates the original size.
     */
    @Column(name = "derivative")
    private Double derivative = null;

    /**
     * If not null, images in this folder can be generated by the function
     * {@link ImageManagementInterface#changeDpi(java.net.URI, int)}. The value
     * is the new DPI for the images.
     */
    @Column(name = "dpi")
    private Integer dpi = null;

    /**
     * {@code USE} identifier keyword for the METS {@code <fileGrp>} in which
     * contents of this folder will be linked.
     */
    @Column(name = "fileGroup")
    private String fileGroup = "";

    /**
     * An encapsulation of the content generator properties of the folder in a
     * way suitable to the JSF design.
     */
    @Transient
    private FolderGeneratorService generator = new FolderGeneratorService(this);

    /**
     * If not null, images in this folder can be generated by the function
     * {@link ImageManagementInterface#getScaledWebImage(java.net.URI, double)}.
     * The value is the factor of scaling for the derivative, a value of 1.0
     * indicates the original size.
     */
    @Column(name = "imageScale")
    private Double imageScale = null;

    /**
     * If not null, images in this folder can be generated by the function
     * {@link ImageManagementInterface#getSizedWebImage(java.net.URI, int)}. The
     * value is the new the new width in pixels.
     */
    @Column(name = "imageSize")
    private Integer imageSize = null;

    /**
     * Indicates whether a METS {@code <fileGrp>} section is created, and how it
     * is populated.
     */
    @Column(name = "linkingMode")
    @Enumerated(EnumType.STRING)
    private LinkingMode linkingMode = LinkingMode.ALL;

    /**
     * The Internet MIME type of the files contained in this folder. The MIME
     * type is used to derive depending settings, such as the file extension, or
     * which content processors can be employed.
     *
     * @see org.kitodo.config.xml.fileformats.FileFormatsConfig
     */
    @Column(name = "mimeType")
    private String mimeType = "image/jpeg";

    /**
     * The path to the folder in the process directory of each processes.
     *
     * <p>
     * This variable represents quite a lot of functionality. On the one hand,
     * placeholders for variables can be contained in the path, which are then
     * replaced at runtime. These placeholders are written as keywords enclosed
     * in parentheses. For example, {@code (processtitle)} is to be replaced by
     * the respective processes’ title. Therefore, you always have to pass a map
     * with the variables when requesting {@link #getRelativePath(Map)} or
     * {@link #getURI(Map, String, String)}. The map can be obtained from
     * {@link de.sub.goobi.helper.VariableReplacer#mapOfVariables()}.
     *
     * <p>
     * Secondly, it is also possible to define folders in such a way that the
     * contents for several folders are stored in the same directory. This is an
     * obsolete concept that should not be used anymore, but that we continue to
     * support because there are legacy systems whose amounts of data files
     * cannot easily be migrated. This behavior is enabled when there is an
     * asterisk after the last directory separator. In that case, the contents
     * of the folder must be listed with a special filter that results from this
     * pattern. Thus, two files representing the same work have a common name
     * part, hereafter called canonical, and a name part for the folder type.
     * Therefore, the function {@link #listContents(Map, String)} returns a map
     * that maps from the canonical part to the associated URI. In turn, the
     * function {@link #getURI(Map, String, String)} takes the canonical part of
     * the file name to form the requested URI.
     */
    @Column(name = "path")
    private String path = "";

    /**
     * The project this folder is configured in.
     */
    @ManyToOne
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "FK_folder_project_id"))
    private Project project = null;

    /**
     * URL path where the contents of the linked METS {@code <fileGrp>} will be
     * published on a web server. The path may contain variables that must be
     * replaced before concatenation.
     */
    @Column(name = "urlStructure")
    private String urlStructure;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Folder that = (Folder) o;
        return Objects.equals(fileGroup, that.fileGroup) && Objects.equals(urlStructure, that.urlStructure)
                && Objects.equals(mimeType, that.mimeType) && Objects.equals(path, that.path)
                && Objects.equals(project, that.project) && copyFolder == that.copyFolder
                && createFolder == that.createFolder && Objects.equals(derivative, that.derivative)
                && Objects.equals(dpi, that.dpi) && Objects.equals(imageScale, that.imageScale)
                && Objects.equals(imageSize, that.imageSize) && Objects.equals(linkingMode, that.linkingMode);
    }

    /**
     * Returns the scale factor to create the contents of the folder as
     * derivative form the content of another folder, if any. If absent, the
     * function is disabled.
     *
     * @return the scale factor. A value of 1.0 refers to the original size.
     */
    public Optional<Double> getDerivative() {
        return Optional.ofNullable(derivative);
    }

    /**
     * Returns the number of DPI to change the resolution of the contents of the
     * folder form the content of another folder, if any. If absent, the
     * function is disabled.
     *
     * @return the resolution
     */
    public Optional<Integer> getDpi() {
        return Optional.ofNullable(dpi);
    }

    /**
     * Returns the file group of the folder.
     *
     * @return the file group
     */
    public String getFileGroup() {
        return this.fileGroup;
    }

    /**
     * Returns the pre-defined entries for the combo box to select the METS use
     * in {@code projectEditMetsPopup.xhtml}.
     *
     * @return the pre-defined entries for the combo box
     */
    public Collection<String> getFileGroups() {
        Collection<String> result = new TreeSet<>(DFG_VIEWER_FILEGRPS);
        result.add(this.fileGroup);
        return result;
    }

    /**
     * Returns an encapsulation to access the generator properties of the folder
     * in a JSF-friendly way.
     *
     * @return the generator controller
     */
    public FolderGeneratorService getGenerator() {
        return generator;
    }

    /**
     * Returns the scale factor to get the contents of the folder as scaled web
     * images form the content of another folder, if any. If absent, the
     * function is disabled.
     *
     * @return the scale factor. A value of 1.0 refers to the original size.
     */
    public Optional<Double> getImageScale() {
        return Optional.ofNullable(imageScale);
    }

    /**
     * Returns the pixel width to get the contents of the folder as sized web
     * images form the content of another folder, if any. If absent, the
     * function is disabled.
     *
     * @return the pixel width
     */
    public Optional<Integer> getImageSize() {
        return Optional.ofNullable(imageSize);
    }

    /**
     * Returns the linking mode of the folder.
     *
     * @return the linking mode
     */
    public LinkingMode getLinkingMode() {
        return linkingMode;
    }

    /**
     * Returns the MIME type of the folder.
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * Returns the path pattern, containing the path to the folder relative to
     * the process directory, and maybe an extra file name pattern.
     *
     * @deprecated This getter is here to get the content of the {@link #path}
     *             field, which is an abstract string containing placeholders,
     *             and optionally file name formatting patterns. It will not
     *             return a resolved path on the file system. Use
     *             {@link #getRelativePath(Map)} to obtain the path to the
     *             folder on the file system.
     *
     * @return path with optional filename pattern
     */
    @Deprecated
    public String getPath() {
        return this.path;
    }

    /**
     * Returns the project of the folder.
     *
     * @return the project
     */
    public Project getProject() {
        return this.project;
    }

    /**
     * Returns the path to the folder.
     *
     * @return the path
     */
    @Transient
    public String getRelativePath(Map<String, String> vars) {
        int lastDelimiter = path.lastIndexOf(File.separatorChar);
        return replaceInString(path.substring(lastDelimiter + 1).indexOf('*') > -1
                ? lastDelimiter >= 0 ? path.substring(0, lastDelimiter) : ""
                : path,
            vars);
    }

    /**
     * Returns the filename suffix with file extension for the UGH library.
     *
     * @deprecated This is a temporary solution and should no longer be used
     *             after that the UGH is removed.
     * @param extensionWithoutDot
     *            filename extension without dot, to be read from the
     *            configuration. The extension can be retrieved from the
     *            configuration based on the mimeType, but reading the
     *            configuration is part of the core module, so it cannot be done
     *            here and must be returned here from the caller.
     * @return the filename suffix with file extension
     */
    @Deprecated
    @Transient
    public String getUGHTail(String extensionWithoutDot) {
        String lastSegment = path.substring(path.lastIndexOf(File.separatorChar) + 1);
        if (lastSegment.indexOf('*') > -1) {
            if (lastSegment.startsWith("*.")) {
                String tail = lastSegment.substring(2);
                if (tail.endsWith(".*")) {
                    tail = tail.substring(0, tail.length() - 1).concat(extensionWithoutDot);
                }
                return tail;
            } else {
                throw new UnsupportedOperationException("The UGH does not support file name prefixes");
            }
        } else {
            return extensionWithoutDot;
        }
    }

    /**
     * Returns the URI to a file in this folder.
     *
     * @param vars
     *            a map of the variable assignments of the variables that can
     *            occur in the {@link #path}
     * @param canonical
     *            the canonical part of the file name. The canonical part is the
     *            part that is different from one file to another in the folder,
     *            but equal between two files representing the same content in
     *            two different folders. A typical canonical part could be
     *            “00000001”.
     * @param extensionWithoutDot
     *            filename extension without dot, to be read from the
     *            configuration. The extension can be retrieved from the
     *            configuration based on the MIME type, but reading the
     *            configuration is part of the core module, so it cannot be done
     *            here and must be returned here from the caller. Typically,
     *            this function has to be called like this: <code>folder<!--
     *            -->.getURI(vars, canonical, FileFormatsConfig<!--
     *            -->.getFileFormat(folder.getMimeType()).get()<!--
     *            -->.getExtension(false))</code>
     * @return composed URI
     */
    @Transient
    public URI getURI(Map<String, String> vars, String canonical, String extensionWithoutDot) {
        int lastSeparator = path.lastIndexOf(File.separatorChar);
        String lastSegment = path.substring(lastSeparator + 1);
        String projectId = project.getId().toString();
        if (lastSegment.indexOf('*') == -1) {
            String localName = canonical + '.' + extensionWithoutDot;
            return Paths.get(Config.getKitodoDataDirectory(), projectId, replaceInString(path, vars), localName)
                    .toUri();
        } else {
            String realPath = path.substring(0, lastSeparator);
            String localName = lastSegment.replaceFirst("\\*", canonical).replaceFirst("\\*$", extensionWithoutDot);
            if (realPath.isEmpty()) {
                return Paths.get(Config.getKitodoDataDirectory(), projectId, localName).toUri();
            } else {
                return Paths.get(Config.getKitodoDataDirectory(), projectId, replaceInString(realPath, vars), localName)
                        .toUri();
            }
        }
    }

    /**
     * Returns the URI to a file in this folder if that file does exist.
     *
     * @param vars
     *            a map of the variable assignments of the variables that can
     *            occur in the {@link #path}
     * @param canonical
     *            the canonical part of the file name. The canonical part is the
     *            part that is different from one file to another in the folder,
     *            but equal between two files representing the same content in
     *            two different folders. A typical canonical part could be
     *            “00000001”.
     * @param extensionWithoutDot
     *            filename extension without dot, to be read from the
     *            configuration. The extension can be retrieved from the
     *            configuration based on the MIME type, but reading the
     *            configuration is part of the core module, so it cannot be done
     *            here and must be returned here from the caller. Typically,
     *            this function has to be called like this: <code>folder<!--
     *            -->.getURIIfExists(vars, canonical, FileFormatsConfig<!--
     *            -->.getFileFormat(folder.getMimeType()).get()<!--
     *            -->.getExtension(false))</code>
     * @return URI, or empty
     */
    @Transient
    public Optional<URI> getURIIfExists(Map<String, String> vars, String canonical, String extensionWithoutDot) {
        URI uri = getURI(vars, canonical, extensionWithoutDot);
        return new File(uri.getPath()).isFile() ? Optional.of(uri) : Optional.empty();
    }

    /**
     * Returns the URL structure of the folder.
     *
     * @return the URL structure
     */
    public String getUrlStructure() {
        return this.urlStructure;
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileGroup, urlStructure, mimeType, path, copyFolder, createFolder, derivative, dpi,
            imageScale, imageSize, linkingMode);
    }

    /**
     * Returns whether the folder is copy folder.
     *
     * @return whether the folder is copy folder
     */
    public boolean isCopyFolder() {
        return copyFolder;
    }

    /**
     * Returns whether the folder is created on process creation.
     *
     * @return whether the folder is created on process creation
     */
    public boolean isCreateFolder() {
        return createFolder;
    }

    /**
     * Returns a map of canonical file name parts to URIs with all files
     * actually contained in this folder. The canonical part is the part that is
     * different from one file to another in the folder, but equal between two
     * files representing the same content in two different folders. A typical
     * canonical part could be “00000001”.
     *
     * @param extensionWithoutDot
     *            filename extension without dot, to be read from the
     *            configuration. The extension can be retrieved from the
     *            configuration based on the mimeType, but reading the
     *            configuration is part of the core module, so it cannot be done
     *            here and must be returned here from the caller.
     * @return map of canonical file name parts to URIs
     */
    @Transient
    public Map<String, URI> listContents(Map<String, String> vars, String extensionWithoutDot) {
        int lastSeparator = path.lastIndexOf(File.separatorChar);
        String lastSegment = path.substring(lastSeparator + 1);
        String projectId = project.getId().toString();
        URI root;
        String pattern;
        int firstStar = lastSegment.indexOf('*');
        if (firstStar == -1) {
            root = Paths.get(Config.getKitodoDataDirectory(), projectId, replaceInString(path, vars)).toUri();
            pattern = "(.*)\\." + Pattern.quote(extensionWithoutDot);
        } else {
            String realPath = replaceInString(path.substring(0, lastSeparator), vars);
            root = (realPath.isEmpty() ? Paths.get(Config.getKitodoDataDirectory(), projectId)
                    : Paths.get(Config.getKitodoDataDirectory(), projectId, realPath)).toUri();
            StringBuilder patternBuilder = new StringBuilder();
            if (firstStar > 0) {
                patternBuilder.append(Pattern.quote(lastSegment.substring(0, firstStar)));
            }
            patternBuilder.append("(.*)");
            if (firstStar < lastSegment.length() - 1) {
                patternBuilder.append(
                    Pattern.quote(lastSegment.substring(firstStar + 1).replaceFirst("\\*$", extensionWithoutDot)));
            }
            pattern = patternBuilder.toString();
        }
        KitodoServiceLoader<FileManagementInterface> fileManagementInterface = new KitodoServiceLoader<>(
                FileManagementInterface.class);
        final Pattern compiledPattern = Pattern.compile(pattern);
        FilenameFilter filter = (dir, name) -> compiledPattern.matcher(name).matches();
        Stream<URI> relativeURIs = fileManagementInterface.loadModule().getSubUris(filter, root).parallelStream();
        Stream<URI> absoluteURIs = relativeURIs
                .map(λ -> new File(FilenameUtils.concat(Config.getKitodoDataDirectory(), λ.getPath())).toURI());
        return absoluteURIs.collect(Collectors.toMap(new Function<URI, String>() {
            @Override
            public String apply(URI uri) {
                Matcher matcher = compiledPattern.matcher(FilenameUtils.getName(uri.getPath()));
                matcher.matches();
                return matcher.group(1);
            }
        }, Function.identity(), LATEST_URI, MAP_FACTORY));
    }

    @Transient
    private static String replaceInString(String string, Map<String, String> replacements) {
        for (Entry<String, String> replacement : replacements.entrySet()) {
            string = string.replace('(' + replacement.getKey() + ')', replacement.getValue());
        }
        return string;
    }

    /**
     * Sets whether the folder is copied on DMS import.
     *
     * @param copyFolder
     *            whether the folder is copied on DMS import
     */
    public void setCopyFolder(boolean copyFolder) {
        this.copyFolder = copyFolder;
    }

    /**
     * Sets whether the folder is created on process creation.
     *
     * @param createFolder
     *            whether the folder is created on process creation
     */
    public void setCreateFolder(boolean createFolder) {
        this.createFolder = createFolder;
    }

    /**
     * Sets the scale factor to create the contents of the folder as derivative
     * form the content of another folder. Can be set to {@code null} to disable
     * the function.
     *
     * @param derivative
     *            the scale factor. A value of 1.0 refers to the original size.
     */
    public void setDerivative(Double derivative) {
        this.derivative = derivative;
    }

    /**
     * Sets the number of DPI to change the resolution of the contents of the
     * folder form the content of another folder. Can be set to {@code null} to
     * disable the function.
     *
     * @param dpi
     *            resolution to set
     */
    public void setDpi(Integer dpi) {
        this.dpi = dpi;
    }

    /**
     * Sets the file group of the folder.
     *
     * @param fileGroup
     *            file group to set
     */
    public void setFileGroup(String fileGroup) {
        this.fileGroup = fileGroup;
    }

    /**
     * Sets the scale factor to get the contents of the folder as scaled web
     * images form the content of another folder. Can be set to {@code null} to
     * disable the function.
     *
     * @param imageScale
     *            the scale factor. A value of 1.0 refers to the original size.
     */
    public void setImageScale(Double imageScale) {
        this.imageScale = imageScale;
    }

    /**
     * Returns the pixel width to get the contents of the folder as sized web
     * images form the content of another folder, if any. If absent, the
     * function is disabled.
     *
     * @param imageSize
     *            the pixel width
     */
    public void setImageSize(Integer imageSize) {
        this.imageSize = imageSize;
    }

    /**
     * Sets the linking mode of the folder.
     *
     * @param linkingMode
     *            linking mode to set
     */
    public void setLinkingMode(LinkingMode linkingMode) {
        this.linkingMode = linkingMode;
    }

    /**
     * Sets the MIME type of the folder.
     *
     * @param mimeType
     *            MIME type to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Sets the path pattern, containing the path to the folder relative to the
     * process directory, and maybe an extra file name pattern. This getter is
     * here to be used by Hibernate and JSF to access the field value, but
     * should not be used for other purpose, unless you know what you are doing.
     *
     * @param path
     *            pat to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the project of the folder.
     *
     * @param project
     *            project to set
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Sets the URL structure of the folder.
     *
     * @param urlStructure
     *            URL structure to set
     */
    public void setUrlStructure(String urlStructure) {
        this.urlStructure = urlStructure;
    }

    @Override
    public String toString() {
        return path + (path.isEmpty() || fileGroup.isEmpty() ? "" : ", ") + fileGroup;
    }
}
