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
package org.fenixedu.academic.domain.schedule.lesson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Joao Carvalho (joao.pedro.carvalho@ist.utl.pt)
 *
 */
public class LessonPeriodCurricularYears {

    private final List<Integer> curricularYears;

    public LessonPeriodCurricularYears(Collection<Integer> curricularYears) {
        super();
        if (curricularYears == null) {
            throw new IllegalArgumentException("exception.null.values");
        }
        this.curricularYears = curricularYears.isEmpty() ? List.of(-1) : new ArrayList<>(curricularYears);
    }

    @Override
    public String toString() {
        return curricularYears.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static LessonPeriodCurricularYears internalize(String data) {
        final List<Integer> years = Stream.of(data.split(",")).map(Integer::parseInt).collect(Collectors.toList());
        return new LessonPeriodCurricularYears(years);
    }

    public List<Integer> getYears() {
        return curricularYears;
    }

    /*
     * The value -1 in the year list represents all the years of the selected degree
     */
    public boolean hasAll() {
        return curricularYears.contains(-1);
    }

}
