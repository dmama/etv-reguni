package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.EtatEntreprise;

/**
 * Validateur des états d'entreprise
 */
public class EtatEntrepriseValidator extends DateRangeEntityValidator<EtatEntreprise> {

	@Override
	protected Class<EtatEntreprise> getValidatedClass() {
		return EtatEntreprise.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'état";
	}
}
