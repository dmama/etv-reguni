package ch.vd.unireg.norentes.common;

import java.util.Collection;

import ch.vd.unireg.norentes.common.NorentesContext.EtapeContext;
import ch.vd.unireg.type.TypeEvenementCivil;

public interface NorentesManager {

	boolean isActif();

	NorentesScenario getCurrentScenario() ;
	NorentesScenario getScenario( String name);

	void closeCurrentScenario() throws Exception;

	void runFirst(NorentesScenario scenario);

	void runToStep(NorentesScenario scenario, int step) ;
	void runToLast(NorentesScenario scenario);

	Collection<NorentesScenario> getScenaries(TypeEvenementCivil evenementCivil);
	TypeEvenementCivil[] getEvenementCivilsUsedForTest();

	int getCurrentEtape();

	int getCountEtape();

	EtapeContext getCurrentEtapeContext();

	EtapeContext getEtapeContext(NorentesScenario scenario, int index);

}
