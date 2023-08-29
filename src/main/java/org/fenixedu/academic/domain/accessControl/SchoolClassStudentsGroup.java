package org.fenixedu.academic.domain.accessControl;

import java.util.Objects;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.joda.time.DateTime;

@GroupOperator("schoolClassStudentsGroup")
public class SchoolClassStudentsGroup extends FenixGroup {

    @GroupArgument
    private SchoolClass schoolClass;

    private SchoolClassStudentsGroup() {
        super();
    }

    private SchoolClassStudentsGroup(SchoolClass schoolClass) {
        this();
        this.schoolClass = schoolClass;
    }

    public static SchoolClassStudentsGroup get(SchoolClass schoolClass) {
        Objects.requireNonNull(schoolClass, "School class is required");
        return new SchoolClassStudentsGroup(schoolClass);
    }

    @Override
    public Stream<User> getMembers() {
        return schoolClass.getRegistrationsSet().stream().map(Registration::getPerson).map(Person::getUser);
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        return getMembers();
    }

    @Override
    public boolean isMember(User user) {
        return getMembers().anyMatch(u -> u == user);
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return isMember(user);
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPresentationNameKeyArgs() {
        return new String[] { schoolClass.getName(), schoolClass.getExecutionInterval().getQualifiedName() };
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(schoolClass);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SchoolClassStudentsGroup) {
            return Objects.equals(schoolClass, ((SchoolClassStudentsGroup) object).schoolClass);
        }
        return false;
    }
}
