package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class CycleTypeTest {

    private static final List<CycleType> EXPECTED_SORTED_BY_WEIGHT =
            Arrays.stream(CycleType.values())
                    .sorted(CycleType.COMPARATOR_BY_LESS_WEIGHT)
                    .toList();

    @Test
    public void testGetSortedValues() {
        assertEquals(EXPECTED_SORTED_BY_WEIGHT, List.copyOf(CycleType.getSortedValues()));
    }

    @Test
    public void testComparatorByLessWeight() {
        List<CycleType> sorted = Arrays.stream(CycleType.values())
                .sorted(CycleType.COMPARATOR_BY_LESS_WEIGHT)
                .toList();
        assertEquals(EXPECTED_SORTED_BY_WEIGHT, sorted);
    }

    @Test
    public void testComparatorByGreaterWeight() {
        List<CycleType> reversed = Arrays.stream(CycleType.values())
                .sorted(CycleType.COMPARATOR_BY_GREATER_WEIGHT)
                .toList();

        List<CycleType> expectedReversed = new ArrayList<>(EXPECTED_SORTED_BY_WEIGHT);
        Collections.reverse(expectedReversed);

        assertEquals(expectedReversed, reversed);
    }

    @Test
    public void testGetSortedValues_returnsAllValues() {
        List<CycleType> sorted = new ArrayList<>(CycleType.getSortedValues());
        assertEquals(CycleType.values().length, sorted.size());
        for (CycleType ct : CycleType.values()) {
            assertTrue(sorted.contains(ct), ct + " should be in sorted values");
        }
    }

    @Test
    public void testGetSortedValues_isSorted() {
        List<CycleType> sorted = new ArrayList<>(CycleType.getSortedValues());
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getWeight() <= sorted.get(i + 1).getWeight());
        }
    }

    @Test
    public void testGetNextAndGetPrevious() {
        List<CycleType> sorted = List.copyOf(CycleType.getSortedValues());

        assertNull(sorted.get(0).getPrevious());
        assertNull(sorted.get(sorted.size() - 1).getNext());

        for (int i = 0; i < sorted.size() - 1; i++) {
            assertEquals(sorted.get(i + 1), sorted.get(i).getNext());
            assertEquals(sorted.get(i), sorted.get(i + 1).getPrevious());
        }
    }
}
