package ch.vd.uniregctb.norentes;

import java.io.Serializable;

import ch.vd.uniregctb.norentes.common.NorentesFactory;
import ch.vd.uniregctb.norentes.common.NorentesScenario;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class NorentesBean implements Serializable {


	/**
	 *
	 */
	private static final long serialVersionUID = 1551892414929763343L;
	private String currentScenarioName;
	private TypeEvenementCivil currentEvenementCivil;

	public NorentesBean() {

		NorentesScenario s = NorentesFactory.getNorentesManager().getCurrentScenario();
		if ( s != null) {
			currentScenarioName = s.getName();
		}
	}



	/**
	 * @return the currentIndexEtape
	 */
	public int getCurrentIndexEtape() {
		return NorentesFactory.getNorentesManager().getCurrentEtape();
	}

	/**
	 * @return the currentScenarioName
	 */
	public String getCurrentScenarioName() {
		return currentScenarioName;
	}

	/**
	 * @param currentScenarioName the currentScenarioName to set
	 */
	public void setCurrentScenarioName(String currentScenarioName) {
		this.currentScenarioName = currentScenarioName;
	}

	/**
	 * @return the currentEvenementCivil
	 */
	public TypeEvenementCivil getCurrentEvenementCivil() {
		return currentEvenementCivil;
	}

	/**
	 * @param currentEvenementCivil the currentEvenementCivil to set
	 */
	public void setCurrentEvenementCivil(TypeEvenementCivil currentEvenementCivil) {
		this.currentEvenementCivil = currentEvenementCivil;
	}

}
