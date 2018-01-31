package ch.vd.uniregctb.validation.tache;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.TacheEnvoiDocument;

public abstract class TacheEnvoiDocumentValidator<T extends TacheEnvoiDocument> extends TacheValidator<T> {

	@Override
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
