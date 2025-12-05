package org.fenixedu.academic.domain.degreeStructure;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class CompetenceCourseType extends CompetenceCourseType_Base {

    public static final String REGULAR = "REGULAR";
    public static final String DISSERTATION = "DISSERTATION";
    public static final String PROJECT_WORK = "PROJECT_WORK";
    public static final String INTERNSHIP = "INTERNSHIP";
    
    protected CompetenceCourseType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static CompetenceCourseType create(String code, LocalizedString name, boolean finalWork) {
        CompetenceCourseType competenceCourseType = new CompetenceCourseType();
        competenceCourseType.setCode(code);
        competenceCourseType.setName(name);
        competenceCourseType.setFinalWork(finalWork);

        return competenceCourseType;
    }

    public void delete() {
        if (!getCompetenceCoursesSet().isEmpty()) {
            throw new DomainException("error.CompetenceCourseType.cannot.delete.related.to.CompetenceCourse");
        }

        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public void setCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new DomainException("error.CompetenceCourseType.code.cannot.be.empty");
        }

        if (isDuplicateCode(code)) {
            throw new DomainException("error.CompetenceCourseType.code.already.exists", code);
        }

        super.setCode(code);
    }

    private boolean isDuplicateCode(String code) {
        return findByCode(code).filter(t -> t != this).isPresent();
    }

    public static Optional<CompetenceCourseType> findByCode(String code) {
        return findAll().filter(t -> code != null && code.equals(t.getCode())).findFirst();
    }

    public static Stream<CompetenceCourseType> findAll() {
        return Bennu.getInstance().getCompetenceCourseTypesSet().stream();
    }

    public static Stream<CompetenceCourseType> findFinalWorks() {
        return findAll().filter(CompetenceCourseType::getFinalWork);
    }
}
