/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReferenceType;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class BibliographicReference extends BibliographicReference_Base {

    public static final Comparator<BibliographicReference> COMPARATOR_BY_ORDER =
            Comparator.comparing(BibliographicReference::getReferenceOrder).thenComparing(BibliographicReference::getTitle)
                    .thenComparing(BibliographicReference::getYear).thenComparing(BibliographicReference::getExternalId);

    public BibliographicReference() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    @Deprecated
    public static BibliographicReference create(final String title, final String authors, final String reference,
            final String year, final Boolean optional) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        final BibliographicReference result = new BibliographicReference();
        result.setTitle(title); //FIXME: remove me
        result.setLocalizedTitle(new LocalizedString(Locale.getDefault(), title));
        result.setAuthors(authors);
        result.setReference(reference); //FIXME: remove me
        result.setLocalizedReference(new LocalizedString(Locale.getDefault(), reference));
        result.setYear(year);
        result.setOptional(optional);

        return result;
    }

    public static BibliographicReference create(final LocalizedString title, final String authors,
            final LocalizedString reference, final String year, final String url, final Integer referenceOrder,
            final Boolean optional) {
//        if (title == null || authors == null || year == null || optional == null) {
//            throw new IllegalArgumentException(BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
//        }

        final BibliographicReference result = new BibliographicReference();
        result.setLocalizedTitle(title);
        result.setAuthors(authors);
        result.setLocalizedReference(reference);
        result.setYear(year);
        result.setUrl(url);
        result.setOptional(optional);
        result.setReferenceOrder(referenceOrder);

        return result;
    }

    /**
     * Copies the provided Bibliographic Reference.
     * Note that this methods only copies the fields of this object. Not the relationships.
     * 
     * @param bibliographicReferenceToCopy
     * @return copied BibliographicReference
     */
    @Deprecated
    public static BibliographicReference copy(BibliographicReference bibliographicReferenceToCopy) {
        if (StringUtils.isBlank(bibliographicReferenceToCopy.getTitle())) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        final Function<LocalizedString, LocalizedString> copy = ls -> ls == null ? null : ls.builder().build();

        BibliographicReference copiedReference = create(copy.apply(bibliographicReferenceToCopy.getLocalizedTitle()),
                bibliographicReferenceToCopy.getAuthors(), copy.apply(bibliographicReferenceToCopy.getLocalizedReference()),
                bibliographicReferenceToCopy.getYear(), bibliographicReferenceToCopy.getUrl(),
                bibliographicReferenceToCopy.getReferenceOrder(), bibliographicReferenceToCopy.getOptional());
        copiedReference.setTitle(bibliographicReferenceToCopy.getTitle()); //FIXME: remove me
        copiedReference.setReference(bibliographicReferenceToCopy.getReference()); //FIXME: remove me
        return copiedReference;
    }

    public BibliographicReference copy() {
        if (StringUtils.isBlank(this.getTitle())) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        final Function<LocalizedString, LocalizedString> copy = ls -> ls == null ? null : ls.builder().build();

        BibliographicReference copiedReference =
                create(copy.apply(this.getLocalizedTitle()), this.getAuthors(), copy.apply(this.getLocalizedReference()),
                        this.getYear(), this.getUrl(), this.getReferenceOrder(), this.getOptional());
        copiedReference.setTitle(this.getTitle()); //FIXME: remove me
        copiedReference.setReference(this.getReference()); //FIXME: remove me
        return copiedReference;
    }

    @Deprecated
    public void edit(final String title, final String authors, final String reference, final String year, final String url,
            final Boolean optional) {

        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        setTitle(title);
        setAuthors(authors);
        setReference(reference);
        setYear(year);
        setOptional(optional);
        setUrl(url);
    }

    public void edit(final LocalizedString title, final String authors, final LocalizedString reference, final String year,
            final String url, final Boolean optional) {

//        if (title == null || authors == null || year == null || optional == null) {
//            throw new IllegalArgumentException (BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
//        }

        setLocalizedTitle(title);
        setAuthors(authors);
        setLocalizedReference(reference);
        setYear(year);
        setOptional(optional);
        setUrl(url);
    }

    public boolean isOptional() {
        return getOptional() != null && getOptional();
    }

    public BibliographicReferenceType getType() {
        return isOptional() ? BibliographicReferenceType.SECONDARY : BibliographicReferenceType.MAIN;
    }

    public void setType(BibliographicReferenceType type) {
        setOptional(BibliographicReferenceType.SECONDARY.equals(type));
    }

    public void delete() {
        setCompetenceCourseInformation(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Set<BibliographicReference> createDomainEntitiesFrom(BibliographicReferences bibliographicReferences) {
        return bibliographicReferences.getBibliographicReferencesList().stream().map(br -> {
            BibliographicReference bibliographicReference = BibliographicReference.create(br.getTitle(), br.getAuthors(),
                    br.getReference(), br.getYear(), BibliographicReferenceType.SECONDARY.equals(br.getType()));
            bibliographicReference.setUrl(br.getUrl());
            bibliographicReference.setReferenceOrder(br.getOrder());
            return bibliographicReference;
        }).collect(Collectors.toSet());
    }

    public static BibliographicReferences createValueTypeFrom(Set<BibliographicReference> bibliographies) {
        BibliographicReferences bibliographicReferences = new BibliographicReferences();
        for (final BibliographicReference reference : bibliographies.stream().sorted(BibliographicReference.COMPARATOR_BY_ORDER)
                .collect(Collectors.toList())) {
            bibliographicReferences =
                    bibliographicReferences.with(reference.getYear(), reference.getLocalizedTitle().getContent(),
                            reference.getAuthors(), reference.getLocalizedReference().getContent(), reference.getUrl(),
                            reference.isOptional() ? BibliographicReferenceType.SECONDARY : BibliographicReferenceType.MAIN,
                            reference.getReferenceOrder());
        }
        return bibliographicReferences;
    }

}
