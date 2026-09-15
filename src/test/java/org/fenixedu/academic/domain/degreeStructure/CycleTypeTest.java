package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
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
    public void testCycleType_GetSortedValues() {
        List<CycleType> sorted = new ArrayList<>(CycleType.getSortedValues());
        assertEquals(EXPECTED_SORTED_BY_WEIGHT, sorted);
        List<Integer> weights = sorted.stream().map(CycleType::getWeight).toList();
        assertEquals(weights.stream().sorted().toList(), weights);
    }

    @Test
    public void testCycleType_ComparatorByLessWeight() {
        List<CycleType> sorted = Arrays.stream(CycleType.values())
                .sorted(CycleType.COMPARATOR_BY_LESS_WEIGHT)
                .toList();
        assertEquals(EXPECTED_SORTED_BY_WEIGHT, sorted);
    }

    @Test
    public void testCycleType_ComparatorByGreaterWeight() {
        List<CycleType> reversed = Arrays.stream(CycleType.values()).sorted(CycleType.COMPARATOR_BY_GREATER_WEIGHT.reversed())
                .toList();
        assertEquals(EXPECTED_SORTED_BY_WEIGHT, reversed);
    }

    @Test
    public void testCycleType_GetNextAndGetPrevious() {
        List<CycleType> sorted = List.copyOf(CycleType.getSortedValues());

        assertNull(sorted.get(0).getPrevious());
        assertNull(sorted.get(sorted.size() - 1).getNext());

        for (int i = 0; i < sorted.size() - 1; i++) {
            assertEquals(sorted.get(i + 1), sorted.get(i).getNext());
            assertEquals(sorted.get(i), sorted.get(i + 1).getPrevious());
        }
    }
}
