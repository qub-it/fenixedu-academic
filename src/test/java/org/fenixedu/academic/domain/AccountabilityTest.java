package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import org.fenixedu.academic.domain.organizationalStructure.Accountability;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AccountabilityTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            OrganizationalStructureTest.initTypes();
            OrganizationalStructureTest.initUnits();
            return null;
        });
    }

    private Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    @Test
    public void testGetComparatorByBeginDate_ordersByBeginDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent1");
            final Person child = createPerson("Child", "child1");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability earlier = new Accountability(parent, child, type);
            earlier.setBeginDate(new YearMonthDay(2020, 1, 1));

            final Accountability later = new Accountability(parent, child, type);
            later.setBeginDate(new YearMonthDay(2022, 6, 15));

            final Comparator<Accountability> comparator = Accountability.getComparatorByBeginDate();
            assertTrue(comparator.compare(earlier, later) < 0);
            assertTrue(comparator.compare(later, earlier) > 0);
            return null;
        });
    }

    @Test
    public void testGetComparatorByBeginDate_equalBeginDates_fallsBackToId() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent2");
            final Person child = createPerson("Child", "child2");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability first = new Accountability(parent, child, type);
            first.setBeginDate(new YearMonthDay(2020, 1, 1));

            final Accountability second = new Accountability(parent, child, type);
            second.setBeginDate(new YearMonthDay(2020, 1, 1));

            final Comparator<Accountability> comparator = Accountability.getComparatorByBeginDate();
            final int result = comparator.compare(first, second);
            if (first.getExternalId().equals(second.getExternalId())) {
                assertEquals(0, result);
            } else {
                assertTrue(result != 0);
            }
            return null;
        });
    }

    @Test
    public void testIsActive_withCurrentDate_returnsTrue() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent3");
            final Person child = createPerson("Child", "child3");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertTrue(accountability.isActive(new YearMonthDay()));
            return null;
        });
    }

    @Test
    public void testIsActive_withPastEndDate_returnsFalse() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent4");
            final Person child = createPerson("Child", "child4");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2020, 1, 1));
            accountability.setEndDate(new YearMonthDay(2020, 12, 31));

            assertFalse(accountability.isActive(new YearMonthDay(2021, 6, 1)));
            return null;
        });
    }

    @Test
    public void testIsActive_noArg() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent5");
            final Person child = createPerson("Child", "child5");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertTrue(accountability.isActive());
            return null;
        });
    }

    @Test
    public void testIsFinished_withPastEndDate_returnsTrue() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent6");
            final Person child = createPerson("Child", "child6");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2020, 1, 1));
            accountability.setEndDate(new YearMonthDay(2020, 12, 31));

            assertTrue(accountability.isFinished());
            return null;
        });
    }

    @Test
    public void testIsFinished_withoutEndDate_returnsFalse() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent7");
            final Person child = createPerson("Child", "child7");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertFalse(accountability.isFinished());
            return null;
        });
    }

    @Test
    public void testGetBeginLocalDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent8");
            final Person child = createPerson("Child", "child8");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2021, 3, 15));

            final LocalDate result = accountability.getBeginLocalDate();
            assertNotNull(result);
            assertEquals(2021, result.getYear());
            assertEquals(3, result.getMonthOfYear());
            assertEquals(15, result.getDayOfMonth());
            return null;
        });
    }

    @Test
    public void testGetBeginLocalDate_nullBeginDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent9");
            final Person child = createPerson("Child", "child9");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(null);

            assertNull(accountability.getBeginLocalDate());
            return null;
        });
    }

    @Test
    public void testSetBeginLocalDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent10");
            final Person child = createPerson("Child", "child10");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginLocalDate(new LocalDate(2021, 5, 20));

            final YearMonthDay beginDate = accountability.getBeginDate();
            assertNotNull(beginDate);
            assertEquals(2021, beginDate.getYear());
            assertEquals(5, beginDate.getMonthOfYear());
            assertEquals(20, beginDate.getDayOfMonth());
            return null;
        });
    }

    @Test
    public void testSetBeginLocalDate_null() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent11");
            final Person child = createPerson("Child", "child11");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginLocalDate(null);

            assertNull(accountability.getBeginDate());
            return null;
        });
    }

    @Test
    public void testGetEndLocalDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent12");
            final Person child = createPerson("Child", "child12");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setEndDate(new YearMonthDay(2022, 8, 10));

            final LocalDate result = accountability.getEndLocalDate();
            assertNotNull(result);
            assertEquals(2022, result.getYear());
            assertEquals(8, result.getMonthOfYear());
            assertEquals(10, result.getDayOfMonth());
            return null;
        });
    }

    @Test
    public void testGetEndLocalDate_nullEndDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent13");
            final Person child = createPerson("Child", "child13");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertNull(accountability.getEndLocalDate());
            return null;
        });
    }

    @Test
    public void testSetEndLocalDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent14");
            final Person child = createPerson("Child", "child14");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setEndLocalDate(new LocalDate(2023, 12, 25));

            final YearMonthDay endDate = accountability.getEndDate();
            assertNotNull(endDate);
            assertEquals(2023, endDate.getYear());
            assertEquals(12, endDate.getMonthOfYear());
            assertEquals(25, endDate.getDayOfMonth());
            return null;
        });
    }

    @Test
    public void testSetEndLocalDate_null() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent15");
            final Person child = createPerson("Child", "child15");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setEndDate(new YearMonthDay(2023, 1, 1));
            accountability.setEndLocalDate(null);

            assertNull(accountability.getEndDate());
            return null;
        });
    }

    @Test
    public void testGetBeginDateInDateType() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent16");
            final Person child = createPerson("Child", "child16");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2021, 7, 4));

            final Date result = accountability.getBeginDateInDateType();
            assertNotNull(result);
            return null;
        });
    }

    @Test
    public void testGetBeginDateInDateType_nullBeginDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent17");
            final Person child = createPerson("Child", "child17");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(null);

            assertNull(accountability.getBeginDateInDateType());
            return null;
        });
    }

    @Test
    public void testGetEndDateInDateType() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent18");
            final Person child = createPerson("Child", "child18");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setEndDate(new YearMonthDay(2022, 11, 30));

            final Date result = accountability.getEndDateInDateType();
            assertNotNull(result);
            return null;
        });
    }

    @Test
    public void testGetEndDateInDateType_nullEndDate() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent19");
            final Person child = createPerson("Child", "child19");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertNull(accountability.getEndDateInDateType());
            return null;
        });
    }

    @Test
    public void testBelongsToPeriod_withinPeriod_returnsTrue() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent20");
            final Person child = createPerson("Child", "child20");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2020, 1, 1));
            accountability.setEndDate(new YearMonthDay(2022, 12, 31));

            assertTrue(accountability.belongsToPeriod(new YearMonthDay(2021, 6, 1), new YearMonthDay(2021, 6, 30)));
            return null;
        });
    }

    @Test
    public void testBelongsToPeriod_outsidePeriod_returnsFalse() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent21");
            final Person child = createPerson("Child", "child21");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            accountability.setBeginDate(new YearMonthDay(2020, 1, 1));
            accountability.setEndDate(new YearMonthDay(2020, 12, 31));

            assertFalse(accountability.belongsToPeriod(new YearMonthDay(2021, 1, 1), new YearMonthDay(2021, 12, 31)));
            return null;
        });
    }

    @Test
    public void testDelete() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            final Person parent = createPerson("Parent", "parent22");
            final Person child = createPerson("Child", "child22");
            final AccountabilityType type = AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE);

            final Accountability accountability = new Accountability(parent, child, type);
            assertNotNull(accountability);

            accountability.delete();

            assertNull(accountability.getAccountabilityType());
            assertNull(accountability.getChildParty());
            assertNull(accountability.getParentParty());
            return null;
        });
    }
}
