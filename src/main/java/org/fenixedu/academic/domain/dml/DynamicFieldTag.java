package org.fenixedu.academic.domain.dml;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class DynamicFieldTag extends DynamicFieldTag_Base {

    public DynamicFieldTag() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static DynamicFieldTag create(String code, LocalizedString name, String domainObjectClassName) {
        Set<DynamicFieldTag> tagsSet = DynamicFieldTag.findByDomainObjectClassName(domainObjectClassName);

        if (tagsSet.stream().anyMatch(tag -> StringUtils.equals(tag.getCode(), code))) {
            throw new DomainException("error.dynamicFieldTag.duplicated.code");
        }

        DynamicFieldTag newTag = new DynamicFieldTag();

        newTag.setCode(code);
        newTag.setName(name);
        newTag.setDomainObjectClassName(domainObjectClassName);

        return newTag;
    }

    public static Set<DynamicFieldTag> findByDomainObjectClassName(String domainObjectClassName) {
        return Bennu.getInstance().getDynamicFieldTagSet().stream()
                .filter(tag -> StringUtils.equals(tag.getDomainObjectClassName(), domainObjectClassName))
                .collect(Collectors.toSet());
    }

    public void delete() {
        getDescriptorSet().forEach(d -> d.setOrCreateDefaultTag());
        getDescriptorSet().clear();

        super.setRoot(null);
        super.deleteDomainObject();
    }
}
