package ch.vd.unireg.validation.fors;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.ForFiscalAvecMotifs;

public abstract class ForFiscalAvecMotifsValidator<T extends ForFiscalAvecMotifs> extends ForFiscalValidator<T> {

	@Override
	public ValidationResults validate(T ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {
			if (ff.getMotifOuverture() != null && ff.getDateDebut() == null) {
				vr.addError("Une date d'ouverture doit être indiquée si un motif d'ouverture l'est.");
			}
			if (ff.getMotifFermeture() != null && ff.getDateFin() == null) {
				vr.addError("Une date de fermeture doit être indiquée si un motif de fermeture l'est.");
			}
		}
		return vr;
	}
}
