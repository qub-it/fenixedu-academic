package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_B_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.initCompetenceCourse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseTypeTest {
    private static final String CODE = org.fenixedu.academic.domain.CompetenceCourseType.DISSERTATION.name();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Dissertation");

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initCompetenceCourseType();
            initCompetenceCourse();
            return null;
        });
    }

    public static CompetenceCourseType initCompetenceCourseType() {
        org.fenixedu.academic.domain.CompetenceCourseType regular = org.fenixedu.academic.domain.CompetenceCourseType.REGULAR;
        String code = regular.name();
        LocalizedString name = new LocalizedString(Locale.getDefault(), code);

        return CompetenceCourseType.findByCode(code)
                .orElseGet(() -> CompetenceCourseType.create(code, name, regular.isFinalWork()));
    }

    private CompetenceCourseType create(String code, LocalizedString name, boolean finalWork) {
        return CompetenceCourseType.create(code, name, finalWork);
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getCompetenceCoursesSet().forEach(CompetenceCourse::delete);
        Bennu.getInstance().getCompetenceCourseTypesSet().forEach(CompetenceCourseType::delete);
    }

    @Test
    public void testCompetenceCourseType_create() {
        CompetenceCourseType competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);

        assertNotNull(competenceCourseTypeEntity);
        assertEquals(CODE, competenceCourseTypeEntity.getCode());
        assertEquals(QUALIFIED_NAME, competenceCourseTypeEntity.getName());
    }

    @Test
    public void testCompetenceCourseType_createDuplicate() {
        create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> CompetenceCourseType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testCompetenceCourseType_delete() {
        // Create a new CompetenceCourseType and its relations
        CompetenceCourseType competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);
        competenceCourse.setCompetenceCourseType(competenceCourseTypeEntity);

        // Verify initial state before deletion
        assertFalse(Bennu.getInstance().getCompetenceCourseTypesSet().isEmpty());
        assertNotEquals(null, competenceCourseTypeEntity.getRootDomainObject());
        assertTrue(competenceCourseTypeEntity.getCompetenceCoursesSet().contains(competenceCourse));

        // Perform deletion
        competenceCourseTypeEntity.getCompetenceCoursesSet().clear();
        competenceCourseTypeEntity.delete();

        // Verify deletion
        assertFalse(Bennu.getInstance().getCompetenceCourseTypesSet().contains(competenceCourseTypeEntity));
        assertNull(competenceCourseTypeEntity.getRootDomainObject());
        assertNull(competenceCourse.getCompetenceCourseType());
    }

    @Test
    public void testCompetenceCourseType_deleteFailsBecauseRelationsNotCleared() {
        CompetenceCourseType competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);
        competenceCourse.setCompetenceCourseType(competenceCourseTypeEntity);

        assertThrows(DomainException.class, competenceCourseTypeEntity::delete);
    }

    @Test
    public void testCompetenceCourseType_competenceCourseRelations() {
        CompetenceCourseType competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);
        competenceCourse.setCompetenceCourseType(competenceCourseTypeEntity);

        // Verify initial relations with CompetenceCourse
        assertTrue(competenceCourseTypeEntity.getCompetenceCoursesSet().contains(competenceCourse));

        // Delete CompetenceCourse
        competenceCourse.delete();

        // Verify that relations with CompetenceCourseType are removed
        assertTrue(competenceCourseTypeEntity.getCompetenceCoursesSet().isEmpty());
    }

    @Test
    public void testCompetenceCourseType_findByCode() {
        create(CODE, QUALIFIED_NAME, true);

        Optional<CompetenceCourseType> found = CompetenceCourseType.findByCode(CODE);
        assertTrue(found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testCompetenceCourseType_findByCodeNotFound() {
        Optional<CompetenceCourseType> found = CompetenceCourseType.findByCode("NON_EXISTENT_CODE");
        assertFalse(found.isPresent());
    }

    @Test
    public void testCompetenceCourseType_findAll() {
        create(CODE, QUALIFIED_NAME, true);

        assertEquals(2, CompetenceCourseType.findAll().count());
    }

    @Test
    public void testCompetenceCourseType_competenceCourseSettersAreSynced() {
        org.fenixedu.academic.domain.CompetenceCourseType _enum = org.fenixedu.academic.domain.CompetenceCourseType.REGULAR;
        CompetenceCourseType cct = CompetenceCourseType.findByCode(_enum.name()).orElseThrow();
        CompetenceCourse cc = CompetenceCourse.find(COURSE_B_CODE);
        cc.setType(null);

        // Assert initial state of CompetenceCourse
        assertNull(cc.getCompetenceCourseType());
        assertNull(cc.getType());

        // Call setter in CompetenceCourseType Enum to trigger the sync in CompetenceCourseType Entity
        cc.setType(_enum);

        // Assert CompetenceCourseType Enum and CompetenceCourseType Entity have the same value
        assertEquals(cct, cc.getCompetenceCourseType());
        assertEquals(cc.getType().name(), cc.getCompetenceCourseType().getCode());

        // Set to null with Enum Setter
        cc.setType(null);

        assertNull(cc.getType());
        assertNull(cc.getCompetenceCourseType());

        // Call setter in CompetenceCourseType to trigger the sync in CompetenceCourse
        cc.setCompetenceCourseType(cct);

        // Assert CompetenceCourseType Enum and CompetenceCourseType Entity have the same value
        assertEquals(cct, cc.getCompetenceCourseType());
        assertEquals(cc.getType().name(), cc.getCompetenceCourseType().getCode());

        // Set to null with CompetenceCourseType Setter
        cc.setCompetenceCourseType(null);

        assertNull(cc.getType());
        assertNull(cc.getCompetenceCourseType());
    }
}