package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CourseLoadDurationByTeachingMethodTest {

    private static final String F2F_METHOD = "F2F";
    private static final String ASYNC_METHOD = "Async";
    private static final String SYNC_METHOD = "Sync";

    private static Unit coursesUnit;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            CompetenceCourseTest.initCompetenceCourse();
            initTeachingMethodTypes();
            coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
            return null;
        });
    }

    private static void initTeachingMethodTypes() {
        if (TeachingMethodType.findAll().findAny().isEmpty()) {
            final Locale locale = Locale.getDefault();
            TeachingMethodType.create(F2F_METHOD, new LocalizedString(locale, "Face-to-face"),
                    new LocalizedString(locale, "F2F"));
            TeachingMethodType.create(ASYNC_METHOD, new LocalizedString(locale, "Asynchronous"),
                    new LocalizedString(locale, "Async"));
            TeachingMethodType.create(SYNC_METHOD, new LocalizedString(locale, "Synchronous"),
                    new LocalizedString(locale, "Sync"));
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void tearDown() {
        Bennu.getInstance().getCourseLoadDurationsByTeachingMethodSet().forEach(CourseLoadDurationByTeachingMethod::delete);
    }

    @Test
    public void testDuplicateTeachingMethods() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CourseLoadDurationByTeachingMethod.create.alreadyExistsForTeachingMethodType");

        final CompetenceCourse competenceCourse = createCompetenceCourse();
        final CompetenceCourseInformation competenceCourseInformation = competenceCourse.findInformationMostRecentUntil(null);
        final CourseLoadType theoreticalType = CourseLoadType.of(CourseLoadType.THEORETICAL);
        competenceCourseInformation.setLoadHours(theoreticalType, new BigDecimal(30));

        final TeachingMethodType faceToFaceMethod = TeachingMethodType.findByCode(F2F_METHOD).orElseThrow();

        final CourseLoadDuration courseLoadDuration =
                competenceCourseInformation.findLoadDurationByType(theoreticalType).orElseThrow();

        CourseLoadDurationByTeachingMethod.create(courseLoadDuration, faceToFaceMethod, new BigDecimal(10));
        CourseLoadDurationByTeachingMethod.create(courseLoadDuration, faceToFaceMethod, new BigDecimal(10));
    }

    @Test
    public void testTeachingMethodDurations() {
        final CompetenceCourse competenceCourse = createCompetenceCourse();
        final CompetenceCourseInformation competenceCourseInformation = competenceCourse.findInformationMostRecentUntil(null);
        final CourseLoadType theoreticalType = CourseLoadType.of(CourseLoadType.THEORETICAL);
        competenceCourseInformation.setLoadHours(theoreticalType, new BigDecimal(30));

        final TeachingMethodType faceToFaceMethod = TeachingMethodType.findByCode(F2F_METHOD).orElseThrow();
        final TeachingMethodType asyncMethod = TeachingMethodType.findByCode(ASYNC_METHOD).orElseThrow();

        final CourseLoadDuration courseLoadDuration =
                competenceCourseInformation.findLoadDurationByType(theoreticalType).orElseThrow();

        CourseLoadDurationByTeachingMethod.create(courseLoadDuration, faceToFaceMethod, new BigDecimal(30));

        Optional<CourseLoadDurationByTeachingMethod> faceToFaceDuration =
                courseLoadDuration.findLoadDurationByTeachingMethod(faceToFaceMethod);
        Optional<CourseLoadDurationByTeachingMethod> asyncFaceDuration =
                courseLoadDuration.findLoadDurationByTeachingMethod(asyncMethod);

        assertTrue(faceToFaceDuration.isPresent());
        assertEquals(faceToFaceDuration.get().getHours(), new BigDecimal(30));

        assertTrue(asyncFaceDuration.isEmpty());
    }

    @Test
    public void testTeachingMethodDurationsViaCompetenceCourseInformation() {
        final CompetenceCourse competenceCourse = createCompetenceCourse();
        final CompetenceCourseInformation competenceCourseInformation = competenceCourse.findInformationMostRecentUntil(null);
        final CourseLoadType theoreticalType = CourseLoadType.of(CourseLoadType.THEORETICAL);
        competenceCourseInformation.setLoadHours(theoreticalType, new BigDecimal(30));

        final TeachingMethodType faceToFaceMethod = TeachingMethodType.findByCode(F2F_METHOD).orElseThrow();
        final TeachingMethodType asyncMethod = TeachingMethodType.findByCode(ASYNC_METHOD).orElseThrow();

        competenceCourseInformation.setLoadHours(theoreticalType, faceToFaceMethod, new BigDecimal(20));

        final Optional<BigDecimal> theoreticalFaceToFaceLoadHours =
                competenceCourseInformation.getLoadHours(theoreticalType, faceToFaceMethod);

        final Optional<BigDecimal> theoreticalAsyncLoadHours =
                competenceCourseInformation.getLoadHours(theoreticalType, asyncMethod);

        final Optional<BigDecimal> practicalFaceToFaceLoadHours = competenceCourseInformation
                .getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), faceToFaceMethod);

        assertTrue(theoreticalFaceToFaceLoadHours.isPresent());
        assertEquals(theoreticalFaceToFaceLoadHours.get(), new BigDecimal(20));
        assertTrue(theoreticalAsyncLoadHours.isEmpty());
        assertTrue(practicalFaceToFaceLoadHours.isEmpty());

        competenceCourseInformation.setLoadHours(theoreticalType, faceToFaceMethod, null);
        final Optional<BigDecimal> theoreticalFaceToFaceLoadHoursEmpty =
                competenceCourseInformation.getLoadHours(theoreticalType, faceToFaceMethod);
        assertTrue(theoreticalFaceToFaceLoadHoursEmpty.isEmpty());
    }

    @Test
    public void testTeachingMethodInvalidDuration() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CourseLoadDurationByTeachingMethod.hours.greaterThanLoadDuration");

        final CompetenceCourse competenceCourse = createCompetenceCourse();
        final CompetenceCourseInformation competenceCourseInformation = competenceCourse.findInformationMostRecentUntil(null);
        final CourseLoadType theoreticalType = CourseLoadType.of(CourseLoadType.THEORETICAL);
        competenceCourseInformation.setLoadHours(theoreticalType, new BigDecimal(30));

        final TeachingMethodType faceToFaceMethod = TeachingMethodType.findByCode(F2F_METHOD).orElseThrow();
        final TeachingMethodType asyncMethod = TeachingMethodType.findByCode(ASYNC_METHOD).orElseThrow();

        final CourseLoadDuration courseLoadDuration =
                competenceCourseInformation.findLoadDurationByType(theoreticalType).orElseThrow();

        CourseLoadDurationByTeachingMethod.create(courseLoadDuration, faceToFaceMethod, new BigDecimal(10));
        final var asyncFaceDuration =
                CourseLoadDurationByTeachingMethod.create(courseLoadDuration, asyncMethod, new BigDecimal(20));

        asyncFaceDuration.setHours(new BigDecimal(20.1));
    }

    private static CompetenceCourse createCompetenceCourse() {
        final String code = UUID.randomUUID().toString();
        return CompetenceCourseTest.createCompetenceCourse(code, code, new BigDecimal(0), AcademicPeriod.SEMESTER,
                ExecutionInterval.findFirstCurrentChild(null), coursesUnit);
    }

}
