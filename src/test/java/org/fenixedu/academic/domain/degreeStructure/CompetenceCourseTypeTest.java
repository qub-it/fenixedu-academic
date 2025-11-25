package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.initCompetenceCourse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    // Example competenceCourseTypeEnum, can be any valid value
    private static final org.fenixedu.academic.domain.CompetenceCourseType competenceCourseTypeEnum =
            org.fenixedu.academic.domain.CompetenceCourseType.REGULAR;
    private static final String CODE = competenceCourseTypeEnum.name();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Regular");
    private static CompetenceCourseType competenceCourseTypeEntity;
    private static CompetenceCourse competenceCourse;

    @Before
    public void init() {
        // Instantiate the dependencies before each test (@BeforeAll doesn't work here)
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initCompetenceCourse();
            competenceCourse = CompetenceCourse.find(COURSE_A_CODE);

            return null;
        });
    }

    private CompetenceCourseType create(String code, LocalizedString name, boolean finalWork) {
        return FenixFramework.getTransactionManager().withTransaction(() -> CompetenceCourseType.create(code, name, finalWork));
    }

    private void initializeCompetenceCourseTypeRelations() {
        // Initialize relations for the created CompetenceCourseType
        FenixFramework.getTransactionManager().withTransaction(() -> {
            competenceCourse.setCompetenceCourseType(competenceCourseTypeEntity);
            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            competenceCourseTypeEntity.getCompetenceCoursesSet().clear();
            Bennu.getInstance().getCompetenceCourseTypesSet().forEach(CompetenceCourseType::delete);
            Bennu.getInstance().getCompetenceCoursesSet().forEach(CompetenceCourse::delete);
            return null;
        });
    }

    @Test
    public void testCompetenceCourseType_create() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);

        assertNotNull(competenceCourseTypeEntity);
        assertEquals(CODE, competenceCourseTypeEntity.getCode());
        assertEquals(QUALIFIED_NAME, competenceCourseTypeEntity.getName());
    }

    @Test
    public void testCompetenceCourseType_createDuplicate() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> CompetenceCourseType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testCompetenceCourseType_delete() {
        // Tests deletion of CompetenceCourseType and its relations

        // Create a new CompetenceCourseType and its relations
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        initializeCompetenceCourseTypeRelations();

        // Verify initial state before deletion
        assertEquals(false, Bennu.getInstance().getCompetenceCourseTypesSet().isEmpty());
        assertNotEquals(null, competenceCourseTypeEntity.getRootDomainObject());
        assertEquals(true, competenceCourseTypeEntity.getCompetenceCoursesSet().contains(competenceCourse));

        // Perform deletion
        competenceCourseTypeEntity.getCompetenceCoursesSet().clear();
        competenceCourseTypeEntity.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getCompetenceCourseTypesSet().isEmpty());
        assertEquals(null, competenceCourseTypeEntity.getRootDomainObject());
        assertEquals(null, competenceCourse.getCompetenceCourseType());
    }

    @Test
    public void testCompetenceCourseType_deleteFailsBecauseRelationsNotCleared() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        initializeCompetenceCourseTypeRelations();

        assertThrows(DomainException.class, () -> competenceCourseTypeEntity.delete());
    }

    @Test
    public void testCompetenceCourseType_competenceCourseRelations() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);
        initializeCompetenceCourseTypeRelations();

        // Verify initial relations with CompetenceCourse
        assertEquals(true, competenceCourseTypeEntity.getCompetenceCoursesSet().contains(competenceCourse));

        // Delete CompetenceCourse
        competenceCourse.delete();

        // Verify that relations with CompetenceCourseType are removed
        assertEquals(true, competenceCourseTypeEntity.getCompetenceCoursesSet().isEmpty());
    }

    @Test
    public void testCompetenceCourseType_findByCode() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);

        Optional<CompetenceCourseType> found = CompetenceCourseType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testCompetenceCourseType_findByCodeNotFound() {
        Optional<CompetenceCourseType> found = CompetenceCourseType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testCompetenceCourseType_findAll() {
        competenceCourseTypeEntity = create(CODE, QUALIFIED_NAME, true);

        CompetenceCourseType c = create("exampleCode", QUALIFIED_NAME, true);
        assertEquals(2, CompetenceCourseType.findAll().count());
        c.delete();
    }
}