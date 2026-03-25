package org.fenixedu.academic.domain.util;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;

import java.util.Locale;

public abstract class UserUtil {

    public static final String ADMIN_USERNAME = "admin";

    public static void initAdminUser() {
        final UserProfile userProfile =
                new UserProfile("Fenix", "Admin", "Fenix Admin", "fenix.admin@fenixedu.com", Locale.getDefault());
        new User(ADMIN_USERNAME, userProfile);
        new Person(userProfile);
    }
}
