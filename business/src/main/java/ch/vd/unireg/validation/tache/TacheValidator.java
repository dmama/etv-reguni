package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.EntityValidatorImpl;

public abstract class TacheValidator<T extends Tache> extends EntityValidatorImpl<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T tache) {
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
