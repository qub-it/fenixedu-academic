package org.fenixedu.academic.domain.curricularRules;

import java.util.Collection;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class AnyCurricularCourseExceptionsConfiguration extends AnyCurricularCourseExceptionsConfiguration_Base {

    protected AnyCurricularCourseExceptionsConfiguration() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    public static void init() {
        if (getInstance() != null) {
            return;
        }
        makeInstance();
    }

    private static AnyCurricularCourseExceptionsConfiguration instance;

    public static AnyCurricularCourseExceptionsConfiguration getInstance() {
        if (instance == null) {
            instance = Bennu.getInstance().getAnyCurricularCourseExceptionsConfiguration();
        }
        return instance;
    }

    @Atomic
    private static AnyCurricularCourseExceptionsConfiguration makeInstance() {
        return new AnyCurricularCourseExceptionsConfiguration();
    }

    @Atomic
    public void addCompetenceCourse(CompetenceCourse competenceCourse) {
        getCompetenceCoursesSet().add(competenceCourse);

    }

    @Atomic
    public void removeCompetenceCourse(CompetenceCourse competenceCourse) {
        getCompetenceCoursesSet().remove(competenceCourse);
    }

    @Atomic
    public void clearCompetenceCourses() {
        getCompetenceCoursesSet().clear();
    }

    @Atomic
    public void addAllCompetenceCourses(Collection<CompetenceCourse> competenceCourses) {
        getCompetenceCoursesSet().addAll(competenceCourses);
    }
    
}
