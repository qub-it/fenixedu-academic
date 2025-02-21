package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class AcademicPeriodOrder extends AcademicPeriodOrder_Base {

    private static final Map<String, AcademicPeriodOrder> CACHED_OBJECTS = new HashMap<>();

    protected AcademicPeriodOrder() {
        super();
        super.setRoot(Bennu.getInstance());
    }

    protected AcademicPeriodOrder(final AcademicPeriod academicPeriod, Integer periodOrder) {
        this();

        super.setAcademicPeriod(academicPeriod);
        super.setPeriodOrder(periodOrder);

        checkRules();
    }

    private void checkRules() {
        if (findAll().anyMatch(apo -> apo != this && apo.isFor(getAcademicPeriod(), getPeriodOrder()))) {
            throw new DomainException("error.AcademicPeriodOrder.already.exists.for.same.academic.period.and.order");
        }
    }

    public boolean isFor(final AcademicPeriod academicPeriod, final Integer periodOrder) {
        return getAcademicPeriod().equals(academicPeriod) && Objects.equals(getPeriodOrder(), periodOrder);
    }

    @Override
    public void setAcademicPeriod(AcademicPeriod academicPeriod) {
        throw new DomainException("error.AcademicPeriodOrder.academic.period.cannot.be.changed");
    }

    @Override
    public void setPeriodOrder(Integer periodOrder) {
        throw new DomainException("error.AcademicPeriodOrder.period.order.cannot.be.changed");
    }

    public String getCode() {
        return getAcademicPeriod().getCode() + getPeriodOrder();
    }

    public void delete() {
        super.setRoot(null);

        super.deleteDomainObject();
    }

//    public boolean isFor(ExecutionInterval executionInterval) {
//        return getAcademicPeriod().equals(executionInterval.getAcademicPeriod())
//                && getPeriodOrder().intValue() == executionInterval.getChildOrder();
//    }

    public static void initialize() {
        if (findAll().findAny().isEmpty()) {
            initializeFromAcademicPeriod();
        }

        AcademicPeriodOrder.findAll()
                .forEach(apo -> CACHED_OBJECTS.put(buildCacheKey(apo.getAcademicPeriod(), apo.getPeriodOrder()), apo));
    }

    private static void initializeFromAcademicPeriod() {
        AcademicPeriod.values().stream().filter(ap -> !ap.equals(AcademicPeriod.OTHER)).filter(ap -> ap.getWeight() <= 1)
                .forEach(ap -> {
                    final int periodsCount = (int) Math.ceil(1 / ap.getWeight());
                    for (int i = 1; i <= periodsCount; i++) {
                        if (findBy(ap, i).isEmpty()) {
                            new AcademicPeriodOrder(ap, i);
                        }
                    }
                });
    }

    public static Optional<AcademicPeriodOrder> findBy(AcademicPeriod academicPeriod, Integer periodOrder) {
        return Optional.ofNullable(CACHED_OBJECTS.get(buildCacheKey(academicPeriod, periodOrder)));
    }

    private static final String buildCacheKey(final AcademicPeriod academicPeriod, Integer periodOrder) {
        return academicPeriod.getRepresentationInStringFormat() + "#" + periodOrder.toString();
    }

    public static Stream<AcademicPeriodOrder> findAll() {
        return Bennu.getInstance().getAcademicPeriodOrdersSet().stream();
    }

}
