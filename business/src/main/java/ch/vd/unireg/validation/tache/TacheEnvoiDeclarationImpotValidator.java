package ch.vd.unireg.validation.tache;

import ch.vd.registre.base.validation.ValidationHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;

public abstract class TacheEnvoiDeclarationImpotValidator<T extends TacheEnvoiDeclarationImpot> extends TacheEnvoiDocumentValidator<T> {

	@Override
	public ValidationResults validate(T tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			ValidationHelper.validate(tache, false, false, vr);
		}
		return vr;
	}
}
