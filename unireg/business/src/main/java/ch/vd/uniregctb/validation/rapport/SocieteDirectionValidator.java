package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.Tiers;

public class SocieteDirectionValidator extends RapportEntreTiersValidator<SocieteDirection> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le fonds de placement n'existe pas");
		}
		else if (!(objet instanceof Entreprise)) {
			vr.addError("Le fonds de placement n'est pas une entreprise");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("L'entreprise propriétaire du fonds n'existe pas");
		}
		else if (!(sujet instanceof Entreprise)) {
			vr.addError("L'entité propriétaire du fonds n'est pas une entreprise");
		}
	}

	@Override
	protected Class<SocieteDirection> getValidatedClass() {
		return SocieteDirection.class;
	}
}
