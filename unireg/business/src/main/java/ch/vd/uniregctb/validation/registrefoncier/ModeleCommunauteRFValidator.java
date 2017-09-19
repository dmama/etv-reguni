package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class ModeleCommunauteRFValidator extends EntityValidatorImpl<ModeleCommunauteRF> {

	@Override
	protected Class<ModeleCommunauteRF> getValidatedClass() {
		return ModeleCommunauteRF.class;
	}

	@Override
	public ValidationResults validate(ModeleCommunauteRF entity) {

		final ValidationResults results = new ValidationResults();

		// Note: si la collection est nulle, c'est qu'on est certainement dans une première phase du save() d'hibernate
		// où l'entité est créée en DB : on peut ignorer le test sur le hashCode car cette méthode sera appelée à nouveau
		// lors de la seconde phase quand les collections sont ajoutées.
		final Set<AyantDroitRF> membres = entity.getMembres();
		if (membres != null) {
			if (entity.getMembresHashCode() != ModeleCommunauteRF.hashCode(membres)) {
				results.addError("Le code de hashage n'est pas correct");
			}
		}

		return results;
	}
}
