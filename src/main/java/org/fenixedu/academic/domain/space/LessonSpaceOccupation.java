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
package org.fenixedu.academic.domain.space;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

public class LessonSpaceOccupation extends LessonSpaceOccupation_Base {

    public LessonSpaceOccupation(Space allocatableSpace, Lesson lesson) {
        super();

        if (lesson != null && lesson.getLessonSpaceOccupation() != null) {
            throw new DomainException("error.lesson.already.has.lessonSpaceOccupation");
        }

        setLesson(lesson);

        edit(allocatableSpace);
    }

    public void edit(Space allocatableSpace) {
        if (getPeriod() == null) {
            throw new DomainException("error.LessonSpaceOccupation.empty.period");
        }

        if (allocatableSpace != null && !allocatableSpace.isFree(getIntervals())) {
            throw new DomainException("error.LessonSpaceOccupation.room.is.not.free", allocatableSpace.getName(),
                    getPeriod().getStartYearMonthDay().toString("dd-MM-yyy"),
                    getPeriod().getLastOccupationPeriodOfNestedPeriods().getEndYearMonthDay().toString("dd-MM-yyy"));
        }

        setResource(allocatableSpace);
    }

    @Override
    public void delete() {
        super.setLesson(null);
        super.delete();
    }

    private OccupationPeriod getPeriod() {
        return getLesson() == null ? null : getLesson().getPeriod();
    }

    @Override
    public List<Interval> getIntervals() {
        List<Interval> result = new ArrayList<>();
        if (!getLesson().wasFinished()) {
            final HourMinuteSecond b = getLesson().getBeginHourMinuteSecond();
            final HourMinuteSecond e = getLesson().getEndHourMinuteSecond();
            for (final YearMonthDay yearMonthDay : getLesson().getAllLessonDatesWithoutInstanceDates()) {
                result.add(new Interval(yearMonthDay.toLocalDate().toDateTime(b.toLocalTime()),
                        yearMonthDay.toLocalDate().toDateTime(e.toLocalTime())));
            }
        }
        return result;
    }

    @Override
    public void setLesson(Lesson lesson) {
        if (lesson == null) {
            throw new DomainException("error.LessonSpaceOccupation.empty.lesson");
        }
        super.setLesson(lesson);
    }

    @Override
    public String getUrl() {
        Lesson li = getLesson();
        if (li == null) {
            return "";
        }

        return li.getShift().getExecutionCourse().getSiteUrl();
    }

    @Override
    public String getInfo() {
        String asd = getLesson().getShift().getCourseLoadType().getName().getContent();
        for (YearMonthDay ymd : getLesson().getAllLessonDates()) {
            asd += "\n" + ymd.toString();
        }
        return asd;
    }

    @Override
    public String getSubject() {
        return getLesson().getShift().getExecutionCourse().getSigla();
    }

    @Override
    public String getType() {
        return BundleUtil.getString(Bundle.APPLICATION, "label.lesson");
    }

    @Override
    public DateTime getStart() {
        return getIntervals().stream().map(Interval::getStart).min(Comparator.naturalOrder()).orElse(null);
    }

    @Override
    public DateTime getEnd() {
        return getIntervals().stream().map(Interval::getEnd).max(Comparator.naturalOrder()).orElse(null);
    }

}
