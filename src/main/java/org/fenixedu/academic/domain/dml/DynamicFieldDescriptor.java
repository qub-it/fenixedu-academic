
package org.fenixedu.academic.domain.dml;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;

import pt.ist.fenixframework.DomainObject;

@SuppressWarnings("rawtypes")
public class DynamicFieldDescriptor extends DynamicFieldDescriptor_Base {

    static final private String DOMAIN_OBJECT_FIELD_NAME = "DynamicField";
    static final private String DOMAIN_OBJECT_METHOD_NAME_ADD = "add" + DOMAIN_OBJECT_FIELD_NAME;
    static final private String DOMAIN_OBJECT_METHOD_NAME_GET = "get" + DOMAIN_OBJECT_FIELD_NAME + "Set";

    protected DynamicFieldDescriptor() {
        super();
        setRoot(Bennu.getInstance());
    }

    protected void init(final String domainObjectClass, final String code, final LocalizedString name,
            final String fieldValueClass, final boolean required) {

        setDomainObjectClassName(domainObjectClass);
        setCode(code);
        setName(name);
        setFieldValueClassName(fieldValueClass);
        setRequired(required);
        setTag(DynamicFieldTag.getOrCreateDefaultTag(domainObjectClass));

        checkRules();

        changeOrder(find(getDomainObjectClassName()).size() - 1);
    }

    private void checkRules() {

        if (Strings.isNullOrEmpty(getCode())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.code.required");
        }

        if (StringUtils.isBlank(getDomainObjectClassName())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.domainObjectClass.required");
        }

        if (!isDomainObjectClass(getDomainObjectClassName())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.domainObjectClass.is.not.a.domain.object");
        }

        if (Bennu.getInstance().getDynamicFieldDescriptorSet().stream().anyMatch(
                df -> df != this && Objects.equals(getCode(), df.getCode()) && df.getDomainClass() == getDomainClass())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.duplicate");
        }

        if (getName() == null || getName().isEmpty()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.name.required");
        }

        if (Strings.isNullOrEmpty(getFieldValueClassName())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.fieldValueClassName.required");
        }

        if (!DynamicFieldValueConverter.isSupported(getFieldValueClass())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.fieldValueClass.unsupported");
        }

        if (getRequired() && getFieldsSet().stream().anyMatch(i -> Strings.isNullOrEmpty(i.getValue()))) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.value.inconsistent");
        }

        if ((getMinLength() != null && getMaxLength() == null) || (getMinLength() == null && getMaxLength() != null)) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.min.and.max.length.must.be.defined.simultaneously");
        }

        if (getMinLength() != null && getMaxLength() != null && getMinLength().intValue() > getMaxLength().intValue()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.min.length.cannot.be.greather.than.max.length");
        }

        if ((getMinNumber() != null && getMaxNumber() == null) || (getMinNumber() == null && getMaxNumber() != null)) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.min.and.max.number.must.be.defined.simultaneously");
        }

        if (getMinNumber() != null && getMaxNumber() != null && getMinNumber().intValue() > getMaxNumber().intValue()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.min.number.cannot.be.greather.than.max.length");
        }
    }

    public void edit(final String code, final LocalizedString name, final String fieldValueClassName, final boolean required,
            final Integer minLength, final Integer maxLength, final BigDecimal minNumber, final BigDecimal maxNumber,
            final boolean richText, final boolean largeSize) {

        super.setCode(code);
        super.setName(name);
        super.setFieldValueClassName(fieldValueClassName);
        super.setRequired(required);
        super.setMinLength(minLength);
        super.setMaxLength(maxLength);
        super.setMinNumber(minNumber);
        super.setMaxNumber(maxNumber);
        super.setRichText(richText);
        super.setLargeSize(largeSize);

        checkRules();
    }

    public Class<? extends DomainObject> getDomainClass() {
        return convertToDomainObjectClass(getDomainObjectClassName());
    }

    private boolean isFor(final DomainObject domainObject) {
        return domainObject != null && convertToDomainObjectClass(getDomainObjectClassName()) == domainObject.getClass();
    }

    protected DynamicField createField(final DomainObject domainObject) {
        final DynamicField result = new DynamicField();

        result.setDescriptor(this);
        setField(domainObject, result);

        // checkRules
        findField(domainObject);

        return result;
    }

    @SuppressWarnings("unchecked")
    protected DynamicField findField(final DomainObject domainObject) {
        DynamicField result = null;

        if (isFor(domainObject)) {

            try {
                final Method method = domainObject.getClass().getMethod(DOMAIN_OBJECT_METHOD_NAME_GET);
                final Set<DynamicField> fields = (Set<DynamicField>) method.invoke(domainObject);

                for (final DynamicField iter : fields) {
                    if (iter.getDescriptor() == this) {

                        if (result != null) {
                            throw new IllegalArgumentException("error.DynamicField.duplicate");
                        }

                        result = iter;
                    }
                }

            } catch (final Throwable t) {
            }
        }

        return result;
    }

    protected void setField(final DomainObject domainObject, final DynamicField field) {
        if (isFor(domainObject) && getFieldsSet().contains(field)) {

            try {
                final Method method = domainObject.getClass().getMethod(DOMAIN_OBJECT_METHOD_NAME_ADD, DynamicField.class);
                method.invoke(domainObject, field);
            } catch (final Throwable t) {
            }
        }
    }

    public Class getFieldValueClass() {
        try {
            return Class.forName(getFieldValueClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void moveUp() {
        final int currentIndex = getOrder();
        if (currentIndex == 0) {
            return;
        }

        final DynamicFieldDescriptor toChange = findAtPosition(getDomainObjectClassName(), currentIndex - 1);
        toChange.changeOrder(currentIndex);
        changeOrder(currentIndex - 1);
    }

    public void moveTop() {
        while (getOrder() != 0) {
            moveUp();
        }
    }

    public void moveDown() {
        final int currentIndex = getOrder();
        if (currentIndex == find(getDomainObjectClassName()).size() - 1) {
            return;
        }

        final DynamicFieldDescriptor toChange = findAtPosition(getDomainObjectClassName(), currentIndex + 1);
        toChange.changeOrder(currentIndex);
        changeOrder(currentIndex + 1);
    }

    public void moveBottom() {
        while (getOrder() < find(getDomainObjectClassName()).size() - 1) {
            moveDown();
        }
    }

    protected void changeOrder(int order) {
        super.setOrder(order);
    }

    @Override
    public void setOrder(int order) {
        throw new RuntimeException("Order change should be done using move methods");
    }

    public void delete() {

        moveBottom();

        if (!getFieldsSet().isEmpty()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.cannot.delete.with.field.instances");
        }

        setTag(null);

        super.setRoot(null);

        super.deleteDomainObject();
    }

    public static DynamicFieldDescriptor create(final String domainClass, final String code, final LocalizedString name,
            final String fieldValueClass, final boolean required) {
        final DynamicFieldDescriptor result = new DynamicFieldDescriptor();
        result.init(domainClass, code, name, fieldValueClass, required);

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DomainObject> convertToDomainObjectClass(String className) {
        try {

            if (isDomainObjectClass(className)) {
                return (Class<? extends DomainObject>) Class.forName(className);
            }

            throw new IllegalArgumentException("Class " + className + " is not a domain object");

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isDomainObjectClass(String className) {
        try {
            final Class<?> result = Class.forName(className);
            return DomainObject.class.isAssignableFrom(result);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static DynamicFieldDescriptor findAtPosition(final String domainClass, final int order) {
        return find(domainClass).stream().filter(df -> df.getOrder() == order).findFirst().orElse(null);
    }

    public static Collection<DynamicFieldDescriptor> find(final String className) {
        if (StringUtils.isBlank(className)) {
            return Collections.emptySet();
        }

        final Class<? extends DomainObject> domainObjectClass = convertToDomainObjectClass(className);
        return Bennu.getInstance().getDynamicFieldDescriptorSet().stream().filter(df -> df.getDomainClass() == domainObjectClass)
                .collect(Collectors.toSet());
    }

    public static Set<DynamicFieldDescriptor> find(final DomainObject domainObject) {
        final Set<DynamicFieldDescriptor> result = new HashSet<>();

        if (domainObject != null) {

            for (final DynamicFieldDescriptor iter : Bennu.getInstance().getDynamicFieldDescriptorSet()) {
                if (iter.isFor(domainObject)) {
                    result.add(iter);
                }
            }
        }

        return result;
    }
}
