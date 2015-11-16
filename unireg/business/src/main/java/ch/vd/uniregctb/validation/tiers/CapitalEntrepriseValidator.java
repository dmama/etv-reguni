package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.CapitalEntreprise;
import ch.vd.uniregctb.tiers.MontantMonetaire;

public class CapitalEntrepriseValidator extends DateRangeEntityValidator<CapitalEntreprise> {

	@Override
	protected String getEntityCategoryName() {
		return "La surcharge de capital";
	}

	@Override
	protected Class<CapitalEntreprise> getValidatedClass() {
		return CapitalEntreprise.class;
	}

	@Override
	public ValidationResults validate(CapitalEntreprise capital) {
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
