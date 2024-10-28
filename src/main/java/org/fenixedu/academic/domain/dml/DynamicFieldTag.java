package org.fenixedu.academic.domain.dml;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class DynamicFieldTag extends DynamicFieldTag_Base {

    private static final String DEFAULT = "DEFAULT";

    private DynamicFieldTag() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static DynamicFieldTag create(String code, LocalizedString name, String domainObjectClassName) {
        DynamicFieldTag newTag = new DynamicFieldTag();

        newTag.setDomainObjectClassName(domainObjectClassName);
        newTag.setCode(code);
        newTag.setName(name);

        return newTag;
    }

    @Override
    public void setCode(String code) {
        Stream<DynamicFieldTag> tagsSet = DynamicFieldTag.findByDomainObjectClassName(getDomainObjectClassName());

        if (tagsSet.anyMatch(tag -> StringUtils.equals(tag.getCode(), code))) {
            throw new DomainException("error.dynamicFieldTag.duplicated.code");
        }

        super.setCode(code);
    }

    public static void setOrCreateDefaultTag(DynamicFieldDescriptor descriptor) {
        DynamicFieldTag defaultTag = DynamicFieldTag.findByDomainObjectClassName(descriptor.getDomainObjectClassName())
                .filter(tag -> StringUtils.equals(tag.getCode(), DEFAULT)).findAny().orElse(null);

        if (defaultTag == null) {
            defaultTag = DynamicFieldTag.create(DEFAULT,
                    BundleUtil.getLocalizedString(Bundle.APPLICATION, "dynamicFieldTag.defaultName"),
                    descriptor.getDomainObjectClassName());
        }

        descriptor.setTag(defaultTag);
    }

    public static Stream<DynamicFieldTag> findByDomainObjectClassName(String domainObjectClassName) {
        return Bennu.getInstance().getDynamicFieldTagSet().stream()
                .filter(tag -> StringUtils.equals(tag.getDomainObjectClassName(), domainObjectClassName));
    }

    public void delete() {
        getDescriptorSet().forEach(d -> setOrCreateDefaultTag(d));

        super.setRoot(null);
        super.deleteDomainObject();
    }
}
