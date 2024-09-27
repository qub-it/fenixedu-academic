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
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class BibliographicReference extends BibliographicReference_Base {

    public static final Comparator<BibliographicReference> COMPARATOR_BY_ORDER =
            Comparator.comparing(BibliographicReference::getReferenceOrder).thenComparing(BibliographicReference::getTitle)
                    .thenComparing(BibliographicReference::getYear).thenComparing(BibliographicReference::getExternalId);

    private BibliographicReference() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public BibliographicReference(final LocalizedString title) {
        this();
        validateTitleNotEmpty(title); //TODO: move this to the setLocalizedTitle 
        setLocalizedTitle(title);
    }

    @Deprecated
    public static BibliographicReference create(final String title, final String authors, final String reference,
            final String year, final Boolean optional) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        final BibliographicReference result = new BibliographicReference();
        result.setTitle(title);
        result.setLocalizedTitle(new LocalizedString(Locale.getDefault(), title));
        result.setAuthors(authors);
        result.setReference(reference);
        result.setLocalizedReference(new LocalizedString(Locale.getDefault(), reference));
        result.setYear(year);
        result.setOptional(optional);

        return result;
    }

    public static BibliographicReference create(final LocalizedString title, final String authors,
            final LocalizedString reference, final String year, final String url, final Integer referenceOrder,
            final Boolean optional) {

        validateTitleNotEmpty(title); //TODO: move this to the setLocalizedTitle 

        final BibliographicReference result = new BibliographicReference();
        result.setLocalizedTitle(title);
        result.setAuthors(authors);
        result.setLocalizedReference(reference);
        result.setYear(year);
        result.setUrl(url);
        result.setOptional(optional);
        result.setReferenceOrder(referenceOrder);

        result.setTitle(title.getContent(Locale.getDefault())); //FIXME: remove me
        result.setReference(reference.getContent(Locale.getDefault())); //FIXME: remove me

        return result;
    }

    private static void validateTitleNotEmpty(final LocalizedString title) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }
    }

    /**
     * Copies the provided Bibliographic Reference.
     * Note that this method won't copy the relationships of the object, only the fields.
     * 
     * @return copied BibliographicReference
     */
    public BibliographicReference copy() {
        final Function<LocalizedString, LocalizedString> copy = ls -> ls == null ? null : ls.builder().build();

        final BibliographicReference copiedReference =
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
        setLocalizedTitle(new LocalizedString(Locale.getDefault(), title));
        setAuthors(authors);
        setReference(reference);
        setLocalizedReference(new LocalizedString(Locale.getDefault(), reference));
        setYear(year);
        setOptional(optional);
        setUrl(url);
    }

    public void edit(final LocalizedString title, final String authors, final LocalizedString reference, final String year,
            final String url, final Boolean optional) {

        validateTitleNotEmpty(title); //TODO: move this to the setLocalizedTitle 

        setLocalizedTitle(title);
        setAuthors(authors);
        setLocalizedReference(reference);
        setYear(year);
        setOptional(optional);
        setUrl(url);

        setTitle(title.getContent(Locale.getDefault())); //FIXME: remove me
        setReference(reference.getContent(Locale.getDefault())); //FIXME: remove me
    }

    public boolean isOptional() {
        return getOptional() != null && getOptional();
    }

    public void delete() {
        setCompetenceCourseInformation(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

}
