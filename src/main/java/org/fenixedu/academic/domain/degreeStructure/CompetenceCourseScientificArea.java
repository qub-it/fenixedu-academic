package org.fenixedu.academic.domain.degreeStructure;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CompetenceCourseScientificArea extends CompetenceCourseScientificArea_Base {

    protected CompetenceCourseScientificArea() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static CompetenceCourseScientificArea create(final CompetenceCourseInformation competenceCourseInformation,
            final Unit scientificAreaUnit) {

        if (!scientificAreaUnit.isScientificAreaUnit()) {
            throw new DomainException("error.CompetenceCourseScientificArea.scientificAreaUnit.mustBeScientificAreaUnit");
        }

        if (find(competenceCourseInformation, scientificAreaUnit).isPresent()) {
            throw new DomainException(
                    "error.CompetenceCourseScientificArea.already.exists.forCompetenceCourseInformationAndScientificAreaUnit",
                    competenceCourseInformation.getName(), scientificAreaUnit.getName());
        }

        final CompetenceCourseScientificArea competenceCourseScientificArea = new CompetenceCourseScientificArea();
        competenceCourseScientificArea.setCompetenceCourseInformation(competenceCourseInformation);
        competenceCourseScientificArea.setScientificAreaUnit(scientificAreaUnit);
        return competenceCourseScientificArea;
    }

    public static Stream<CompetenceCourseScientificArea> findAll() {
        return Bennu.getInstance().getCompetenceCourseScientificAreasSet().stream();
    }

    public static Optional<CompetenceCourseScientificArea> find(final CompetenceCourseInformation competenceCourseInformation,
            final Unit scientificAreaUnit) {
        return competenceCourseInformation.getCompetenceCourseScientificAreasSet().stream()
                .filter(scientificArea -> scientificArea.getScientificAreaUnit() == scientificAreaUnit).findFirst();
    }

    @Override
    public void setCredits(BigDecimal credits) {
        super.setCredits(credits);
        if (credits != null) {
            checkScientificAreasCreditsRules(getCompetenceCourseInformation());
        }
    }

    public static void checkScientificAreasCreditsRules(final CompetenceCourseInformation competenceCourseInformation) {
        final BigDecimal courseCredits = Optional.ofNullable(competenceCourseInformation.getCredits()).orElse(BigDecimal.ZERO);

        final BigDecimal scientificAreasCredits = competenceCourseInformation.getCompetenceCourseScientificAreasSet().stream()
                .map(CompetenceCourseScientificArea::getCredits).filter(Objects::nonNull).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (courseCredits.compareTo(scientificAreasCredits) < 0) {
            throw new DomainException("error.CompetenceCourseScientificArea.credits.exceedingTotalCourseCredits",
                    courseCredits.toString(), scientificAreasCredits.toString());
        }
    }

    public void delete() {
        setCompetenceCourseInformation(null);
        setScientificAreaUnit(null);
        setRoot(null);
        super.deleteDomainObject();
    }
}
