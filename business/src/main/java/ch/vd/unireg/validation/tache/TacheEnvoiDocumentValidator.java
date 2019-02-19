package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiDocument;

public abstract class TacheEnvoiDocumentValidator<T extends TacheEnvoiDocument> extends TacheValidator<T> {

	@Override
	@NotNull
	public ValidationResults validate(T tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			if (tache.getTypeDocument() == null) {
				vr.addError("Le type de document ne peut pas être nul.");
			}
		}
		return vr;
	}
}
