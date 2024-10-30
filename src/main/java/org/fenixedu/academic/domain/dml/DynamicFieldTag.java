package org.fenixedu.academic.domain.dml;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class DynamicFieldTag extends DynamicFieldTag_Base {

    private static final String DEFAULT_CODE = "DEFAULT_CODE";

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

        if (code != getCode() && tagsSet.anyMatch(tag -> StringUtils.equals(tag.getCode(), code))) {
            throw new DomainException("error.dynamicFieldTag.duplicated.code");
        }

        super.setCode(code);
    }

    public static DynamicFieldTag getOrCreateDefaultTag(String domainObjectClassName) {
        Optional<DynamicFieldTag> optional = DynamicFieldTag.findByDomainObjectClassName(domainObjectClassName)
                .filter(tag -> tag.getCode().equals(DEFAULT_CODE)).findAny();
        if (optional.isEmpty()) {
            return DynamicFieldTag.create(DEFAULT_CODE,
                    BundleUtil.getLocalizedString(Bundle.APPLICATION, "dynamicFieldTag.defaultName"), domainObjectClassName);
        } else {
            return optional.get();
        }
    }

    public static Stream<DynamicFieldTag> findByDomainObjectClassName(String domainObjectClassName) {
        return Bennu.getInstance().getDynamicFieldTagsSet().stream()
                .filter(tag -> tag.getDomainObjectClassName().equals(domainObjectClassName));
    }

    public void delete() {
        getDescriptorsSet().forEach(d -> d.setTag(getOrCreateDefaultTag(d.getDomainObjectClassName())));

        super.setRoot(null);
        super.deleteDomainObject();
    }
}
