package ch.vd.unireg.validation.registrefoncier;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PrincipalCommunauteRF;
import ch.vd.unireg.validation.EntityValidatorImpl;
import ch.vd.unireg.validation.tiers.TiersValidator;

public class ModeleCommunauteRFValidator extends EntityValidatorImpl<ModeleCommunauteRF> {

	@Override
	protected Class<ModeleCommunauteRF> getValidatedClass() {
		return ModeleCommunauteRF.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(ModeleCommunauteRF entity) {

		final ValidationResults results = new ValidationResults();
		if (entity.isAnnule()) {
			return results;
		}

		// Note: si la collection est nulle, c'est qu'on est certainement dans une première phase du save() d'hibernate
		// où l'entité est créée en DB : on peut ignorer le test sur le hashCode car cette méthode sera appelée à nouveau
		// lors de la seconde phase quand les collections sont ajoutées.
		final Set<AyantDroitRF> membres = entity.getMembres();
		if (membres != null) {
			if (entity.getMembresHashCode() != ModeleCommunauteRF.hashCode(membres)) {
				results.addError("Le code de hashage n'est pas correct");
			}
		}

		results.merge(validatePrincipaux(entity));

		return results;
	}

	private ValidationResults validatePrincipaux(ModeleCommunauteRF modele) {

		final ValidationResults vr = new ValidationResults();

		final Set<PrincipalCommunauteRF> principaux = modele.getPrincipaux();
		if (principaux != null && !principaux.isEmpty()) {
			// chaque principal pour lui-même
			final ValidationService validationService = getValidationService();
			principaux.stream()
					.map(validationService::validate)
					.forEach(vr::merge);

			// puis les chevauchements
			if (!vr.hasErrors()) {
				TiersValidator.checkNonOverlap(principaux,
				                               AnnulableHelper::nonAnnule,
				                               vr,
				                               "principaux non-annulés");
			}
		}

		return vr;
	}
}
