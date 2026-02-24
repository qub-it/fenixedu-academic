package org.fenixedu.academic.domain.curricularPeriod;

import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.dto.CurricularPeriodInfoDTO;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.*;
import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class CurricularPeriodTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            return null;
        });
    }

    @Test
    public void testConstructor() {
        CurricularPeriod curricularPeriod = new CurricularPeriod(YEAR);
        assertNotNull(curricularPeriod);
        assertEquals(YEAR, curricularPeriod.getAcademicPeriod());
        assertNull(curricularPeriod.getParent());
        assertNull(curricularPeriod.getChildOrder());
    }

    @Test
    public void testConstructorWithParent() {
        CurricularPeriod parent = new CurricularPeriod(YEAR);
        CurricularPeriod child = new CurricularPeriod(SEMESTER, 1, parent);
        assertNotNull(child);
        assertEquals(SEMESTER, child.getAcademicPeriod());
        assertEquals(parent, child.getParent());
        assertEquals(Integer.valueOf(1), child.getChildOrder());
        assertTrue(parent.getChildsSet().contains(child));
    }

    @Test
    public void testGetSortedChilds() {
        CurricularPeriod parent = new CurricularPeriod(YEAR);
        CurricularPeriod child2 = new CurricularPeriod(SEMESTER, 2, parent);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, parent);
        CurricularPeriod child3 = new CurricularPeriod(TRIMESTER, 1, child1);

        List<CurricularPeriod> sortedChilds = parent.getSortedChilds();
        assertEquals(2, sortedChilds.size());
        assertEquals(child1, sortedChilds.get(0));
        assertEquals(child2, sortedChilds.get(1));

        List<CurricularPeriod> child1SortedChilds = child1.getSortedChilds();
        assertEquals(1, child1SortedChilds.size());
        assertEquals(child3, child1SortedChilds.get(0));
    }

    @Test
    public void testFindChild() {
        CurricularPeriod parent = new CurricularPeriod(YEAR);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, parent);
        CurricularPeriod child2 = new CurricularPeriod(SEMESTER, 2, parent);

        assertTrue(parent.findChild(SEMESTER, 2).isPresent());
        assertEquals(child2, parent.findChild(SEMESTER, 2).get());
        assertTrue(parent.findChild(SEMESTER, 3).isEmpty());
        assertTrue(parent.findChild(TRIMESTER, 1).isEmpty());
    }

    @Test
    public void testGetCurricularPeriod() {
        CurricularPeriod root = new CurricularPeriod(YEAR);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, root);
        CurricularPeriod child2 = new CurricularPeriod(TRIMESTER, 2, child1);

        CurricularPeriodInfoDTO path1 = new CurricularPeriodInfoDTO(1, SEMESTER);
        CurricularPeriodInfoDTO path2 = new CurricularPeriodInfoDTO(2, TRIMESTER);

        assertEquals(child1, root.getCurricularPeriod(path1));
        assertEquals(child2, root.getCurricularPeriod(path1, path2));
        assertNull(root.getCurricularPeriod(new CurricularPeriodInfoDTO(2, SEMESTER)));
    }

    @Test(expected = DomainException.class)
    public void testGetCurricularPeriodWithInvalidPath() {
        CurricularPeriod root = new CurricularPeriod(YEAR);
        CurricularPeriodInfoDTO path1 = new CurricularPeriodInfoDTO(1, SEMESTER);
        CurricularPeriodInfoDTO path2 = new CurricularPeriodInfoDTO(2, SEMESTER);
        root.getCurricularPeriod(path1, path2);
    }

    @Test
    public void testGetOrderByType() {
        CurricularPeriod root = new CurricularPeriod(YEAR);
        root.setChildOrder(1);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 2, root);
        CurricularPeriod child2 = new CurricularPeriod(TRIMESTER, 3, child1);

        assertEquals(Integer.valueOf(1), child2.getOrderByType(YEAR));
        assertEquals(Integer.valueOf(2), child2.getOrderByType(SEMESTER));
        assertEquals(Integer.valueOf(3), child2.getOrderByType(TRIMESTER));
    }

    @Test
    public void testDelete() {
        CurricularPeriod parent = new CurricularPeriod(YEAR);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, parent);
        CurricularPeriod child2 = new CurricularPeriod(SEMESTER, 2, parent);
        CurricularPeriod grandchild = new CurricularPeriod(TRIMESTER, 1, child1);

        child1.delete();

        assertFalse(parent.getChildsSet().contains(child1));
        assertEquals(1, parent.getChildsSet().size());
        assertEquals(Integer.valueOf(1), child2.getChildOrder());

        // System.out.println(">>> grandchild valid: " + FenixFramework.isDomainObjectValid(grandchild));
        // assertFalse(FenixFramework.isDomainObjectValid(grandchild));
    }

//    @Test
//    public void testCompareTo() {
//            CurricularPeriod p1 = new CurricularPeriod(YEAR);
//            p1.setChildOrder(1);
//            CurricularPeriod p2 = new CurricularPeriod(YEAR);
//            p2.setChildOrder(2);
//            CurricularPeriod p3 = new CurricularPeriod(SEMESTER, 1, p1);
//            CurricularPeriod p4 = new CurricularPeriod(SEMESTER, 2, p1);
//            CurricularPeriod p5 = new CurricularPeriod(SEMESTER, 1, p3);
//
//            assertTrue(p1.compareTo(p2) < 0);
//            assertTrue(p2.compareTo(p1) > 0);
//            assertTrue(p1.compareTo(p3) > 0);
//            assertTrue(p3.compareTo(p4) < 0);
//            assertTrue(p3.compareTo(p5) > 0);
//    }

    @Test
    public void testGetNext() {
        CurricularPeriod parent = new CurricularPeriod(YEAR);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, parent);
        CurricularPeriod child2 = new CurricularPeriod(SEMESTER, 2, parent);

        assertNull(parent.getNext());
        assertEquals(child2, child1.getNext());
        assertNull(child2.getNext());
    }

    @Test
    public void testContains() {
        CurricularPeriod root = new CurricularPeriod(YEAR);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 1, root);
        CurricularPeriod child2 = new CurricularPeriod(TRIMESTER, 1, child1);

        assertNotNull(root.contains(SEMESTER, 1));
        assertEquals(child1, root.contains(SEMESTER, 1));
        assertNotNull(root.contains(TRIMESTER, 1));
        assertEquals(child2, root.contains(TRIMESTER, 1));
        assertNull(root.contains(SEMESTER, 2));
    }

    @Test
    public void testHasCurricularPeriod() {
        CurricularPeriod root = new CurricularPeriod(YEAR);
        root.setChildOrder(1);
        CurricularPeriod child1 = new CurricularPeriod(SEMESTER, 2, root);
        CurricularPeriod child2 = new CurricularPeriod(TRIMESTER, 3, child1);

        assertTrue(child2.hasCurricularPeriod(YEAR, 1));
        assertTrue(child2.hasCurricularPeriod(SEMESTER, 2));
        assertFalse(child2.hasCurricularPeriod(SEMESTER, 1));
        assertTrue(child2.hasCurricularPeriod(TRIMESTER, 3));
        assertFalse(child2.hasCurricularPeriod(YEAR, 2));
    }

    @Test
    public void testGetAbsoluteOrderOfChild() {
        CurricularPeriod root = new CurricularPeriod(THREE_YEAR);
        CurricularPeriod year1 = new CurricularPeriod(YEAR, 1, root);
        CurricularPeriod year2 = new CurricularPeriod(YEAR, 2, root);
        CurricularPeriod year3 = new CurricularPeriod(YEAR, 3, root);

        CurricularPeriod year1sem1 = new CurricularPeriod(SEMESTER, 1, year1);
        CurricularPeriod year1sem2 = new CurricularPeriod(SEMESTER, 2, year1);
        CurricularPeriod year1trim1 = new CurricularPeriod(TRIMESTER, 1, year1);
        CurricularPeriod year1trim2 = new CurricularPeriod(TRIMESTER, 2, year1);
        CurricularPeriod year1trim3 = new CurricularPeriod(TRIMESTER, 3, year1);
        CurricularPeriod year1trim4 = new CurricularPeriod(TRIMESTER, 4, year1);

        CurricularPeriod year2sem1 = new CurricularPeriod(SEMESTER, 1, year2);
        CurricularPeriod year2sem2 = new CurricularPeriod(SEMESTER, 2, year2);
        CurricularPeriod year2trim1 = new CurricularPeriod(TRIMESTER, 1, year2);
        CurricularPeriod year2trim2 = new CurricularPeriod(TRIMESTER, 2, year2);
        CurricularPeriod year2trim3 = new CurricularPeriod(TRIMESTER, 3, year2);
        CurricularPeriod year2trim4 = new CurricularPeriod(TRIMESTER, 4, year2);

        CurricularPeriod year3sem1 = new CurricularPeriod(SEMESTER, 1, year3);
        CurricularPeriod year3sem2 = new CurricularPeriod(SEMESTER, 2, year3);
        CurricularPeriod year3trim1 = new CurricularPeriod(TRIMESTER, 1, year3);
        CurricularPeriod year3trim2 = new CurricularPeriod(TRIMESTER, 2, year3);
        CurricularPeriod year3trim3 = new CurricularPeriod(TRIMESTER, 3, year3);
        CurricularPeriod year3trim4 = new CurricularPeriod(TRIMESTER, 4, year3);

        assertEquals(1, root.getAbsoluteOrderOfChild()); // 1

        assertEquals(1, year1.getAbsoluteOrderOfChild()); // (1-1) * 3 + 1
        assertEquals(2, year2.getAbsoluteOrderOfChild()); // (1-1) * 3 + 2
        assertEquals(3, year3.getAbsoluteOrderOfChild()); // (1-1) * 3 + 3

        assertEquals(1, year1sem1.getAbsoluteOrderOfChild()); // (1-1) * 6 + 1
        assertEquals(2, year1sem2.getAbsoluteOrderOfChild()); // (1-1) * 6 + 2
        assertEquals(1, year1trim1.getAbsoluteOrderOfChild()); // (1-1) * 6 + 1
        assertEquals(2, year1trim2.getAbsoluteOrderOfChild()); // (1-1) * 6 + 2
        assertEquals(3, year1trim3.getAbsoluteOrderOfChild()); // (1-1) * 6 + 3
        assertEquals(4, year1trim4.getAbsoluteOrderOfChild()); // (1-1) * 6 + 4

        assertEquals(7, year2sem1.getAbsoluteOrderOfChild()); // (2-1) * 6 + 1
        assertEquals(8, year2sem2.getAbsoluteOrderOfChild()); // (2-1) * 6 + 2
        assertEquals(7, year2trim1.getAbsoluteOrderOfChild()); // (2-1) * 6 + 1
        assertEquals(8, year2trim2.getAbsoluteOrderOfChild()); // (2-1) * 6 + 2
        assertEquals(9, year2trim3.getAbsoluteOrderOfChild()); // (2-1) * 6 + 3
        assertEquals(10, year2trim4.getAbsoluteOrderOfChild()); // (2-1) * 6 + 4

        assertEquals(13, year3sem1.getAbsoluteOrderOfChild()); // (3-1) * 6 + 1
        assertEquals(14, year3sem2.getAbsoluteOrderOfChild()); // (3-1) * 6 + 2
        assertEquals(13, year3trim1.getAbsoluteOrderOfChild()); // (3-1) * 6 + 1
        assertEquals(14, year3trim2.getAbsoluteOrderOfChild()); // (3-1) * 6 + 2
        assertEquals(15, year3trim3.getAbsoluteOrderOfChild()); // (3-1) * 6 + 3
        assertEquals(16, year3trim4.getAbsoluteOrderOfChild()); // (3-1) * 6 + 4

    }

    @Test
    public void testGetWeight() {
        CurricularPeriod root = new CurricularPeriod(THREE_YEAR);
        CurricularPeriod year1 = new CurricularPeriod(YEAR, 1, root);
        CurricularPeriod year2 = new CurricularPeriod(YEAR, 2, root);
        CurricularPeriod year3 = new CurricularPeriod(YEAR, 3, root);

        CurricularPeriod year1sem1 = new CurricularPeriod(SEMESTER, 1, year1);
        CurricularPeriod year1sem2 = new CurricularPeriod(SEMESTER, 2, year1);
        CurricularPeriod year1trim1 = new CurricularPeriod(TRIMESTER, 1, year1);
        CurricularPeriod year1trim2 = new CurricularPeriod(TRIMESTER, 2, year1);
        CurricularPeriod year1trim3 = new CurricularPeriod(TRIMESTER, 3, year1);
        CurricularPeriod year1trim4 = new CurricularPeriod(TRIMESTER, 4, year1);

        CurricularPeriod year2sem1 = new CurricularPeriod(SEMESTER, 1, year2);
        CurricularPeriod year2sem2 = new CurricularPeriod(SEMESTER, 2, year2);
        CurricularPeriod year2trim1 = new CurricularPeriod(TRIMESTER, 1, year2);
        CurricularPeriod year2trim2 = new CurricularPeriod(TRIMESTER, 2, year2);
        CurricularPeriod year2trim3 = new CurricularPeriod(TRIMESTER, 3, year2);
        CurricularPeriod year2trim4 = new CurricularPeriod(TRIMESTER, 4, year2);

        CurricularPeriod year3sem1 = new CurricularPeriod(SEMESTER, 1, year3);
        CurricularPeriod year3sem2 = new CurricularPeriod(SEMESTER, 2, year3);
        CurricularPeriod year3trim1 = new CurricularPeriod(TRIMESTER, 1, year3);
        CurricularPeriod year3trim2 = new CurricularPeriod(TRIMESTER, 2, year3);
        CurricularPeriod year3trim3 = new CurricularPeriod(TRIMESTER, 3, year3);
        CurricularPeriod year3trim4 = new CurricularPeriod(TRIMESTER, 4, year3);

        // root
        assertEquals(0f, root.getWeight(), 0.001);

        // years
        assertEquals(1f, year1.getWeight(), 0.001);
        assertEquals(2f, year2.getWeight(), 0.001);
        assertEquals(3f, year3.getWeight(), 0.001);

        // year 1, semesters
        assertEquals(0.5f, year1sem1.getWeight(), 0.001);
        assertEquals(1f, year1sem2.getWeight(), 0.001);

        // year 1, trimesters
        assertEquals(0.25f, year1trim1.getWeight(), 0.001);
        assertEquals(0.5, year1trim2.getWeight(), 0.001);
        assertEquals(0.75, year1trim3.getWeight(), 0.001);
        assertEquals(1f, year1trim4.getWeight(), 0.001);

        // year 2, semesters
        assertEquals(0.5f, year2sem1.getWeight(), 0.001);
        assertEquals(1f, year2sem2.getWeight(), 0.001);

        // year 2, trimesters
        assertEquals(0.25f, year2trim1.getWeight(), 0.001);
        assertEquals(0.5, year2trim2.getWeight(), 0.001);
        assertEquals(0.75, year2trim3.getWeight(), 0.001);
        assertEquals(1f, year2trim4.getWeight(), 0.001);

        // year 3, semesters
        assertEquals(0.5f, year3sem1.getWeight(), 0.001);
        assertEquals(1f, year3sem2.getWeight(), 0.001);

        // year 3, trimesters
        assertEquals(0.25f, year3trim1.getWeight(), 0.001);
        assertEquals(0.5, year3trim2.getWeight(), 0.001);
        assertEquals(0.75, year3trim3.getWeight(), 0.001);
        assertEquals(1f, year3trim4.getWeight(), 0.001);

    }

    @Test
    public void testGetFullWeight() {
        CurricularPeriod root = new CurricularPeriod(THREE_YEAR);
        CurricularPeriod year1 = new CurricularPeriod(YEAR, 1, root);
        CurricularPeriod year2 = new CurricularPeriod(YEAR, 2, root);
        CurricularPeriod year3 = new CurricularPeriod(YEAR, 3, root);

        CurricularPeriod year1sem1 = new CurricularPeriod(SEMESTER, 1, year1);
        CurricularPeriod year1sem2 = new CurricularPeriod(SEMESTER, 2, year1);
        CurricularPeriod year1trim1 = new CurricularPeriod(TRIMESTER, 1, year1);
        CurricularPeriod year1trim2 = new CurricularPeriod(TRIMESTER, 2, year1);
        CurricularPeriod year1trim3 = new CurricularPeriod(TRIMESTER, 3, year1);
        CurricularPeriod year1trim4 = new CurricularPeriod(TRIMESTER, 4, year1);

        CurricularPeriod year2sem1 = new CurricularPeriod(SEMESTER, 1, year2);
        CurricularPeriod year2sem2 = new CurricularPeriod(SEMESTER, 2, year2);
        CurricularPeriod year2trim1 = new CurricularPeriod(TRIMESTER, 1, year2);
        CurricularPeriod year2trim2 = new CurricularPeriod(TRIMESTER, 2, year2);
        CurricularPeriod year2trim3 = new CurricularPeriod(TRIMESTER, 3, year2);
        CurricularPeriod year2trim4 = new CurricularPeriod(TRIMESTER, 4, year2);

        CurricularPeriod year3sem1 = new CurricularPeriod(SEMESTER, 1, year3);
        CurricularPeriod year3sem2 = new CurricularPeriod(SEMESTER, 2, year3);
        CurricularPeriod year3trim1 = new CurricularPeriod(TRIMESTER, 1, year3);
        CurricularPeriod year3trim2 = new CurricularPeriod(TRIMESTER, 2, year3);
        CurricularPeriod year3trim3 = new CurricularPeriod(TRIMESTER, 3, year3);
        CurricularPeriod year3trim4 = new CurricularPeriod(TRIMESTER, 4, year3);

        // root
        assertEquals(0f, root.getFullWeight(), 0.001);

        // years
        assertEquals(1f, year1.getFullWeight(), 0.001);
        assertEquals(2f, year2.getFullWeight(), 0.001);
        assertEquals(3f, year3.getFullWeight(), 0.001);

        // year 1, semesters
        assertEquals(1.5f, year1sem1.getFullWeight(), 0.001);
        assertEquals(2f, year1sem2.getFullWeight(), 0.001);

        // year 1, trimesters
        assertEquals(1.25f, year1trim1.getFullWeight(), 0.001);
        assertEquals(1.5, year1trim2.getFullWeight(), 0.001);
        assertEquals(1.75, year1trim3.getFullWeight(), 0.001);
        assertEquals(2f, year1trim4.getFullWeight(), 0.001);

        // year 2, semesters
        assertEquals(2.5f, year2sem1.getFullWeight(), 0.001);
        assertEquals(3f, year2sem2.getFullWeight(), 0.001);

        // year 2, trimesters
        assertEquals(2.25f, year2trim1.getFullWeight(), 0.001);
        assertEquals(2.5, year2trim2.getFullWeight(), 0.001);
        assertEquals(2.75, year2trim3.getFullWeight(), 0.001);
        assertEquals(3f, year2trim4.getFullWeight(), 0.001);

        // year 3, semesters
        assertEquals(3.5f, year3sem1.getFullWeight(), 0.001);
        assertEquals(4f, year3sem2.getFullWeight(), 0.001);

        // year 3, trimesters
        assertEquals(3.25f, year3trim1.getFullWeight(), 0.001);
        assertEquals(3.5, year3trim2.getFullWeight(), 0.001);
        assertEquals(3.75, year3trim3.getFullWeight(), 0.001);
        assertEquals(4f, year3trim4.getFullWeight(), 0.001);

    }

    @Test
    public void testCompareTo() {
        CurricularPeriod root = new CurricularPeriod(THREE_YEAR);
        CurricularPeriod year1 = new CurricularPeriod(YEAR, 1, root);
        CurricularPeriod year2 = new CurricularPeriod(YEAR, 2, root);
        CurricularPeriod year3 = new CurricularPeriod(YEAR, 3, root);

        CurricularPeriod year1sem1 = new CurricularPeriod(SEMESTER, 1, year1);
        CurricularPeriod year1sem2 = new CurricularPeriod(SEMESTER, 2, year1);
        CurricularPeriod year1trim1 = new CurricularPeriod(TRIMESTER, 1, year1);
        CurricularPeriod year1trim2 = new CurricularPeriod(TRIMESTER, 2, year1);
        CurricularPeriod year1trim3 = new CurricularPeriod(TRIMESTER, 3, year1);
        CurricularPeriod year1trim4 = new CurricularPeriod(TRIMESTER, 4, year1);

        CurricularPeriod year2sem1 = new CurricularPeriod(SEMESTER, 1, year2);
        CurricularPeriod year2sem2 = new CurricularPeriod(SEMESTER, 2, year2);
        CurricularPeriod year2trim1 = new CurricularPeriod(TRIMESTER, 1, year2);
        CurricularPeriod year2trim2 = new CurricularPeriod(TRIMESTER, 2, year2);
        CurricularPeriod year2trim3 = new CurricularPeriod(TRIMESTER, 3, year2);
        CurricularPeriod year2trim4 = new CurricularPeriod(TRIMESTER, 4, year2);

        CurricularPeriod year3sem1 = new CurricularPeriod(SEMESTER, 1, year3);
        CurricularPeriod year3sem2 = new CurricularPeriod(SEMESTER, 2, year3);
        CurricularPeriod year3trim1 = new CurricularPeriod(TRIMESTER, 1, year3);
        CurricularPeriod year3trim2 = new CurricularPeriod(TRIMESTER, 2, year3);
        CurricularPeriod year3trim3 = new CurricularPeriod(TRIMESTER, 3, year3);
        CurricularPeriod year3trim4 = new CurricularPeriod(TRIMESTER, 4, year3);

        List<CurricularPeriod> allPeriods = new ArrayList<>();
        allPeriods.add(year3trim4);
        allPeriods.add(year2sem1);
        allPeriods.add(year1trim2);
        allPeriods.add(root);
        allPeriods.add(year1);
        allPeriods.add(year3sem1);
        allPeriods.add(year2trim3);
        allPeriods.add(year1sem2);
        allPeriods.add(year2);
        allPeriods.add(year3trim1);
        allPeriods.add(year1trim4);
        allPeriods.add(year3);
        allPeriods.add(year2trim1);
        allPeriods.add(year1sem1);
        allPeriods.add(year2sem2);
        allPeriods.add(year3trim2);
        allPeriods.add(year1trim3);
        allPeriods.add(year3sem2);
        allPeriods.add(year2trim2);
        allPeriods.add(year3trim3);
        allPeriods.add(year1trim1);
        allPeriods.add(year2trim4);

        allPeriods.sort(Comparator.naturalOrder());

//        System.out.println("**** Sorted Curricular Periods: ****");
//        allPeriods.forEach(cp -> System.out.println(">>> " + cp.getFullLabel() + " - " + cp.getAcademicPeriod()
//                .getName() + " - W:" + cp.getWeight() + " - FW:" + cp.getFullWeight()));

        assertEquals(allPeriods.get(0), root);

        assertEquals(allPeriods.get(1), year1);
        assertEquals(allPeriods.get(2), year2);
        assertEquals(allPeriods.get(3), year3);

        assertEquals(allPeriods.get(4), year1trim1);
        assertEquals(allPeriods.get(5), year1trim2);
        assertEquals(allPeriods.get(6), year1trim3);
        assertEquals(allPeriods.get(7), year1trim4);
        assertEquals(allPeriods.get(8), year1sem1);
        assertEquals(allPeriods.get(9), year1sem2);

        assertEquals(allPeriods.get(10), year2trim1);
        assertEquals(allPeriods.get(11), year2trim2);
        assertEquals(allPeriods.get(12), year2trim3);
        assertEquals(allPeriods.get(13), year2trim4);
        assertEquals(allPeriods.get(14), year2sem1);
        assertEquals(allPeriods.get(15), year2sem2);

        assertEquals(allPeriods.get(16), year3trim1);
        assertEquals(allPeriods.get(17), year3trim2);
        assertEquals(allPeriods.get(18), year3trim3);
        assertEquals(allPeriods.get(19), year3trim4);
        assertEquals(allPeriods.get(20), year3sem1);
        assertEquals(allPeriods.get(21), year3sem2);

    }

}
