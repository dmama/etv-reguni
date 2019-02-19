package ch.vd.unireg.validation.complements;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class CoordonneesFinancieresValidator extends DateRangeEntityValidator<CoordonneesFinancieres> {
	@Override
	protected Class<CoordonneesFinancieres> getValidatedClass() {
		return CoordonneesFinancieres.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Les coordonnées financières";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}

	@Override
	@NotNull
	public ValidationResults validate(CoordonneesFinancieres entity) {
		final ValidationResults results = super.validate(entity);

		// une entité annulée est toujours valide...
		if (entity.isAnnule()) {
			return results;
		}

		final CompteBancaire compteBancaire = entity.getCompteBancaire();

		// il doit au moins y avoir une donnée
		if (StringUtils.isBlank(entity.getTitulaire()) &&
				(compteBancaire == null || StringUtils.isBlank(compteBancaire.getIban())) &&
				(compteBancaire == null || StringUtils.isBlank(compteBancaire.getBicSwift()))) {
			results.addError("Au minimum le titulaire, l'IBAN ou le code BicSwift doit être renseigné.");
		}

		return results;
	}
}
