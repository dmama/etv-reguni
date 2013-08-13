package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Filiation;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class FiliationValidator extends RapportEntreTiersValidator<Filiation> {

	@Override
	protected Class<Filiation> getValidatedClass() {
		return Filiation.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);

		if (objet == null) {
			vr.addError("Le contribuable enfant n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers enfant %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);

		if (sujet == null) {
			vr.addError("Le contribuable parent n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers parent %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}
}
