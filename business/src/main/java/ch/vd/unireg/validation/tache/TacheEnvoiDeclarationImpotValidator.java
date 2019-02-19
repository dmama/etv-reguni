package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;

public abstract class TacheEnvoiDeclarationImpotValidator<T extends TacheEnvoiDeclarationImpot> extends TacheEnvoiDocumentValidator<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			ValidationHelper.validate(tache, false, false, vr);
		}
		return vr;
	}
}
