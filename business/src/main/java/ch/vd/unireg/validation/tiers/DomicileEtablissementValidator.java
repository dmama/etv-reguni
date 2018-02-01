package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.DomicileEtablissement;

public class DomicileEtablissementValidator extends LocalisationDateeValidator<DomicileEtablissement> {

	@Override
	protected String getEntityCategoryName() {
		return "Le domicile d'établissement";
	}

	@Override
	protected Class<DomicileEtablissement> getValidatedClass() {
		return DomicileEtablissement.class;
	}
}
