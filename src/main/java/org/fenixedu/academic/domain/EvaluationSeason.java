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
package org.fenixedu.academic.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;

public class EvaluationSeason extends EvaluationSeason_Base implements Comparable<EvaluationSeason> {

    public EvaluationSeason() {
        super();
        setEvaluationConfiguration(EvaluationConfiguration.getInstance());
    }

    @Deprecated(forRemoval = true)
    public EvaluationSeason(LocalizedString acronym, LocalizedString name, boolean normal, boolean improvement,
            boolean specialAuthorization, boolean special) {
        this();
        setAcronym(acronym);
        setName(name);
        setNormal(normal);
        setImprovement(improvement);
        setSpecialAuthorization(specialAuthorization);
        setSpecial(special);
    }

    public static EvaluationSeason create(final String code, final LocalizedString acronym, final LocalizedString name,
            final boolean normal, final boolean improvement, final boolean special, final boolean specialAuthorization,
            final boolean active, final boolean requiresEnrolmentEvaluation, final boolean supportsEmptyGrades,
            final boolean supportsTeacherConfirmation, final boolean allowsMarksheets) {
        EvaluationSeason evaluationSeason = new EvaluationSeason();
        evaluationSeason.edit(code, acronym, name, normal, improvement, special, specialAuthorization, getNextSeasonOrder(),
                active, requiresEnrolmentEvaluation, supportsEmptyGrades, supportsTeacherConfirmation, allowsMarksheets);
        return evaluationSeason;
    }

    public void edit(final String code, final LocalizedString acronym, final LocalizedString name, final boolean normal,
            final boolean improvement, final boolean special, final boolean specialAuthorization, final Integer seasonOrder,
            final boolean active, final boolean requiresEnrolmentEvaluation, final boolean supportsEmptyGrades,
            final boolean supportsTeacherConfirmation, final boolean allowsMarksheets) {
        setAcronym(acronym);
        setName(name);
        setNormal(normal);
        setImprovement(improvement);
        setSpecial(special);
        setSpecialAuthorization(specialAuthorization);
        setCode(code);
        setProperties(seasonOrder, active, requiresEnrolmentEvaluation, supportsEmptyGrades, supportsTeacherConfirmation,
                allowsMarksheets);

        checkRules();
    }

    public void setProperties(final Integer seasonOrder, final boolean active, final boolean requiresEnrolmentEvaluation,
            final boolean supportsEmptyGrades, final boolean supportsTeacherConfirmation, final boolean allowsMarksheets) {
        setSeasonOrder(seasonOrder);
        setActive(active);
        setRequiresEnrolmentEvaluation(requiresEnrolmentEvaluation);
        setSupportsEmptyGrades(supportsEmptyGrades);
        setSupportsTeacherConfirmation(supportsTeacherConfirmation);
        setAllowsMarksheets(allowsMarksheets);
    }

    private void checkRules() {
        if (getSeasonOrder() == null) {
            throw new DomainException("error.EvaluationSeason.order.required");
        }

        if (StringUtils.isBlank(getCode())) {
            throw new DomainException("error.EvaluationSeason.code.required");
        }

        if (getAcronym() == null || getAcronym().isEmpty()) {
            throw new DomainException("error.EvaluationSeason.acronym.required");
        }

        if (getName() == null || getName().isEmpty()) {
            throw new DomainException("error.EvaluationSeason.name.required");
        }

        if (!hasValidType()) {
            throw new DomainException("error.EvaluationSeason.type.not.unique");
        }
    }

    private boolean hasValidType() {
        if (isSpecialAuthorization()) {
            return true;
        }
        return Stream.of(isNormal(), isImprovement(), isSpecial()).filter(Boolean::booleanValue).count() == 1;
    }

    public boolean isNormal() {
        return getNormal();
    }

    public boolean isSpecial() {
        return getSpecial();
    }

    public boolean isImprovement() {
        return getImprovement();
    }

    public boolean isSpecialAuthorization() {
        return getSpecialAuthorization();
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isRequiresEnrolmentEvaluation() {
        return getRequiresEnrolmentEvaluation();
    }

    public boolean isSupportsEmptyGrades() {
        return getSupportsEmptyGrades();
    }

    public boolean isSupportsTeacherConfirmation() {
        return getSupportsTeacherConfirmation();
    }

    public boolean isAllowsMarksheets() {
        return getAllowsMarksheets();
    }

    @Override
    public void setCode(String code) {
        if (StringUtils.isNotBlank(code)) {
            if (all().anyMatch(es -> es != this && Objects.equals(es.getCode(), code))) {
                throw new DomainException("error.EvaluationSeason.code.already.exists");
            }
        }

        super.setCode(code);
    }

    @Override
    public void setName(final LocalizedString name) {
        if (name != null && !name.isEmpty()) {
            if (all().anyMatch(es -> es != this && name.equals(es.getName()))) {
                throw new DomainException("error.EvaluationSeason.duplicated.name");
            }
        }

        super.setName(name);
    }

    public void delete() {
        if (!getEvaluationSet().isEmpty()) {
            throw new DomainException("error.EvaluationSeason.not.empty.to.delete");
        }

        super.setEvaluationConfiguration(null);
        super.setEvaluationConfigurationAsDefault(null);
        getUnitsAllowedForDirectEnrolmentSet().clear();
        super.deleteDomainObject();
    }

    public static Integer getNextSeasonOrder() {
        return all().map(EvaluationSeason::getSeasonOrder).filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    }

    public static Stream<EvaluationSeason> readNormalSeasons() {
        return all().filter(EvaluationSeason::isNormal);
    }

    public static Stream<EvaluationSeason> readSpecialSeasons() {
        return all().filter(EvaluationSeason::isSpecial);
    }

    public static Stream<EvaluationSeason> readImprovementSeasons() {
        return all().filter(EvaluationSeason::isImprovement);
    }

    public static Stream<EvaluationSeason> readSpecialAuthorizations() {
        return all().filter(EvaluationSeason::isSpecialAuthorization);
    }

    public static Stream<EvaluationSeason> all() {
        return EvaluationConfiguration.getInstance().getEvaluationSeasonSet().stream();
    }

    public static Optional<EvaluationSeason> findByCode(final String code) {
        return all().filter(es -> Objects.equals(es.getCode(), code)).findFirst();
    }

    public static Stream<EvaluationSeason> findByActive(final boolean active) {
        return all().filter(es -> active == es.getActive());
    }

    @Override
    public int compareTo(EvaluationSeason o) {
        return getExternalId().compareTo(o.getExternalId());
    }

}
