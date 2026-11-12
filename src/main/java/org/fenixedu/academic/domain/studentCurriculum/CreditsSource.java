package org.fenixedu.academic.domain.studentCurriculum;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.exceptions.DomainException;

public class CreditsSource extends CreditsSource_Base {

    protected CreditsSource() {
        super();
    }

    CreditsSource(final Credits credits, final IEnrolment source) {
        this();
        super.init(credits);

        if (source instanceof Enrolment) {
            super.setSourceEnrolment((Enrolment) source);
        } else if (source instanceof ExternalEnrolment) {
            super.setSourceExternalEnrolment((ExternalEnrolment) source);
        } else {
            throw new DomainException("error.CreditsSource.unknown.source");
        }
    }

    @Override
    public IEnrolment getIEnrolment() {
        return getSourceEnrolment() != null ? getSourceEnrolment() : getSourceExternalEnrolment();
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkExactlyOneSource() {
        if (getClass() != CreditsSource.class || getCredits() == null) {
            return true;
        }
        return (getSourceEnrolment() == null) != (getSourceExternalEnrolment() == null);
    }

    @Override
    public void delete() {
        super.setSourceEnrolment(null);
        super.setSourceExternalEnrolment(null);
        super.delete();
    }
}