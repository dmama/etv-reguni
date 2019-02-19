package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheAnnulationDeclaration;

public abstract class TacheAnnulationDeclarationValidator<T extends TacheAnnulationDeclaration> extends TacheValidator<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			if (tache.getDeclaration() == null) {
				vr.addError("La déclaration associée à la tâche d'annulation ne peut pas être nulle.");
			}
		}
		return vr;
	}
}
