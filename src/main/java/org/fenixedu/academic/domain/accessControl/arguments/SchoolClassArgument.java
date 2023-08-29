package org.fenixedu.academic.domain.accessControl.arguments;

import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.bennu.core.annotation.GroupArgumentParser;

@GroupArgumentParser
public class SchoolClassArgument extends DomainObjectArgumentParser<SchoolClass> {
    @Override
    public Class<SchoolClass> type() {
        return SchoolClass.class;
    }
}
