package ch.vd.unireg.validation.tiers;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.validation.EntityValidatorImpl;

/**
 * Validateur des états d'entreprise
 */
public class EtatEntrepriseValidator extends EntityValidatorImpl<EtatEntreprise> {

	@Override
	protected Class<EtatEntreprise> getValidatedClass() {
		return EtatEntreprise.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull EtatEntreprise entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {
			if (entity.getDateObtention() == null) {
				vr.addError("La date d'obtention est obligatoire sur un état d'entreprise.");
			}
			if (entity.getType() == null) {
				vr.addError("Le type d'état d'entreprise est obligatoire.");
			}
			if (entity.getGeneration() == null) {
				vr.addError("Le type de génération est obligatoire.");
			}
		}
		return vr;
	}
}
