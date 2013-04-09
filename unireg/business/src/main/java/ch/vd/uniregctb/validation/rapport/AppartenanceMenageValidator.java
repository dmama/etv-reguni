package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class AppartenanceMenageValidator extends RapportEntreTiersValidator<AppartenanceMenage> {

	@Override
	protected Class<AppartenanceMenage> getValidatedClass() {
		return AppartenanceMenage.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le ménage commun n'existe pas");
		}
		else if (!(objet instanceof MenageCommun)) {
			vr.addError(String.format("Le tiers %s n'est pas un ménage commun", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("La personne physique n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}
}
