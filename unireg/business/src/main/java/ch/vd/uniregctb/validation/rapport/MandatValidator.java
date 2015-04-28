package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class MandatValidator extends RapportEntreTiersValidator<Mandat> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le mandataire n'existe pas");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le mandant n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique) && !(sujet instanceof Entreprise)) {
			vr.addError("Le mandant n'est ni une personne physique ni une entreprise");
		}
	}

	@Override
	protected Class<Mandat> getValidatedClass() {
		return Mandat.class;
	}
}
