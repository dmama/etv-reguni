package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

public  class AssujettissementParSubstitutionValidator extends RapportEntreTiersValidator<AssujettissementParSubstitution> {

	@Override
	protected void verificationClasses(ValidationResults vr, AssujettissementParSubstitution ret) {
		super.verificationClasses(vr, ret);
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le tiers dont l'assujetissement se substitue n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError("Le tiers dont l'assujetissement se substitue n'est pas une personne physique");
		}
	}
	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le tiers sous assujettissement substitué n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) { // [SIFISC-719]
			vr.addError("Le tiers sous assujettissement substitué n'est pas une personne physique");
		}
	}

	@Override
	protected Class<AssujettissementParSubstitution> getValidatedClass() {
		return AssujettissementParSubstitution.class;
	}
}
