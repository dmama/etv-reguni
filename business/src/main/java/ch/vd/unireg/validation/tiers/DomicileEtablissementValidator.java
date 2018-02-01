package ch.vd.unireg.validation.tiers;

import ch.vd.unireg.tiers.DomicileEtablissement;

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
