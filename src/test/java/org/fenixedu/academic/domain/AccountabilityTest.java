package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.organizationalStructure.Accountability;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.joda.time.YearMonthDay;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AccountabilityTest {

    private static final Comparator<Accountability> COMPARATOR = Accountability.getComparatorByBeginDate();
    private AccountabilityType accountabilityType;
    private Person parent;
    private Person child;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            OrganizationalStructureTest.initTypes();
            OrganizationalStructureTest.initUnits();
            return null;
        });
    }

    @Before
    public void setUp() {
        parent = createPerson("Parent", "parent" + UUID.randomUUID().toString().substring(0, 5));
        child = createPerson("Child", "child" + UUID.randomUUID().toString().substring(0, 5));
        accountabilityType = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);
    }

    @Test
    public void testGetComparatorByBeginDate_ordersByBeginDate() {
        final Accountability earlier = new Accountability(parent, child, accountabilityType);
        earlier.setBeginDate(new YearMonthDay(2020, 1, 1));

        final Accountability later = new Accountability(parent, child, accountabilityType);
        later.setBeginDate(new YearMonthDay(2022, 6, 15));

        assertTrue(COMPARATOR.compare(earlier, later) < 0);
        assertTrue(COMPARATOR.compare(later, earlier) > 0);
    }

    @Test
    public void testGetComparatorByBeginDate_equalBeginDates_fallsBackToId() {
        final Accountability first = new Accountability(parent, child, accountabilityType);
        first.setBeginDate(new YearMonthDay(2020, 1, 1));

        final Accountability second = new Accountability(parent, child, accountabilityType);
        second.setBeginDate(new YearMonthDay(2020, 1, 1));

        final int result = COMPARATOR.compare(first, second);
        if (first.getExternalId().equals(second.getExternalId())) {
            assertEquals(0, result);
        } else {
            assertTrue(result != 0);
        }
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }
}
