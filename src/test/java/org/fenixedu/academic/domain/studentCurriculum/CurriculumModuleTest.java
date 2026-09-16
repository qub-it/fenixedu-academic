package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurriculumModuleTest {

    private static CurriculumGroup groupA;
    private static CurriculumGroup groupA2;
    private static CurriculumGroup groupB;
    private static CurriculumGroup groupC;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            UserUtil.initAdminUser();
            StudentTest.initRegistrationConfigEntities();

            final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
            final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

            final DegreeCurricularPlan dcp = createDcp(executionYear);
            final Student student = StudentTest.createStudent("Curriculum Module Test Student",
                    "curriculum.module.test.student." + UUID.randomUUID());
            final Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
            final RootCurriculumGroup root = registration.getLastStudentCurricularPlan().getRoot();

            // Group Structure:
            //
            // DCP root / SCP root
            //  └── A  (groupA)
            //      └── B  (groupB)
            //          ├── A  (groupA2)
            //          └── C  (groupC)
            final CourseGroup courseGroupA = new CourseGroup(dcp.getRoot(), "A", "A", executionInterval, null);
            final CourseGroup courseGroupB = new CourseGroup(courseGroupA, "B", "B", executionInterval, null);
            final CourseGroup courseGroupC = new CourseGroup(courseGroupB, "C", "C", executionInterval, null);
            final CourseGroup courseGroupA2 = new CourseGroup(courseGroupB, "A", "A", executionInterval, null);

            groupA = new CurriculumGroup(root, courseGroupA);
            groupB = new CurriculumGroup(groupA, courseGroupB);
            groupA2 = new CurriculumGroup(groupB, courseGroupA2);
            groupC = new CurriculumGroup(groupB, courseGroupC);

            return null;
        });
    }

    @Test
    public void testCurriculumModule_ComparatorByNameAndId() {
        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupA2, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_NAME_AND_ID);

        assertEquals("A", modules.get(0).getName().getContent());
        assertEquals("A", modules.get(1).getName().getContent());
        assertEquals("B", modules.get(2).getName().getContent());
        assertEquals("C", modules.get(3).getName().getContent());

        CurriculumModule first = modules.get(0);
        CurriculumModule second = modules.get(1);
        assertTrue((first == groupA && second == groupA2) || (first == groupA2 && second == groupA));
        assertTrue(first.getExternalId().compareTo(second.getExternalId()) < 0);
    }

    @Test
    public void testCurriculumModule_ComparatorByFullPathNameAndId() {
        assertEquals(groupA2.getFullPath(), groupB.getFullPath() + " > A");
        assertEquals(groupC.getFullPath(), groupB.getFullPath() + " > C");

        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupA2, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_FULL_PATH_NAME_AND_ID);
        assertEquals(groupA, modules.get(0));
        assertEquals(groupB, modules.get(1));
        assertEquals(groupA2, modules.get(2));
        assertEquals(groupC, modules.get(3));
    }

    @Test
    public void testCurriculumModule_ComparatorByCreationDate() {
        groupA.setCreationDateDateTime(new DateTime(2020, 1, 1, 0, 0, 0));
        groupB.setCreationDateDateTime(new DateTime(2021, 6, 15, 12, 0, 0));
        groupC.setCreationDateDateTime(new DateTime(2022, 12, 31, 23, 59, 59));

        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_CREATION_DATE);
        assertEquals(groupA, modules.get(0));
        assertEquals(groupB, modules.get(1));
        assertEquals(groupC, modules.get(2));
    }

    private static DegreeCurricularPlan createDcp(final ExecutionYear executionYear) {
        final DegreeType degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());
        degreeType.setCode("D" + UUID.randomUUID());

        final Degree degree = DegreeTest.createDegree(degreeType, "D" + UUID.randomUUID(), "Curriculum Module Test Degree",
                executionYear);
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan("Curriculum Module Test Degree Plan",
                User.findByUsername(UserUtil.ADMIN_USERNAME).getPerson(), AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }
}