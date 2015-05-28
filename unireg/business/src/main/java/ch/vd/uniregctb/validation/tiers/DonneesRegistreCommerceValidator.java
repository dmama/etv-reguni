package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
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

	@Override
	public ValidationResults validate(DonneesRegistreCommerce drc) {
		final ValidationResults vr = super.validate(drc);
		if (!drc.isAnnule()) {
			if (drc.getFormeJuridique() == null) {
				vr.addError("La forme juridique est une donnée obligatoire pour les informations du registre du commerce.");
			}
			if (StringUtils.isBlank(drc.getRaisonSociale())) {
				vr.addError("La raison sociale est une donnée obligatoire pour les informations du registre du commerce.");
			}

			// TODO capital null autorisé ?
		}
		return vr;
	}
}
