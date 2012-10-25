package ch.vd.uniregctb.supergra;

import ch.vd.registre.base.validation.ValidationResults;

/**
 * Etat d'un tiers modifié lors d'une session SuperGra.
 */
public class TiersState {

	private final EntityKey key;
	private final ValidationResults validationResults;

	public TiersState(EntityKey key, ValidationResults validationResults) {
		this.key = key;
		this.validationResults = validationResults;
	}

	public EntityKey getKey() {
		return key;
	}

	public ValidationResults getValidationResults() {
		return validationResults;
	}

	/**
	 * @return <b>vrai</b> si le tiers ne possède ni erreur ni warning.
	 */
	public boolean isValid() {
		return !validationResults.hasErrors() && !validationResults.hasWarnings();
	}

	/**
	 * @return <b>vrai</b> si le tiers est en erreur
	 */
	public boolean isInError() {
		return validationResults.hasErrors();
	}
}
