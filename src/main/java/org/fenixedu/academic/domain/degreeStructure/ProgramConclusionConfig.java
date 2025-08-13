package org.fenixedu.academic.domain.degreeStructure;

import java.util.Collection;
import java.util.Collections;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProgramConclusionConfig extends ProgramConclusionConfig_Base {
    
    protected ProgramConclusionConfig() {
        super();
        super.setRootDomainObject(Bennu.getInstance());
    }

    protected void init(LocalizedString conclusionTitle, DegreeCurricularPlan degreeCurricularPlan,
            ProgramConclusion programConclusion) {
        setConclusionTitle(conclusionTitle);
        setDegreeCurricularPlan(degreeCurricularPlan);
        setProgramConclusion(programConclusion);
        changeConfigOrder(degreeCurricularPlan.getProgramConclusionConfigsSet().size() - 1);
    }

    public static ProgramConclusionConfig create(LocalizedString conclusionTitle, DegreeCurricularPlan degreeCurricularPlan,
            ProgramConclusion programConclusion) {
        ProgramConclusionConfig config = new ProgramConclusionConfig();
        config.init(conclusionTitle, degreeCurricularPlan, programConclusion);

        return config;
    }

    public void edit(LocalizedString conclusionTitle, ProgramConclusion programConclusion) {
        setConclusionTitle(conclusionTitle);
        setProgramConclusion(programConclusion);
    }

    @Override
    public void setConclusionTitle(final LocalizedString conclusionTitle) {
        if (conclusionTitle == null || conclusionTitle.isEmpty()) {
            throw new DomainException("error.ProgramConclusionConfig.conclusionTitle.cannot.be.null.or.empty");
        }
        super.setConclusionTitle(conclusionTitle);
    }

    @Override
    public void setDegreeCurricularPlan(final DegreeCurricularPlan degreeCurricularPlan) {
        if (degreeCurricularPlan == null) {
            throw new DomainException("error.ProgramConclusionConfig.degreeCurricularPlan.cannot.be.null");
        }
        super.setDegreeCurricularPlan(degreeCurricularPlan);
    }

    @Override
    public void setProgramConclusion(final ProgramConclusion programConclusion) {
        if (programConclusion == null) {
            throw new DomainException("error.ProgramConclusionConfig.programConclusion.cannot.be.null");
        }
        if (!getConclusionProcessesSet().isEmpty()) {
            throw new DomainException(
                    "error.ProgramConclusionConfig.cannot.change.programConclusion.with.related.conclusionProcesses");
        }
        super.setProgramConclusion(programConclusion);
    }

    @Override
    public void setConfigOrder(final Integer configOrder) {
        throw new DomainException("error.order.change.should.be.done.using.move.methods");
    }

    @Override
    public void addIncludedModules(final DegreeModule includedModule) {
        if (getExcludedModulesSet().contains(includedModule)) {
            throw new DomainException("error.ProgramConclusionConfig.degreeModule.is.excluded",
                    includedModule.getNameI18N().getContent());
        }
        super.addIncludedModules(includedModule);
    }

    @Override
    public void addExcludedModules(final DegreeModule excludedModule) {
        if (getIncludedModulesSet().contains(excludedModule)) {
            throw new DomainException("error.ProgramConclusionConfig.degreeModule.is.included",
                    excludedModule.getNameI18N().getContent());
        }
        super.addExcludedModules(excludedModule);
    }

    protected void changeConfigOrder(Integer order) {
        super.setConfigOrder(order);
    }

    public void moveUp() {
        final int currentIndex = getConfigOrder();
        if (currentIndex == 0) {
            return;
        }

        final ProgramConclusionConfig toChange = findAtPosition(getDegreeCurricularPlan(), currentIndex - 1);
        toChange.changeConfigOrder(currentIndex);
        changeConfigOrder(currentIndex - 1);
    }

    public void moveTop() {
        while (getConfigOrder() != 0) {
            moveUp();
        }
    }

    public void moveDown() {
        final int currentIndex = getConfigOrder();
        if (currentIndex == find(getDegreeCurricularPlan()).size() - 1) {
            return;
        }

        final ProgramConclusionConfig toChange = findAtPosition(getDegreeCurricularPlan(), currentIndex + 1);
        toChange.changeConfigOrder(currentIndex);
        changeConfigOrder(currentIndex + 1);
    }

    public void moveBottom() {
        while (getConfigOrder() < find(getDegreeCurricularPlan()).size() - 1) {
            moveDown();
        }
    }

    public void delete() {
        moveBottom();

        if (!getConclusionProcessesSet().isEmpty()) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.conclusionProcesses");
        }

        super.setProgramConclusion(null);
        super.setDegreeCurricularPlan(null);
        getIncludedModulesSet().clear();
        getExcludedModulesSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static ProgramConclusionConfig findAtPosition(final DegreeCurricularPlan degreeCurricularPlan, final Integer order) {
        return find(degreeCurricularPlan).stream().filter(c -> c.getConfigOrder().equals(order)).findFirst().orElse(null);
    }

    public static Collection<ProgramConclusionConfig> find(final DegreeCurricularPlan degreeCurricularPlan) {
        if (degreeCurricularPlan == null) {
            return Collections.emptySet();
        }

        return degreeCurricularPlan.getProgramConclusionConfigsSet();
    }
}
