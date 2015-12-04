package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Tiers;

public class FusionEntreprisesValidator extends RapportEntreTiersValidator<FusionEntreprises> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("L'entreprise après fusion n'existe pas");
		}
		else if (!(objet instanceof Entreprise)) {
			vr.addError("L'entité après fusion n'est pas une entreprise");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("L'entreprise avant fusion n'existe pas");
		}
		else if (!(sujet instanceof Entreprise)) {
			vr.addError("L'entité avant fusion n'est pas une entreprise");
		}
	}

	@Override
	protected Class<FusionEntreprises> getValidatedClass() {
		return FusionEntreprises.class;
	}
}
