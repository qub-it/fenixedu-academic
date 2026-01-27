package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;

import java.util.Locale;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ProgramConclusionProcess;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProgramConclusionConfigTest {

    private static final LocalizedString CONCLUSION_TITLE = new LocalizedString(Locale.getDefault(), "Title");

    private static ProgramConclusionConfig config;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static ProgramConclusion programConclusion;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            DegreeTest.initDegree();
            degreeCurricularPlan = new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), DCP_NAME_V1, AcademicPeriod.THREE_YEAR);
            programConclusion = new ProgramConclusion();
            programConclusion.setCode("PROGRAM_CONCLUSION_CODE");
            programConclusion.setName(new LocalizedString());

            return null;
        });
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getProgramConclusionConfigsSet().forEach(ProgramConclusionConfig::delete);
    }

    private ProgramConclusionConfig create(LocalizedString conclusionTitle, DegreeCurricularPlan degreeCurricularPlan,
            ProgramConclusion programConclusion) {
        return ProgramConclusionConfig.create(conclusionTitle, degreeCurricularPlan, programConclusion);
    }

    @Test
    public void testProgramConclusionConfig_createSuccess() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);

        assertNotEquals(null, config);
        assertEquals(CONCLUSION_TITLE, config.getConclusionTitle());
        assertEquals(degreeCurricularPlan, config.getDegreeCurricularPlan());
        assertEquals(programConclusion, config.getProgramConclusion());
    }


    @Test
    public void testProgramConclusionConfig_setConfigOrderThrowsDomainException() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        assertThrows(DomainException.class, () -> config.setConfigOrder(1));
    }

    @Test
    public void testProgramConclusionConfig_changeConfigOrder() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        int initialOrder = config.getConfigOrder();
        config.changeConfigOrder(2);
        assertEquals(2, config.getConfigOrder());
        assertNotEquals(initialOrder, config.getConfigOrder());
    }

    @Test
    public void testProgramConclusionConfig_addDegreeModulesSuccess() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        DegreeModule dm1 = new CourseGroup();
        DegreeModule dm2 = new CurricularCourse();
        config.addIncludedModules(dm1);
        config.addExcludedModules(dm2);
        assertEquals(1, config.getIncludedModulesSet().size());
        assertEquals(dm1, config.getIncludedModulesSet().iterator().next());
        assertEquals(1, config.getExcludedModulesSet().size());
        assertEquals(dm2, config.getExcludedModulesSet().iterator().next());
    }

    @Test
    public void testProgramConclusionConfig_addAlreadyIncludedModuleToExcludedModulesThrowsDomainException() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        DegreeModule dm1 = new CourseGroup();
        config.addIncludedModules(dm1);
        assertThrows(DomainException.class, () -> config.addExcludedModules(dm1));
    }

    @Test
    public void testProgramConclusionConfig_addAlreadyExcludedModuleToIncludedModulesThrowsDomainException() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        DegreeModule dm1 = new CourseGroup();
        config.addExcludedModules(dm1);
        assertThrows(DomainException.class, () -> config.addIncludedModules(dm1));
    }

    @Test
    public void testProgramConclusionConfig_editSuccess() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        assertEquals(CONCLUSION_TITLE, config.getConclusionTitle());
        assertEquals(programConclusion, config.getProgramConclusion());

        LocalizedString newTitle = new LocalizedString(Locale.getDefault(), "New Title");
        ProgramConclusion newProgramConclusion = new ProgramConclusion();

        config.edit(newTitle, newProgramConclusion);

        assertEquals(newTitle, config.getConclusionTitle());
        assertEquals(newProgramConclusion, config.getProgramConclusion());
    }

    @Test
    public void testProgramConclusionConfig_deleteSuccess() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        config.addIncludedModules(new CourseGroup());
        config.addExcludedModules(new CurricularCourse());

        assertEquals(false, Bennu.getInstance().getProgramConclusionConfigsSet().isEmpty());
        assertNotEquals(null, config.getRootDomainObject());
        assertEquals(degreeCurricularPlan, config.getDegreeCurricularPlan());
        assertEquals(programConclusion, config.getProgramConclusion());
        assertEquals(1, config.getIncludedModulesSet().size());
        assertEquals(1, config.getExcludedModulesSet().size());

        config.delete();
        assertEquals(true, Bennu.getInstance().getProgramConclusionConfigsSet().isEmpty());
        assertEquals(null, config.getRootDomainObject());
        assertEquals(null, config.getDegreeCurricularPlan());
        assertEquals(null, config.getProgramConclusion());
        assertEquals(0, config.getIncludedModulesSet().size());
        assertEquals(0, config.getExcludedModulesSet().size());
    }

    @Test
    public void testProgramConclusionConfig_moveUp() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        ProgramConclusionConfig config2 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());

        assertEquals(0, config.getConfigOrder());
        assertEquals(1, config2.getConfigOrder());

        config2.moveUp();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config.getConfigOrder());

        // Testing that moving up the first config doesn't change anything
        config2.moveUp();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config.getConfigOrder());
    }

    @Test
    public void testProgramConclusionConfig_moveDown() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        ProgramConclusionConfig config2 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());

        assertEquals(0, config.getConfigOrder());
        assertEquals(1, config2.getConfigOrder());

        config.moveDown();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config.getConfigOrder());

        // Testing that moving down the last config doesn't change anything
        config.moveDown();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config.getConfigOrder());
    }

    @Test
    public void testProgramConclusionConfig_moveTop() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);
        ProgramConclusionConfig config2 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());
        ProgramConclusionConfig config3 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());

        assertEquals(0, config.getConfigOrder());
        assertEquals(1, config2.getConfigOrder());
        assertEquals(2, config3.getConfigOrder());

        config3.moveTop();

        assertEquals(0, config3.getConfigOrder());
        assertEquals(1, config.getConfigOrder());
        assertEquals(2, config2.getConfigOrder());

        // Testing that moving top the first config doesn't change anything
        config3.moveTop();

        assertEquals(0, config3.getConfigOrder());
        assertEquals(1, config.getConfigOrder());
        assertEquals(2, config2.getConfigOrder());
    }

    @Test
    public void testProgramConclusionConfig_moveBottom() {
        config = create(CONCLUSION_TITLE, degreeCurricularPlan, programConclusion);

        ProgramConclusionConfig config2 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());
        ProgramConclusionConfig config3 = create(CONCLUSION_TITLE, degreeCurricularPlan, new ProgramConclusion());

        assertEquals(0, config.getConfigOrder());
        assertEquals(1, config2.getConfigOrder());
        assertEquals(2, config3.getConfigOrder());

        config.moveBottom();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config3.getConfigOrder());
        assertEquals(2, config.getConfigOrder());

        // Testing that moving bottom the last config doesn't change anything
        config.moveBottom();

        assertEquals(0, config2.getConfigOrder());
        assertEquals(1, config3.getConfigOrder());
        assertEquals(2, config.getConfigOrder());
    }
}
