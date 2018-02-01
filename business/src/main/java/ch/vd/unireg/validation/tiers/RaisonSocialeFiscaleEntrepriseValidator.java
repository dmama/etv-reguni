package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;

public class RaisonSocialeFiscaleEntrepriseValidator extends DonneeCivileEntrepriseValidator<RaisonSocialeFiscaleEntreprise> {

	@Override
	protected String getEntityCategoryName() {
		return "La raison sociale";
	}

	@Override
	protected Class<RaisonSocialeFiscaleEntreprise> getValidatedClass() {
		return RaisonSocialeFiscaleEntreprise.class;
	}

	@Override
	public ValidationResults validate(RaisonSocialeFiscaleEntreprise entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (StringUtils.isBlank(entity.getRaisonSociale())) {
				vr.addError("La raison sociale est une donnée obligatoire.");
			}
		}
		return vr;
	}
}
