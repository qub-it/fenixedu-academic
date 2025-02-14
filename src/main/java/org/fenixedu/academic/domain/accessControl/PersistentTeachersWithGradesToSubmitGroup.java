package org.fenixedu.academic.domain.accessControl;

import org.fenixedu.bennu.core.groups.Group;

public class PersistentTeachersWithGradesToSubmitGroup extends PersistentTeachersWithGradesToSubmitGroup_Base {

    private PersistentTeachersWithGradesToSubmitGroup() {
        super();
    }

    @Override
    public Group toGroup() {
        return Group.nobody();
    }

}
