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
/*
 * Created on 21/Jul/2003
 *
 * 
 */
package org.fenixedu.academic.domain;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.TimeOfDay;
import org.joda.time.YearMonthDay;

public class Summary extends Summary_Base {

    public static final String CREATE_SIGNAL = "academic.summary.create.signal";
    public static final String EDIT_SIGNAL = "academic.summary.edit.signal";

    public Summary(LocalizedString title, LocalizedString summaryText, Integer studentsNumber, Professorship professorship,
            String teacherName, Lesson lesson, LocalDate date) {
        super();
        setRootDomainObject(Bennu.getInstance());

        YearMonthDay dateYMD = new YearMonthDay(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());

        final Shift shift = lesson.getShift();
        final ExecutionCourse executionCourse = shift.getExecutionCourse();

        setShift(shift);
        setSummaryDateYearMonthDay(dateYMD);
        setExecutionCourse(executionCourse);
        setTitle(title);
        setSummaryText(summaryText);

        checkSpecialParameters(professorship, teacherName, lesson);
        checkIfSummaryDateIsValid(dateYMD, executionCourse.getExecutionInterval(), lesson);

        setStudentsNumber(studentsNumber);
        setProfessorship(professorship);
        setTeacherName(teacherName);
        setLastModifiedDateDateTime(new DateTime());
        setTaught(Boolean.TRUE);

        setSummaryHourHourMinuteSecond(lesson.getBeginHourMinuteSecond());
        lessonInstanceManagement(lesson, dateYMD);
        if (getLessonInstance() == null) {
            throw new DomainException("error.Summary.empty.LessonInstances");
        }

        ContentManagementLog.createLog(executionCourse, Bundle.MESSAGING, "log.executionCourse.content.summary.added",
                title.getContent(), shift.getPresentationName(), executionCourse.getNome(),
                executionCourse.getDegreePresentationString());

        Signal.emit(CREATE_SIGNAL, new DomainObjectEvent<Summary>(this));
    }

    @Deprecated
    public Summary(LocalizedString title, LocalizedString summaryText, Integer studentsNumber, Boolean isExtraLesson,
            Professorship professorship, String teacherName, Teacher teacher, Shift shift, Lesson lesson, YearMonthDay date,
            Space room, Partial hour, ShiftType type, Boolean taught) {
        this(title, summaryText, studentsNumber, professorship, teacherName, lesson,
                new LocalDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkRequiredParameters() {
        return getTitle() != null && !getTitle().isEmpty() && getSummaryText() != null && !getSummaryText().isEmpty()
                && getSummaryDateYearMonthDay() != null && getSummaryHourHourMinuteSecond() != null;
    }

    private void lessonInstanceManagement(Lesson lesson, YearMonthDay day) {
        LessonInstance lessonInstance = lesson.getLessonInstanceFor(day);

        if (lessonInstance == null) {
            lesson.createAllLessonInstances();

            lessonInstance = lesson.getLessonInstanceFor(day);
            if (lessonInstance == null) {
                throw new DomainException("error.summary.no.valid.date.to.lesson");
            }
        }

        lessonInstance.setSummary(this);
    }

    private void checkIfSummaryDateIsValid(YearMonthDay date, ExecutionInterval executionSemester, Lesson lesson) {
        if (lesson.getAssociatedSummaries().stream().filter(s -> s != this)
                .anyMatch(s -> s.getSummaryDateYearMonthDay().isEqual(date))) {
            throw new DomainException("error.summary.already.exists");
        }

        if (!lesson.isTimeValidToInsertSummary(new HourMinuteSecond(), date)) {
            throw new DomainException("error.summary.no.valid.time.to.lesson");
        }
    }

    private void checkSpecialParameters(Professorship professorship, String teacherName, Lesson lesson) {
        if (professorship == null && StringUtils.isEmpty(teacherName)) {
            throw new DomainException("error.summary.no.teacher");
        }

        if (lesson == null) {
            throw new DomainException("error.summary.no.lesson");
        }
    }

    public void delete() {
        ContentManagementLog.createLog(getShift().getExecutionCourse(), Bundle.MESSAGING,
                "log.executionCourse.content.summary.removed", getTitle().getContent(), getShift().getPresentationName(),
                getShift().getExecutionCourse().getNome(), getShift().getExecutionCourse().getDegreePresentationString());

        super.setExecutionCourse(null);
        super.setShift(null);
        super.setLessonInstance(null);
        setRoom(null);
        setProfessorship(null);
        setTeacher(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public Lesson getLesson() {
        return getLessonInstance() != null ? getLessonInstance().getLesson() : null;
    }

    @Override
    public void setSummaryHourHourMinuteSecond(HourMinuteSecond summaryHourHourMinuteSecond) {
        if (summaryHourHourMinuteSecond == null) {
            throw new DomainException("error.Summary.empty.time");
        }
        super.setSummaryHourHourMinuteSecond(summaryHourHourMinuteSecond);
    }

    @Override
    public void setSummaryDateYearMonthDay(YearMonthDay summaryDateYearMonthDay) {
        if (summaryDateYearMonthDay == null) {
            throw new DomainException("error.summary.no.date");
        }
        super.setSummaryDateYearMonthDay(summaryDateYearMonthDay);
    }

    @Override
    public void setTitle(LocalizedString title) {
        if (title == null || title.getLocales().isEmpty()) {
            throw new DomainException("error.summary.no.title");
        }
        super.setTitle(title);
    }

    @Override
    public void setSummaryText(LocalizedString summaryText) {
        if (summaryText == null || summaryText.getLocales().isEmpty()) {
            throw new DomainException("error.summary.no.summaryText");
        }
        super.setSummaryText(summaryText);
    }

    @Override
    public void setLessonInstance(LessonInstance lessonInstance) {
        if (lessonInstance == null) {
            throw new DomainException("error.Summary.empty.lessonInstance");
        }
        super.setLessonInstance(lessonInstance);
    }

    public void moveFromTeacherToProfessorship(Professorship professorship) {
        if (getTeacher() != null && professorship != null && professorship.getExecutionCourse().equals(getExecutionCourse())
                && professorship.getTeacher().equals(getTeacher())) {

            setTeacher(null);
            setProfessorship(professorship);
        }
    }

    public DateTime getSummaryDateTime() {
        HourMinuteSecond time = getSummaryHourHourMinuteSecond();
        return getSummaryDateYearMonthDay()
                .toDateTime(new TimeOfDay(time.getHour(), time.getMinuteOfHour(), time.getSecondOfMinute(), 0));
    }

}
