package org.fenixedu.academic.domain.degreeStructure;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProgramConclusionConfig extends ProgramConclusionConfig_Base {
    
    public ProgramConclusionConfig() {
        super();
        super.setRootDomainObject(Bennu.getInstance());
    }

    public static ProgramConclusionConfig create(Integer configOrder, LocalizedString conclusionTitle) {
        ProgramConclusionConfig config = new ProgramConclusionConfig();
        config.setConfigOrder(configOrder);
        config.setConclusionTitle(conclusionTitle);

        return config;
    }

    public void delete() {
        if (getProgramConclusion() != null) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.programConclusion");
        }
        if (getDegreeCurricularPlan() != null) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.degreeCurricularPlan");
        }
        if (!getConclusionProcessesSet().isEmpty()) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.conclusionProcesses");
        }
        if (!getIncludedModulesSet().isEmpty()) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.degreeModules");
        }

        getExcludedModulesSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Stream<ProgramConclusionConfig> findAll() {
        return Bennu.getInstance().getProgramConclusionConfigsSet().stream();
    }
}
