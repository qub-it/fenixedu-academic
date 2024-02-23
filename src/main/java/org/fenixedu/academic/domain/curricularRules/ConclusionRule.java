package org.fenixedu.academic.domain.curricularRules;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;

public interface ConclusionRule {

    public boolean isConcluded(CurriculumGroup group, ExecutionYear executionYear);

    default public boolean canBeEvaluatedForConclusion(CurriculumGroup group, ExecutionYear executionYear) {
        return true;
    }

    public boolean canConclude(CurriculumGroup group, ExecutionYear executionYear);

}
