package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;

public class DonneesRegistreCommerceValidator extends DateRangeEntityValidator<DonneesRegistreCommerce> {

	@Override
	protected String getEntityCategoryName() {
		return "L'ensemble de données du registre du commerce";
	}

	@Override
	protected Class<DonneesRegistreCommerce> getValidatedClass() {
		return DonneesRegistreCommerce.class;
	}
}
