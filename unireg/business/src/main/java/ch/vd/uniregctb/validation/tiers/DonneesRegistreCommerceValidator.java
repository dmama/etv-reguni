package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.MontantMonetaire;

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

			// capital
			final MontantMonetaire capital = drc.getCapital();
			if (capital != null) {
				// capital sans monnaie ou montant -> erreur
				if (StringUtils.isBlank(capital.getMonnaie()) || capital.getMontant() == null) {
					vr.addError(String.format("Le capital d'une entreprise doit être composé d'un montant (%s ici) et d'une monnaie ('%s' ici).",
					                          capital.getMontant() != null ? String.valueOf(capital.getMontant()) : "vide",
					                          StringUtils.trimToEmpty(capital.getMonnaie())));
				}
				else if (capital.getMontant() < 0) {
					vr.addError(String.format("Le capital d'une entreprise ne peut être négatif (%d).", capital.getMontant()));
				}
			}

			// TODO capital null autorisé ?
		}
		return vr;
	}
}
