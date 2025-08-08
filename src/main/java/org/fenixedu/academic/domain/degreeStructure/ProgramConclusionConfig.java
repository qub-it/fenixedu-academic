package org.fenixedu.academic.domain.degreeStructure;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProgramConclusionConfig extends ProgramConclusionConfig_Base {
    
    protected ProgramConclusionConfig() {
        super();
        super.setRootDomainObject(Bennu.getInstance());
    }

    public static ProgramConclusionConfig create(LocalizedString conclusionTitle, DegreeCurricularPlan degreeCurricularPlan,
            ProgramConclusion programConclusion) {
        ProgramConclusionConfig config = new ProgramConclusionConfig();
        config.setConclusionTitle(conclusionTitle);
        config.setDegreeCurricularPlan(degreeCurricularPlan);
        config.setProgramConclusion(programConclusion);
        config.changeConfigOrder(degreeCurricularPlan.getProgramConclusionConfigsSet().size() - 1);

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
    public void setConfigOrder(final int configOrder) {
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

    protected void changeConfigOrder(int order) {
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
        if (currentIndex == getDegreeCurricularPlan().getProgramConclusionConfigsSet().size() - 1) {
            return;
        }

        final ProgramConclusionConfig toChange = findAtPosition(getDegreeCurricularPlan(), currentIndex + 1);
        toChange.changeConfigOrder(currentIndex);
        changeConfigOrder(currentIndex + 1);
    }

    public void moveBottom() {
        while (getConfigOrder() < getDegreeCurricularPlan().getProgramConclusionConfigsSet().size() - 1) {
            moveDown();
        }
    }

    public void delete() {
        if (!getConclusionProcessesSet().isEmpty()) {
            throw new DomainException("error.ProgramConclusionConfig.cannot.delete.with.related.conclusionProcesses");
        }

        moveBottom();

        setProgramConclusion(null);
        setDegreeCurricularPlan(null);
        getIncludedModulesSet().clear();
        getExcludedModulesSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    private static ProgramConclusionConfig findAtPosition(final DegreeCurricularPlan degreeCurricularPlan, final int order) {
        return degreeCurricularPlan.getProgramConclusionConfigsSet().stream().filter(c -> c.getConfigOrder() == order).findFirst()
                .orElse(null);
    }
}
