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

package org.kitodo.dataformat.access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.tuple.Pair;
import org.kitodo.api.MdSec;
import org.kitodo.api.Metadata;
import org.kitodo.api.MetadataEntry;
import org.kitodo.api.MetadataGroup;
import org.kitodo.api.dataformat.ExistingOrLinkedStructure;
import org.kitodo.api.dataformat.MediaUnit;
import org.kitodo.api.dataformat.Structure;
import org.kitodo.api.dataformat.View;
import org.kitodo.api.dataformat.mets.InputStreamProviderInterface;
import org.kitodo.dataformat.metskitodo.AmdSecType;
import org.kitodo.dataformat.metskitodo.DivType;
import org.kitodo.dataformat.metskitodo.KitodoType;
import org.kitodo.dataformat.metskitodo.MdSecType;
import org.kitodo.dataformat.metskitodo.MdSecType.MdWrap;
import org.kitodo.dataformat.metskitodo.MdSecType.MdWrap.XmlData;
import org.kitodo.dataformat.metskitodo.MetadataGroupType;
import org.kitodo.dataformat.metskitodo.MetadataType;
import org.kitodo.dataformat.metskitodo.Mets;

/**
 * The tree-like outline structure for digital representation. This structuring
 * structure can be subdivided into arbitrary finely granular
 * {@link #substructures}. It can be described by {@link #metadata}.
 */
public class DivXmlElementAccess extends Structure {
    /**
     * The qualified name of the Kitodo meta-data format, needed to assemble the
     * meta-data entries in METS using JAXB.
     */
    private static final QName KITODO_QNAME = new QName("http://meta.kitodo.org/v1/", "kitodo");

    /**
     * Some magic numbers that are used in the METS XML file representation of
     * this structure to describe relations between XML elements. They need to
     * be stored because some scatty third-party scripts rely on them not being
     * changed anymore once assigned.
     */
    private final String metsReferrerId;

    /**
     * Creates a new DivXmlElementAccess.
     */
    public DivXmlElementAccess() {
        super();
        metsReferrerId = UUID.randomUUID().toString();
    }

    /**
     * Creates a new DivXmlElementAccess for an existing structure.
     */
    DivXmlElementAccess(Structure structure) {
        super(structure);
        metsReferrerId = structure instanceof DivXmlElementAccess ? ((DivXmlElementAccess) structure).metsReferrerId
                : UUID.randomUUID().toString();
    }

    /**
     * Constructor to read a structure from METS.
     *
     * @param div
     *            METS {@code <div>} element from which the structure is to be
     *            built
     * @param mets
     *            METS data structure from which it is possible to determine
     *            what kind of metadata section is linked
     * @param mediaUnitsMap
     *            From this map, the media units are read, which must be
     *            referenced here by their ID.
     * @param inputStreamProvider
     *            a function that opens an input stream
     * @throws IOException
     *             if the reading fails
     */
    DivXmlElementAccess(DivType div, Mets mets, Map<String, Set<FileXmlElementAccess>> mediaUnitsMap,
            InputStreamProviderInterface inputStreamProvider) throws IOException {
        super();
        super.setLabel(div.getLABEL());
        for (Object mdSecType : div.getDMDID()) {
            super.getMetadata().addAll(readMetadata((MdSecType) mdSecType, MdSec.DMD_SEC));
        }
        for (Object mdSecType : div.getADMID()) {
            super.getMetadata().addAll(readMetadata((MdSecType) mdSecType, amdSecTypeOf(mets, (MdSecType) mdSecType)));
        }
        metsReferrerId = div.getID();
        super.setOrderlabel(div.getORDERLABEL());
        for (DivType child : div.getDiv()) {
            if (child.getMptr().isEmpty()) {
                super.getChildren().add(new DivXmlElementAccess(child, mets, mediaUnitsMap, inputStreamProvider));
            } else {
                super.getChildren()
                        .add(new MptrXmlElementAccess(child, mets, inputStreamProvider).getLinkedStructure());
            }
        }
        super.setType(div.getTYPE());
        Set<FileXmlElementAccess> fileXmlElementAccesses = mediaUnitsMap.get(div.getID());
        if (Objects.nonNull(fileXmlElementAccesses)) {
            for (FileXmlElementAccess fileXmlElementAccess : fileXmlElementAccesses) {
                if (Objects.nonNull(fileXmlElementAccess)) {
                    super.getViews().add(new AreaXmlElementAccess(fileXmlElementAccess).getView());
                }
            }
        }
    }

    /**
     * Determines from a METS data structure of which type is a meta-data
     * section.
     *
     * <p>
     * Implementation note: This method would be a good candidate for
     * parallelization.
     *
     * @param mets
     *            METS data structure that determines what type of meta-data
     *            section is
     * @param mdSec
     *            administrative meta-data section whose type is to be
     *            determined
     * @return the type of administrative meta-data section
     */
    static final MdSec amdSecTypeOf(Mets mets, MdSecType mdSec) {
        for (AmdSecType amdSec : mets.getAmdSec()) {
            if (amdSec.getSourceMD().contains(mdSec)) {
                return MdSec.SOURCE_MD;
            } else if (amdSec.getDigiprovMD().contains(mdSec)) {
                return MdSec.DIGIPROV_MD;
            } else if (amdSec.getRightsMD().contains(mdSec)) {
                return MdSec.RIGHTS_MD;
            } else if (amdSec.getTechMD().contains(mdSec)) {
                return MdSec.TECH_MD;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Reads a meta-data section and adds the meta-data to the structure.
     *
     * @param mdSecType
     *            type of meta-data section
     * @param mdSec
     *            meta-data section to be read
     *
     * @return
     */
    static final Collection<Metadata> readMetadata(MdSecType mdSecType, MdSec mdSec) {
        Collection<Metadata> metadata = new HashSet<>();
        for (Object object : mdSecType.getMdWrap().getXmlData().getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) object;
                Object value = jaxbElement.getValue();
                if (value instanceof KitodoType) {
                    KitodoType kitodoType = (KitodoType) value;
                    for (MetadataType metadataEntry : kitodoType.getMetadata()) {
                        metadata.add(new MetadataXmlElementAccess(mdSec, metadataEntry).getMetadataEntry());
                    }
                    for (MetadataGroupType metadataGroup : kitodoType.getMetadataGroup()) {
                        metadata.add(new MetadataGroupXmlElementAccess(mdSec, metadataGroup).getMetadataGroup());
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * Creates a METS {@code <div>} element from this structure.
     *
     * @param mediaUnitIDs
     *            the assigned identifier for each media unit so that the link
     *            pairs of the struct link section can be formed later
     * @param smLinkData
     *            the link pairs of the struct link section are added to this
     *            list
     * @param mets
     *            the METS structure in which the meta-data is added
     * @return a METS {@code <div>} element
     */
    DivType toDiv(Map<MediaUnit, String> mediaUnitIDs, LinkedList<Pair<String, String>> smLinkData, Mets mets) {
        DivType div = new DivType();
        div.setID(metsReferrerId);
        div.setLABEL(super.getLabel());
        div.setORDERLABEL(super.getOrderlabel());
        div.setTYPE(super.getType());
        smLinkData.addAll(super.getViews().parallelStream().map(View::getMediaUnit).map(mediaUnitIDs::get)
                .map(mediaUnitId -> Pair.of(metsReferrerId, mediaUnitId)).collect(Collectors.toList()));

        Optional<MdSecType> optionalDmdSec = createMdSec(super.getMetadata(), MdSec.DMD_SEC);
        if (optionalDmdSec.isPresent()) {
            MdSecType dmdSec = optionalDmdSec.get();
            String name = metsReferrerId + ':' + MdSec.DMD_SEC.toString();
            dmdSec.setID(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString());
            mets.getDmdSec().add(dmdSec);
            div.getDMDID().add(dmdSec);
        }
        Optional<AmdSecType> optionalAmdSec = createAmdSec(super.getMetadata(), metsReferrerId, div);
        if (optionalAmdSec.isPresent()) {
            AmdSecType admSec = optionalAmdSec.get();
            mets.getAmdSec().add(admSec);
        }

        for (ExistingOrLinkedStructure substructure : super.getChildren()) {
            div.getDiv().add(new DivXmlElementAccess((Structure) substructure).toDiv(mediaUnitIDs, smLinkData, mets));
        }
        return div;
    }

    /**
     * Creates a meta-data section of the specified domain of the Kitodo type
     * and returns it with its connection to the METS if there is data for it.
     *
     * @param domain
     *            Domain for which a metadata section is to be generated
     * @return a metadata section, if there is data for it
     */
    static Optional<MdSecType> createMdSec(Iterable<Metadata> metadata, MdSec domain) {
        KitodoType kitodoType = new KitodoType();
        for (Metadata entry : metadata) {
            if (domain.equals(entry.getDomain())) {
                if (entry instanceof MetadataEntry) {
                    kitodoType.getMetadata().add(new MetadataXmlElementAccess((MetadataEntry) entry).toMetadata());
                } else if (entry instanceof MetadataGroup) {
                    kitodoType.getMetadataGroup()
                            .add(new MetadataGroupXmlElementAccess((MetadataGroup) entry).toXMLMetadataGroup());
                }
            }
        }
        if (kitodoType.getMetadata().isEmpty() && kitodoType.getMetadataGroup().isEmpty()) {
            return Optional.empty();
        } else {
            XmlData xmlData = new XmlData();
            xmlData.getAny().add(new JAXBElement<>(KITODO_QNAME, KitodoType.class, kitodoType));
            MdWrap mdWrap = new MdWrap();
            mdWrap.setXmlData(xmlData);
            MdSecType dmdSec = new MdSecType();
            dmdSec.setMdWrap(mdWrap);
            return Optional.of(dmdSec);
        }
    }

    /**
     * Generates an {@code <amdSec>} if administrative meta-data exists on this
     * structure.
     *
     * @param div
     *            div where ADMID references must be added to the generated
     *            meta-data sections
     * @return an {@code <amdSec>}, if necessary
     */
    static Optional<AmdSecType> createAmdSec(Iterable<Metadata> metadata, String metsReferrerId, DivType div) {
        AmdSecType amdSec = new AmdSecType();
        boolean source = addMdSec(createMdSec(metadata, MdSec.SOURCE_MD), metsReferrerId, MdSec.SOURCE_MD,
            AmdSecType::getSourceMD, amdSec, div);
        boolean digiprov = addMdSec(createMdSec(metadata, MdSec.DIGIPROV_MD), metsReferrerId, MdSec.DIGIPROV_MD,
            AmdSecType::getDigiprovMD, amdSec, div);
        boolean rights = addMdSec(createMdSec(metadata, MdSec.RIGHTS_MD), metsReferrerId, MdSec.RIGHTS_MD,
            AmdSecType::getRightsMD, amdSec, div);
        boolean tech = addMdSec(createMdSec(metadata, MdSec.TECH_MD), metsReferrerId, MdSec.TECH_MD,
            AmdSecType::getTechMD, amdSec, div);
        return source || digiprov || rights || tech ? Optional.of(amdSec) : Optional.empty();
    }

    /**
     * Adds a meta-data section to an administrative meta-data section, if there
     * is one. This function deduplicates fourfold existing function for four
     * different meta-data sections.
     *
     * @param optionalMdSec
     *            perhaps existing meta-data section to be added if it exists
     * @param mdSecType
     *            the type of the mdSec, used in ID generation
     * @param mdSecTypeGetter
     *            the getter via which the meta-data section can be added to the
     *            administrative meta-data section
     * @param amdSec
     *            administrative meta-data section to which the meta-data
     *            section should be added, if any
     * @param div
     *            div where ADMID references must be added to the generated
     *            meta-data sections
     * @return whether something has been added to the administrative meta-data
     *         section
     */
    private static boolean addMdSec(Optional<MdSecType> optionalMdSec, String metsReferrerId, MdSec mdSecType,
            Function<AmdSecType, List<MdSecType>> mdSecTypeGetter, AmdSecType amdSec, DivType div) {

        if (!optionalMdSec.isPresent()) {
            return false;
        } else {
            MdSecType mdSec = optionalMdSec.get();
            String name = metsReferrerId + ':' + mdSecType.toString();
            mdSec.setID(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString());
            mdSecTypeGetter.apply(amdSec).add(mdSec);
            div.getADMID().add(mdSec);
            return true;
        }
    }
}
