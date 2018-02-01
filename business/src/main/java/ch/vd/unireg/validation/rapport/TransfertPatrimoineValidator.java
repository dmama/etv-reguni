package ch.vd.unireg.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TransfertPatrimoine;

public class TransfertPatrimoineValidator extends RapportEntreTiersValidator<TransfertPatrimoine> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("L'entreprise réceptrice de patrimoine n'existe pas");
		}
		else if (!(objet instanceof Entreprise)) {
			vr.addError("L'entité réceptrice de patrimoine n'est pas une entreprise");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("L'entreprise émettrice de patrimoine n'existe pas");
		}
		else if (!(sujet instanceof Entreprise)) {
			vr.addError("L'entité émettrice de patrimoine n'est pas une entreprise");
		}
	}

	@Override
	protected Class<TransfertPatrimoine> getValidatedClass() {
		return TransfertPatrimoine.class;
	}
}
