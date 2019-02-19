package ch.vd.unireg.validation.tiers;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.CapitalFiscalEntreprise;
import ch.vd.unireg.tiers.MontantMonetaire;

public class CapitalFiscalEntrepriseValidator extends DonneeCivileEntrepriseValidator<CapitalFiscalEntreprise> {

	@Override
	protected String getEntityCategoryName() {
		return "Le capital";
	}

	@Override
	protected Class<CapitalFiscalEntreprise> getValidatedClass() {
		return CapitalFiscalEntreprise.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull CapitalFiscalEntreprise capital) {
		final ValidationResults vr = super.validate(capital);
		if (!capital.isAnnule()) {
			final MontantMonetaire montant = capital.getMontant();
			if (montant == null) {
				vr.addError("Le capital d'une entreprise doit être composé d'un montant et d'une monnaie (tous deux vides ici).");
			}
			else {
				// capital sans monnaie ou montant -> erreur
				if (StringUtils.isBlank(montant.getMonnaie()) || montant.getMontant() == null) {
					vr.addError(String.format("Le capital d'une entreprise doit être composé d'un montant (%s ici) et d'une monnaie ('%s' ici).",
					                          montant.getMontant() != null ? String.valueOf(montant.getMontant()) : "vide",
					                          StringUtils.trimToEmpty(montant.getMonnaie())));
				}
				else if (montant.getMonnaie().length() > 3) {
					vr.addError(String.format("La monnaie du capital doit être exprimée par un code ISO à 3 lettres ('%s' ici).", montant.getMonnaie()));
				}
				else if (montant.getMontant() < 0) {
					vr.addError(String.format("Le capital d'une entreprise ne peut être négatif (%d).", montant.getMontant()));
				}
			}
		}
		return vr;
	}
}
