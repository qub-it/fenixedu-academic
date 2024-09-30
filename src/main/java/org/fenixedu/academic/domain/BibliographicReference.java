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
        setLocalizedTitle(title);
    }

    @Deprecated
    public static BibliographicReference create(final String title, final String authors, final String reference,
            final String year, final Boolean optional) {

        final BibliographicReference result = new BibliographicReference();
        result.setTitle(title);
        result.setAuthors(authors);
        result.setReference(reference);
        result.setYear(year);
        result.setOptional(optional);

        return result;
    }

    public static BibliographicReference create(final LocalizedString title, final String authors,
            final LocalizedString reference, final String year, final String url, final Integer referenceOrder,
            final Boolean optional) {

        final BibliographicReference result = new BibliographicReference(title);
        result.setAuthors(authors);
        result.setLocalizedReference(reference);
        result.setYear(year);
        result.setUrl(url);
        result.setOptional(optional);
        result.setReferenceOrder(referenceOrder);

        return result;
    }

    @Deprecated
    @Override
    public void setTitle(String title) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }

        super.setTitle(title);
        super.setLocalizedTitle(StringUtils.isBlank(title) ? null : new LocalizedString(Locale.getDefault(), title));
    }

    @Deprecated
    @Override
    public void setReference(String reference) {
        super.setReference(reference);
        super.setLocalizedReference(StringUtils.isBlank(reference) ? null : new LocalizedString(Locale.getDefault(), reference));
    }

    @Override
    public void setLocalizedTitle(LocalizedString title) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.required.field.title.is.not.filled"));
        }
        super.setLocalizedTitle(title);
        super.setTitle(title.getContent(Locale.getDefault())); //FIXME: remove me
    }

    @Override
    public void setLocalizedReference(LocalizedString reference) { //FIXME: remove me
        super.setReference(reference == null ? null : reference.getContent(Locale.getDefault()));
        super.setLocalizedReference(reference);
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
        if (getLocalizedTitle() == null && this.getTitle() != null) { //FIXME: remove me
            copiedReference.setTitle(this.getTitle());
        }
        if (getLocalizedReference() == null && this.getReference() != null) { //FIXME: remove me
            copiedReference.setReference(this.getReference());
        }
        return copiedReference;
    }

    @Deprecated
    public void edit(final String title, final String authors, final String reference, final String year, final String url,
            final Boolean optional) {

        setTitle(title);
        setAuthors(authors);
        setReference(reference);
        setYear(year);
        setOptional(optional);
        setUrl(url);
    }

    public void edit(final LocalizedString title, final String authors, final LocalizedString reference, final String year,
            final String url, final Boolean optional) {

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

    public void delete() {
        setCompetenceCourseInformation(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

}
