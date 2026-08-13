package org.fenixedu.academic.domain.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.Locale;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PhoneTest {

    private static final Comparator<MobilePhone> MOBILE_PHONE_COMPARATOR = MobilePhone.COMPARATOR_BY_NUMBER;
    private static final Comparator<Phone> PHONE_COMPARATOR = Phone.COMPARATOR_BY_NUMBER;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    @Test
    public void testComparatorByNumber() {
        final Person person = createPerson("Person", "person");
        final Phone phoneA = Phone.create(person, "910000001", PartyContactType.PERSONAL, true);
        final Phone phoneB = Phone.create(person, "910000002", PartyContactType.PERSONAL, true);

        assertTrue(PHONE_COMPARATOR.compare(phoneA, phoneB) < 0);
        assertTrue(PHONE_COMPARATOR.compare(phoneB, phoneA) > 0);
        assertEquals(0, PHONE_COMPARATOR.compare(phoneA, phoneA));

        // numbers with equal values are ordered by the contact type (PERSONAL before WORK)
        final Phone phoneAwork = Phone.create(person, "910000001", PartyContactType.WORK, true);
        assertTrue(PHONE_COMPARATOR.compare(phoneA, phoneAwork) < 0);
        assertTrue(PHONE_COMPARATOR.compare(phoneAwork, phoneA) > 0);

        final MobilePhone mobilePhoneA = MobilePhone.create(person, "910000001", PartyContactType.PERSONAL, true);
        final MobilePhone mobilePhoneB = MobilePhone.create(person, "910000002", PartyContactType.PERSONAL, true);

        assertTrue(MOBILE_PHONE_COMPARATOR.compare(mobilePhoneA, mobilePhoneB) < 0);
        assertTrue(MOBILE_PHONE_COMPARATOR.compare(mobilePhoneB, mobilePhoneA) > 0);
        assertEquals(0, MOBILE_PHONE_COMPARATOR.compare(mobilePhoneA, mobilePhoneA));

        // numbers with equal values are ordered by the contact type (PERSONAL before WORK)
        final MobilePhone mobilePhoneAwork = MobilePhone.create(person, "910000001", PartyContactType.WORK, true);
        assertTrue(MOBILE_PHONE_COMPARATOR.compare(mobilePhoneA, mobilePhoneAwork) < 0);
        assertTrue(MOBILE_PHONE_COMPARATOR.compare(mobilePhoneAwork, mobilePhoneA) > 0);
    }

    @Test
    public void testCreatePhonesContacts() {
        final Person person = createPerson("Person", "phones.person");

        final MobilePhone mobilePhone = MobilePhone.create(person, "910000001", PartyContactType.PERSONAL, true, true);
        assertNotNull(mobilePhone);
        assertTrue(mobilePhone.hasValue("910000001"));
        assertTrue(mobilePhone.getVisibleToPublic());
        assertTrue(mobilePhone.getVisibleToStudents());
        assertTrue(mobilePhone.getVisibleToStaff());

        // null for empty number
        assertNull(MobilePhone.create(person, null, PartyContactType.PERSONAL, true));
        assertNull(MobilePhone.create(person, "", PartyContactType.PERSONAL, true));

        final Phone phone = Phone.create(person, "210000001", PartyContactType.PERSONAL, true, true);
        assertNotNull(phone);
        assertTrue(phone.hasValue("210000001"));
        assertTrue(phone.getVisibleToPublic());
        assertTrue(phone.getVisibleToStudents());
        assertTrue(phone.getVisibleToStaff());

        // null for empty number
        assertNull(Phone.create(person, null, PartyContactType.PERSONAL, true, true));
        assertNull(Phone.create(person, "", PartyContactType.PERSONAL, true, true));
    }

    @Test
    public void testFindOrCreateMobilePhone() {
        final Person person = createPerson("Person", "mobile.person");
        final String number = "910000001";

        // null for empty number
        assertNull(MobilePhone.findOrCreate(person, null, PartyContactType.PERSONAL, true));
        assertNull(MobilePhone.findOrCreate(person, "", PartyContactType.PERSONAL, true));

        final MobilePhone first = MobilePhone.findOrCreate(person, number, PartyContactType.PERSONAL, true);
        assertNotNull(first);
        assertTrue(first.hasValue(number));

        // while pending validation the existing contact is not reused
        final MobilePhone second = MobilePhone.findOrCreate(person, number, PartyContactType.PERSONAL, true);
        assertNotEquals(first, second);
        assertTrue(second.hasValue(number));

        // once active and valid, the existing contact is returned instead of creating a new one
        second.setValid();
        final MobilePhone found = MobilePhone.findOrCreate(person, number, PartyContactType.PERSONAL, true);
        assertEquals(second, found);
    }

    @Test
    public void testSetNumber() {
        final Person person = createPerson("Person", "set.number.person");

        final Phone phone = Phone.create(person, "910000001", PartyContactType.PERSONAL, true);
        assertThrows(DomainException.class, () -> phone.setNumber(null));
        assertThrows(DomainException.class, () -> phone.setNumber(""));
        phone.setNumber("910000002");
        assertEquals("910000002", phone.getNumber());

        final MobilePhone mobilePhone = MobilePhone.create(person, "910000003", PartyContactType.PERSONAL, true);
        assertThrows(DomainException.class, () -> mobilePhone.setNumber(null));
        assertThrows(DomainException.class, () -> mobilePhone.setNumber(""));
        mobilePhone.setNumber("910000004");
        assertEquals("910000004", mobilePhone.getNumber());
    }
}
