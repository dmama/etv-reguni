package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.PrincipalCommunauteRF;
import ch.vd.uniregctb.validation.EntityValidatorImpl;
import ch.vd.uniregctb.validation.ValidationService;
import ch.vd.uniregctb.validation.tiers.TiersValidator;

public class ModeleCommunauteRFValidator extends EntityValidatorImpl<ModeleCommunauteRF> {

	@Override
	protected Class<ModeleCommunauteRF> getValidatedClass() {
		return ModeleCommunauteRF.class;
	}

	@Override
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
