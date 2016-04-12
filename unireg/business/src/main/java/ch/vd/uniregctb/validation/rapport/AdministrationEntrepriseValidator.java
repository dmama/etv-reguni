package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class AdministrationEntrepriseValidator extends RapportEntreTiersValidator<AdministrationEntreprise> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("L'administrateur n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError("L'administrateur n'est pas une personne physique");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("L'entreprise administrée n'existe pas");
		}
		else if (!(sujet instanceof Entreprise)) {
			vr.addError("L'entité administrée n'est pas une entreprise");
		}
	}

	@Override
	protected Class<AdministrationEntreprise> getValidatedClass() {
		return AdministrationEntreprise.class;
	}
}
