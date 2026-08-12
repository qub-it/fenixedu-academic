package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.Locale;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class JobTest {

    private static final Comparator<Job> COMPARATOR_BY_BEGIN_DATE = Job.COMPARATOR_BY_BEGIN_DATE;

    private static Person person;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            CountryTest.initCountries();

            person = createPerson("Job Person", "job.person");
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    private static Job createJobWithBeginDate(final LocalDate beginDate) {
        return Job.create(person, null, null, null, "Researcher", beginDate, null, null, null, null);
    }

    @Test
    public void testComparatorByBeginDate() {
        Job jobOlder = createJobWithBeginDate(new LocalDate(2010, 1, 1));
        Job jobNewer = createJobWithBeginDate(new LocalDate(2020, 1, 1));
        assertTrue(COMPARATOR_BY_BEGIN_DATE.compare(jobNewer, jobOlder) > 0);
        assertTrue(COMPARATOR_BY_BEGIN_DATE.compare(jobOlder, jobNewer) < 0);
        assertEquals(0, COMPARATOR_BY_BEGIN_DATE.compare(jobOlder, jobOlder));

        // null begin date
        Job jobWithoutBeginDate = createJobWithBeginDate(null);
        assertTrue(COMPARATOR_BY_BEGIN_DATE.compare(jobWithoutBeginDate, jobNewer) < 0);
        assertTrue(COMPARATOR_BY_BEGIN_DATE.compare(jobNewer, jobWithoutBeginDate) > 0);
        assertTrue(COMPARATOR_BY_BEGIN_DATE.compare(jobOlder, jobWithoutBeginDate) > 0);
    }

    @Test
    public void testCreate() {
        Country portugal = Country.readByTwoLetterCode("PT");
        LocalDate beginDate = new LocalDate(2015, 1, 1);
        LocalDate endDate = new LocalDate(2015, 12, 31);

        Job job = Job.create(person, "IST", "Lisbon", portugal, "Researcher", beginDate, endDate, JobApplicationType.ANNOUNCEMENT,
                ContractType.EFFECTIVE, 1500.0);

        assertEquals("IST", job.getEmployerName());
        assertEquals("Lisbon", job.getCity());
        assertEquals(portugal, job.getCountry());
        assertEquals("Researcher", job.getPosition());
        assertEquals(beginDate, job.getBeginDate());
        assertEquals(endDate, job.getEndDate());
        assertEquals(JobApplicationType.ANNOUNCEMENT, job.getJobApplicationType());
        assertEquals(ContractType.EFFECTIVE, job.getContractType());
        assertEquals(Double.valueOf(1500.0), job.getSalary());
        assertEquals(person, job.getPerson());

        // null person, throws
        DomainException e = assertThrows(DomainException.class,
                () -> Job.create(null, "IST", "Lisbon", portugal, "Researcher", beginDate, endDate,
                        JobApplicationType.ANNOUNCEMENT, ContractType.EFFECTIVE, 1500.0));
        assertEquals("job.creation.person.null", e.getMessage());

        // all fields null, throws
        e = assertThrows(DomainException.class, () -> Job.create(person, null, null, null, null, null, null, null, null, null));
        assertEquals("job.creation.allFields.null", e.getMessage());

        // begin date after end date, throws
        final LocalDate beginDateAfterEndDate = endDate.plusDays(1);
        e = assertThrows(DomainException.class,
                () -> Job.create(person, null, null, null, null, beginDateAfterEndDate, endDate, null, null, null));
        assertEquals("job.creation.beginDate.after.endDate", e.getMessage());
    }
}