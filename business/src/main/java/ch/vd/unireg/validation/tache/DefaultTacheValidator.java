package ch.vd.uniregctb.validation.tache;

import ch.vd.uniregctb.tiers.Tache;

public final class DefaultTacheValidator extends TacheValidator<Tache> {

	@Override
	protected Class<Tache> getValidatedClass() {
		return Tache.class;
	}
}
