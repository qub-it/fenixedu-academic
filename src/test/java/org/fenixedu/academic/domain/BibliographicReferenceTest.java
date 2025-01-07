package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class BibliographicReferenceTest {

    @Test
    public void testBibliographicReference_create() {
        final CompetenceCourseInformation cci = new CompetenceCourseInformation();
        assertTrue(cci.getBibliographiesSet().isEmpty());

        final BibliographicReference br1 = createFilledBibliographicReference();

        assertTrue(cci.getBibliographiesSet().isEmpty());
        br1.setCompetenceCourseInformation(cci);
        assertTrue(cci.getBibliographiesSet().size() == 1);

        final BibliographicReference br2 = createMostNullsBibliographicReference();
        br2.setCompetenceCourseInformation(cci);
        assertTrue(cci.getBibliographiesSet().size() == 2);
    }

    private static BibliographicReference createFilledBibliographicReference() {
        return BibliographicReference.create(
                new LocalizedString(Locale.getDefault(), "Workplace Etiquette").with(Locale.GERMAN, "Etikette am Arbeitsplatz"),
                "Junior", new LocalizedString(Locale.getDefault(), "For the People"), "2004", "www.url.com", 1, false);
    }

    private static BibliographicReference createMostNullsBibliographicReference() {
        return BibliographicReference.create(new LocalizedString(Locale.getDefault(), "Discovering Safety, Joseph Ph., 2000"),
                null, null, null, null, null, null);
    }

    @Test
    public void testBibliographicReference_createExceptions() {
        assertThrows(IllegalArgumentException.class,
                () -> BibliographicReference.create(null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> BibliographicReference.create(new LocalizedString(), null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> BibliographicReference
                .create(new LocalizedString(Locale.getDefault(), ""), null, null, null, null, null, null));
    }

    @Test
    public void testBibliographicReference_edit() {
        final BibliographicReference br1 = createFilledBibliographicReference();
        final BibliographicReference br2 = createMostNullsBibliographicReference();

        final String referenceDefault = "Referência 1";
        final String titleDefault = "Title 1"; //default Locale in test env is ENGLISH

        br1.edit(new LocalizedString(Locale.getDefault(), titleDefault).with(Locale.GERMAN, "Titel 1"), "Senior",
                new LocalizedString(Locale.getDefault(), referenceDefault).with(Locale.GERMAN, "Referenz 1"), "2000",
                "www.url.pt", false);
        assertEquals(br1.getTitle(), titleDefault);
        assertTrue(new LocalizedString(Locale.getDefault(), titleDefault).with(Locale.GERMAN, "Titel 1")
                .equals(br1.getLocalizedTitle()));

        assertEquals(br1.getReference(), referenceDefault);
        assertTrue(new LocalizedString(Locale.getDefault(), referenceDefault).with(Locale.GERMAN, "Referenz 1")
                .equals(br1.getLocalizedReference()));

        assertEquals(br1.getAuthors(), "Senior");
        assertEquals(br1.getYear(), "2000");
        assertEquals(br1.getUrl(), "www.url.pt");
        assertTrue(br1.getOptional() == false);

        assertTrue(br1.getReferenceOrder() == 1);

        br2.edit(new LocalizedString(Locale.GERMAN, "Sicherheit entdecken (Band 2), Joseph Ph., 2000"), null, null, null, null,
                null);
        assertEquals(br2.getTitle(), br2.getLocalizedTitle().getContent());
        assertTrue(new LocalizedString(Locale.GERMAN, "Sicherheit entdecken (Band 2), Joseph Ph., 2000")
                .equals(br2.getLocalizedTitle()));

        assertNull(br2.getReference());
        assertNull(br2.getLocalizedReference());

        assertNull(br2.getAuthors());
        assertNull(br2.getYear());
        assertNull(br2.getUrl());
        assertNull(br2.getOptional());

        assertNull(br2.getReferenceOrder()); //doesn't reorder
    }

    @Test
    public void testBibliographicReference_editExceptions() {
        final BibliographicReference br1 = createFilledBibliographicReference();
        final CompetenceCourseInformation cci = new CompetenceCourseInformation();
        assertThrows(IllegalArgumentException.class, () -> br1.edit((LocalizedString) null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> br1.edit(new LocalizedString(), null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> br1.edit(new LocalizedString(Locale.getDefault(), ""), null, null, null, null, null));

        assertThrows(IllegalArgumentException.class, () -> br1.setLocalizedTitle(null));
        assertThrows(IllegalArgumentException.class, () -> br1.setLocalizedTitle(new LocalizedString()));
        assertThrows(IllegalArgumentException.class, () -> br1.setLocalizedTitle(new LocalizedString(Locale.getDefault(), "")));
    }

    @Test
    public void testBibliographicReference_copy() {
        final CompetenceCourseInformation cci = new CompetenceCourseInformation();
        final BibliographicReference br1 = createFilledBibliographicReference();
        final BibliographicReference br2 = createMostNullsBibliographicReference();
        cci.getBibliographiesSet().addAll(Set.of(br1, br2));

        assertTrue(cci.getBibliographiesSet().size() == 2);

        final BibliographicReference copyBr1 = br1.copy();
        equals(br1, copyBr1);
        assertNull(copyBr1.getCompetenceCourseInformation());

        final BibliographicReference copyBr2 = br2.copy();
        equals(br2, copyBr2);
        assertNull(copyBr2.getCompetenceCourseInformation());

        assertTrue(cci.getBibliographiesSet().size() == 2);

        copyBr2.setLocalizedReference(new LocalizedString(Locale.getDefault(), "abc"));
        assertNotEquals(br2.getLocalizedReference() == null ? null : br2.getLocalizedReference().json().toString(),
                copyBr2.getLocalizedReference() == null ? null : copyBr2.getLocalizedReference().json().toString());

        copyBr2.setAuthors(".hP phesoJ");
        assertNotEquals(br2.getAuthors(), copyBr2.getAuthors());

        cci.delete();
    }

    private static void equals(BibliographicReference a, BibliographicReference b) {
        assertEquals(a.getTitle(), b.getTitle());
        assertEquals(a.getTitle(), b.getLocalizedTitle().getContent(Locale.getDefault()));
        assertEquals(a.getLocalizedTitle().getContent(Locale.getDefault()), b.getTitle());
        assertEquals(a.getLocalizedTitle().json().toString(), b.getLocalizedTitle().json().toString());

        final Function<LocalizedString, String> getContent =
                ls -> b.getLocalizedReference() == null ? null : b.getLocalizedReference().getContent(Locale.getDefault());
        assertEquals(a.getReference(), b.getReference());
        assertEquals(a.getReference(), getContent.apply(b.getLocalizedReference()));
        assertEquals(getContent.apply(a.getLocalizedReference()), b.getReference());
        assertEquals(a.getLocalizedReference() == null ? null : a.getLocalizedReference().json().toString(),
                b.getLocalizedReference() == null ? null : b.getLocalizedReference().json().toString());

        assertEquals(a.getAuthors(), b.getAuthors());
        assertEquals(a.getYear(), b.getYear());
        assertEquals(a.getUrl(), b.getUrl());
        assertEquals(a.getReferenceOrder(), b.getReferenceOrder());
        assertEquals(a.getOptional(), b.getOptional());
    }

    @Test
    public void testBibliographicReference_delete() {
        final BibliographicReference br2 = createMostNullsBibliographicReference();
        br2.delete();

        final CompetenceCourseInformation cci = new CompetenceCourseInformation();
        final BibliographicReference br1 = createFilledBibliographicReference();
        br1.setCompetenceCourseInformation(cci);
        br1.delete();

        final BibliographicReference br3 = createMostNullsBibliographicReference();
        br3.setCompetenceCourseInformation(cci);
        br3.delete();
        cci.delete();
    }

}
