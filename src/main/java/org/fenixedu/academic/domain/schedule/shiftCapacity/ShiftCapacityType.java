package org.fenixedu.academic.domain.schedule.shiftCapacity;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

public class ShiftCapacityType extends ShiftCapacityType_Base {

    protected ShiftCapacityType() {
        super();
        setRoot(Bennu.getInstance());
        setActive(true);

        final Integer maxPriority = findAll().map(t -> t.getEvaluationPriority()).max(Comparator.naturalOrder()).orElse(0);
        setEvaluationPriority(maxPriority + 1);
    }

    public static ShiftCapacityType create(final LocalizedString name, final ShiftCapacityStrategy strategy) {
        final ShiftCapacityType type = new ShiftCapacityType();
        type.setName(name);
        type.setStrategy(strategy);
        return type;
    }

    public boolean isActive() {
        return super.getActive();
    }

    public boolean isDefaultType() {
        return super.getDefaultType();
    }

    @Override
    public void setDefaultType(boolean defaultType) {
        if (defaultType) {
            findAll().filter(ShiftCapacityType::isDefaultType).forEach(t -> t.setDefaultType(false)); // set current default type to false
        }
        super.setDefaultType(defaultType);
    }

    @Override
    public void setCode(String code) {
        if (StringUtils.isNotBlank(code) && findAll().filter(t -> t != this).anyMatch(t -> code.equalsIgnoreCase(t.getCode()))) {
            throw new DomainException("error.ShiftCapacityType.setCode.alreadyExistsTypeWithSameCode", code);
        }
        super.setCode(code);
    }

    public void incrementEvaluationPriority() {
        int newPriority = getEvaluationPriority() - 1;
        findAll().filter(t -> t.getEvaluationPriority() == newPriority).findAny().ifPresent(t -> {
            t.setEvaluationPriority(newPriority + 1);
            setEvaluationPriority(newPriority);
        });
    }

    public void decrementEvaluationPriority() {
        int newPriority = getEvaluationPriority() + 1;
        findAll().filter(t -> t.getEvaluationPriority() == newPriority).findAny().ifPresent(t -> {
            t.setEvaluationPriority(newPriority - 1);
            setEvaluationPriority(newPriority);
        });
    }

    public void delete() {
        if (!getCapacitiesSet().isEmpty()) {
            throw new DomainException("error.ShiftCapacityType.delete.associatedCapacitiesNotEmpty");
        }

        setStrategy(null);
        setRoot(null);

        final AtomicInteger newPriority = new AtomicInteger();
        findAll().sorted(Comparator.comparing(ShiftCapacityType::getEvaluationPriority))
                .forEach(t -> t.setEvaluationPriority(newPriority.incrementAndGet()));

        super.deleteDomainObject();
    }

    @Atomic
    private static ShiftCapacityType createDefault() {
        return findAll().filter(ShiftCapacityType::isDefaultType).findAny().orElseGet(() -> {
            final ShiftCapacityType defaultType = new ShiftCapacityType();
            defaultType.setDefaultType(true);
            defaultType.setName(BundleUtil.getLocalizedString(Bundle.APPLICATION, "label.allStudents"));

            final ShiftCapacityStrategy defaultStrategy =
                    ShiftCapacityStrategy.findAll().filter(s -> s instanceof AllStudentsShiftCapacityStrategy).findAny()
                            .orElseGet(() -> new AllStudentsShiftCapacityStrategy());
            defaultType.setStrategy(defaultStrategy);

            return defaultType;
        });
    }

    // TODO change visibility to private
    public static ShiftCapacityType findOrCreateDefault() {
        return findAll().filter(ShiftCapacityType::isDefaultType).findAny().orElseGet(() -> createDefault());
    }

    public static Stream<ShiftCapacityType> findAll() {
        return Bennu.getInstance().getShiftCapacityTypesSet().stream();
    }

}
