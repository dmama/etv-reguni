package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ScissionEntreprise;
import ch.vd.unireg.tiers.Tiers;

public class ScissionEntrepriseValidator extends RapportEntreTiersValidator<ScissionEntreprise> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("L'entreprise après scission n'existe pas");
		}
		else if (!(objet instanceof Entreprise)) {
			vr.addError("L'entité après scission n'est pas une entreprise");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("L'entreprise avant scission n'existe pas");
		}
		else if (!(sujet instanceof Entreprise)) {
			vr.addError("L'entité avant scission n'est pas une entreprise");
		}
	}

	@Override
	protected Class<ScissionEntreprise> getValidatedClass() {
		return ScissionEntreprise.class;
	}
}
