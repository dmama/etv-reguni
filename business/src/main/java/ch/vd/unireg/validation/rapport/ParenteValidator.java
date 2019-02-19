package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

public class ParenteValidator extends RapportEntreTiersValidator<Parente> {

	@Override
	protected Class<Parente> getValidatedClass() {
		return Parente.class;
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);

		if (sujet == null) {
			vr.addError("Le contribuable enfant n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers enfant %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);

		if (objet == null) {
			vr.addError("Le contribuable parent n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers parent %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}
}
