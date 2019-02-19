package ch.vd.unireg.validation.tiers;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;

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
	@NotNull
	public ValidationResults validate(@NotNull RaisonSocialeFiscaleEntreprise entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (StringUtils.isBlank(entity.getRaisonSociale())) {
				vr.addError("La raison sociale est une donnée obligatoire.");
			}
		}
		return vr;
	}
}
