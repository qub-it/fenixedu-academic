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
package org.fenixedu.academic.domain.curricularPeriod;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.CurricularPeriodInfoDTO;
import org.fenixedu.academic.util.CurricularPeriodLabelFormatter;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

/**
 * 
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 * 
 */
public class CurricularPeriod extends CurricularPeriod_Base implements Comparable<CurricularPeriod> {

    static {
        getRelationCurricularPeriodParentChilds().addListener(new CurricularPeriodParentChildsListener());
    }

    public CurricularPeriod(AcademicPeriod academicPeriod) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setAcademicPeriod(academicPeriod);
    }

    public CurricularPeriod(AcademicPeriod academicPeriod, Integer order, CurricularPeriod parent) {
        this(academicPeriod);
        setChildOrder(order);
        setParent(parent);

    }

    public List<CurricularPeriod> getSortedChilds() {
        return getChildsSet().stream().sorted().toList();
    }

    public Optional<CurricularPeriod> findChild(AcademicPeriod academicPeriod, int order) {
        return getChildsSet().stream().filter(cp -> cp.getChildOrder().intValue() == order)
                .filter(cp -> cp.getAcademicPeriod().equals(academicPeriod)).findAny();
    }

    /**
     * Returns the period reached by walking the given path of child segments from
     * this CurricularPeriod, or null if any segment has no matching child.
     */
    public CurricularPeriod getCurricularPeriod(CurricularPeriodInfoDTO... curricularPeriodsPaths) {
        CurricularPeriod curricularPeriod = this;

        for (CurricularPeriodInfoDTO path : sortAndValidatePath(curricularPeriodsPaths)) {
            curricularPeriod = curricularPeriod.findChild(path.getPeriodType(), path.getOrder()).orElse(null);

            if (curricularPeriod == null) {
                break;
            }
        }

        return curricularPeriod;
    }

    public Integer getOrderByType(AcademicPeriod academicPeriod) {
        if (getAcademicPeriod().equals(academicPeriod)) {
            return getChildOrder();
        }

        if (getParent() != null && getParent().getAcademicPeriod().getWeight() > getAcademicPeriod().getWeight()) {
            return this.getParent().getOrderByType(academicPeriod);
        }

        return null;
    }

    private CurricularPeriodInfoDTO[] sortAndValidatePath(CurricularPeriodInfoDTO... curricularPeriodsPaths) {
        CurricularPeriodInfoDTO[] sorted = curricularPeriodsPaths.clone();

        Arrays.sort(sorted, (c1, c2) -> {
            final float c1Weight = c1.getPeriodType().getWeight();
            final float c2Weight = c2.getPeriodType().getWeight();

            if (c1Weight == c2Weight) {
                throw new DomainException("error.pathShouldNotHaveSameTypePeriods");
            }

            return Float.compare(c2Weight, c1Weight);
        });
        return sorted;
    }

    public void delete() {

        if (!getContextsSet().isEmpty()) {
            throw new DomainException("error.delete.CurricularPeriod.existingContexts", getFullLabel());
        }

        setDegreeCurricularPlan(null);

        final CurricularPeriod parent = getParent();
        setParent(null);

        // reorder remaining 'brothers' periods
        if (parent != null) {
            final Map<AcademicPeriod, AtomicInteger> counter = new HashMap<>();
            parent.getSortedChilds().forEach(child -> child.setChildOrder(
                    counter.computeIfAbsent(child.getAcademicPeriod(), x -> new AtomicInteger()).incrementAndGet()));
        }

        for (CurricularPeriod child : getChildsSet()) {
            child.delete();
        }

        setRootDomainObject(null);
        deleteDomainObject();

    }

    public String getLabel() {
        return CurricularPeriodLabelFormatter.getLabel(this, false);
    }

    public String getFullLabel() {
        return CurricularPeriodLabelFormatter.getFullLabel(this, false);
    }

    public String getFullLabel(final Locale locale) {
        return CurricularPeriodLabelFormatter.getFullLabelI18N(this, false, locale);
    }

    @Override
    public int compareTo(CurricularPeriod o) {
        // sort by Parent
        int parentCompare = Comparator.nullsFirst(CurricularPeriod::compareTo).compare(getParent(), o.getParent());
        if (parentCompare != 0) {
            return parentCompare;
        }

        // sort by AcademicPeriod
        Float w1 = Optional.ofNullable(getAcademicPeriod()).map(AcademicPeriod::getWeight).orElse(null);
        Float w2 = Optional.ofNullable(o.getAcademicPeriod()).map(AcademicPeriod::getWeight).orElse(null);

        int academicPeriodTypeCompare = Comparator.nullsFirst(Float::compareTo).compare(w1, w2);
        if (academicPeriodTypeCompare != 0) {
            return academicPeriodTypeCompare;
        }

        // sort by child order
        return Comparator.nullsFirst(Integer::compareTo).compare(getChildOrder(), o.getChildOrder());
    }

    private static class CurricularPeriodParentChildsListener extends RelationAdapter<CurricularPeriod, CurricularPeriod> {
        @Override
        public void beforeAdd(CurricularPeriod parent, CurricularPeriod child) {
            if (parent == null) {
                return;
            }

            final AcademicPeriod childAcademicPeriod = child.getAcademicPeriod();
            if (childAcademicPeriod.getWeight() >= parent.getAcademicPeriod().getWeight()) {
                throw new DomainException("error.childTypeGreaterThanParentType");
            }

            // re-order childs
            Integer order = child.getChildOrder();
            if (order == null) {
                long count =
                        parent.getChildsSet().stream().filter(p -> p.getAcademicPeriod().equals(childAcademicPeriod)).count();
                child.setChildOrder(Math.toIntExact(count) + 1);
            }
        }
    }

    public Integer getParentOrder() {
        return Optional.ofNullable(getParent()).map(CurricularPeriod::getChildOrder).orElse(null);
    }

    public CurricularPeriod getNext() {
        if (this.getParent() == null) {
            return null;
        }

        List<CurricularPeriod> brothers = this.getParent().getSortedChilds();

        for (Iterator<CurricularPeriod> iterator = brothers.iterator(); iterator.hasNext(); ) {
            CurricularPeriod brother = iterator.next();

            if (brother.getChildOrder().equals(this.getChildOrder()) && iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    public CurricularPeriod contains(AcademicPeriod academicPeriod, Integer order) {
        if (Objects.equals(getAcademicPeriod(), academicPeriod) && Objects.equals(getChildOrder(), order)) {
            return this;
        }

        return getChildsSet().stream().map(child -> child.contains(academicPeriod, order)).filter(Objects::nonNull).findFirst()
                .orElse(null);
    }

    public boolean hasCurricularPeriod(AcademicPeriod academicPeriod, Integer order) {
        if (Objects.equals(getAcademicPeriod(), academicPeriod) && Objects.equals(getChildOrder(), order)) {
            return true;
        }

        return Optional.ofNullable(getParent()).map(parent -> parent.hasCurricularPeriod(academicPeriod, order)).orElse(false);
    }

    public int getAbsoluteOrderOfChild() {
        if (getChildOrder() == null) {
            return 1;
        } else {
            final CurricularPeriod parentCurricularPeriod = getParent();
            final int absoluteOrderOfParent = parentCurricularPeriod.getAbsoluteOrderOfChild();
            final int numberOfBrothersAndSisters = parentCurricularPeriod.getChildsSet().size();
            return (absoluteOrderOfParent - 1) * numberOfBrothersAndSisters + getChildOrder().intValue();
        }
    }

    public boolean hasChildOrder() {
        return getChildOrder() != null;
    }

    public boolean hasChildOrderValue(final Integer order) {
        return hasChildOrder() && getChildOrder().equals(order);
    }

    public static CurricularPeriod findEquivalentCurricularPeriodForDegreeCurricularPlan(CurricularPeriod sourcePeriod,
            DegreeCurricularPlan targetDCP) {
        if (sourcePeriod == null || targetDCP == null) {
            return null;
        }

        int year = sourcePeriod.getParentOrder() != null ? sourcePeriod.getParentOrder() : 1;
        return targetDCP.getCurricularPeriodFor(year, sourcePeriod.getChildOrder(), sourcePeriod.getAcademicPeriod());
    }

}
