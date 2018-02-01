package ch.vd.unireg.validation.tiers;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;

public class FormeJuridiqueFiscaleEntrepriseValidator extends DonneeCivileEntrepriseValidator<FormeJuridiqueFiscaleEntreprise> {

	@Override
	protected String getEntityCategoryName() {
		return "La forme juridique";
	}

	@Override
	protected Class<FormeJuridiqueFiscaleEntreprise> getValidatedClass() {
		return FormeJuridiqueFiscaleEntreprise.class;
	}

	@Override
	public ValidationResults validate(FormeJuridiqueFiscaleEntreprise entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (entity.getFormeJuridique() == null) {
				vr.addError("La forme juridique est une donnée obligatoire.");
			}
		}
		return vr;
	}
}
