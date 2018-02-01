package ch.vd.unireg.validation.tache;

import ch.vd.unireg.tiers.Tache;

public final class DefaultTacheValidator extends TacheValidator<Tache> {

	@Override
	protected Class<Tache> getValidatedClass() {
		return Tache.class;
	}
}
