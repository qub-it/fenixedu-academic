package org.fenixedu.academic.domain.degreeStructure;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class TeachingMethodType extends TeachingMethodType_Base {

    protected TeachingMethodType() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static TeachingMethodType create(final String code, final LocalizedString name, final LocalizedString initials) {
        final TeachingMethodType result = new TeachingMethodType();
        result.setCode(code);
        result.setName(name);
        result.setInitials(initials);
        result.setDisplayOrder((int) findAll().count());
        return result;
    }

    public static Stream<TeachingMethodType> findAll() {
        return Bennu.getInstance().getTeachingMethodTypesSet().stream();
    }

    public static Optional<TeachingMethodType> findByCode(final String code) {
        return findAll().filter(type -> Objects.equals(type.getCode(), code)).findAny();
    }

    @Override
    public void setCode(String code) {
        if (findAll().filter(type -> type != this).anyMatch(type -> Objects.equals(type.getCode(), code))) {
            throw new IllegalArgumentException("TeachingMethodType already exists with same code");
        }

        super.setCode(code);
    }

    @Override
    public void setDisplayOrder(int displayOrder) {
        if (findAll().filter(type -> type != this).anyMatch(type -> type.getDisplayOrder() == displayOrder)) {
            throw new IllegalArgumentException("TeachingMethodType already exists with same display order");
        }

        super.setDisplayOrder(displayOrder);
    }

    public void delete() {
        if (!getCourseLoadDurationsByTeachingMethodSet().isEmpty()) {
            throw new IllegalStateException("TeachingMethodType cannot be deleted, because it has CourseLoadDurations");
        }

        setRoot(null);
        super.deleteDomainObject();
    }
}
