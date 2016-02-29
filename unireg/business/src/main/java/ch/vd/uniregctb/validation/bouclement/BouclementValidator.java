package ch.vd.uniregctb.validation.bouclement;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class BouclementValidator extends EntityValidatorImpl<Bouclement> {

	@Override
	protected Class<Bouclement> getValidatedClass() {
		return Bouclement.class;
	}

	@Override
	public ValidationResults validate(Bouclement bouclement) {
		final ValidationResults vr = new ValidationResults();
		if (!bouclement.isAnnule()) {

			// la date de début est obligatoire
			if (bouclement.getDateDebut() == null) {
				vr.addError("La date de début est obligatoire sur une donnée de bouclement.");
			}

			// l'ancrage aussi
			if (bouclement.getAncrage() == null) {
				vr.addError("L'ancrage est obligatoire sur une donnée de bouclement.");
			}

			// la période est non seulement obligatoire, mais bornée
			if (bouclement.getPeriodeMois() < 1 || bouclement.getPeriodeMois() > 99) {
				vr.addError("La période (en mois) doit être comprise entre 1 et 99 (" + bouclement.getPeriodeMois() + ").");
			}
		}
		return vr;
	}
}
