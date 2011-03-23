package ch.vd.uniregctb.validation.tache;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class TacheValidator<T extends Tache> extends EntityValidatorImpl<T> {

	public ValidationResults validate(T tache) {
		final ValidationResults vr = new ValidationResults();
		if (!tache.isAnnule()) {
			final CollectiviteAdministrative collAdm = tache.getCollectiviteAdministrativeAssignee();
			final TypeEtatTache etat = tache.getEtat();
			if (collAdm == null && etat != TypeEtatTache.TRAITE) {
				vr.addError("La collectivité assignée doit être renseignée.");
			}
			if (tache.getContribuable() == null) {
				vr.addError("La tâche doit être associée à un contribuable.");
			}
		}
		return vr;
	}
}
