package ch.vd.unireg.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

public class ActiviteEconomiqueValidator extends RapportEntreTiersValidator<ActiviteEconomique> {

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("L'établissement de l'activité économique n'existe pas");
		}
		else if (!(objet instanceof Etablissement)) {
			vr.addError("L'établissement de l'activité économique n'est pas un établissement");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("La personne physique ou morale exerçant l'activité économique n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique) && !(sujet instanceof Entreprise)) { // [SIFISC-719]
			vr.addError("Le tiers exerçant l'activité économique n'est ni une personne physique ni une entreprise");
		}
	}

	@Override
	protected Class<ActiviteEconomique> getValidatedClass() {
		return ActiviteEconomique.class;
	}
}
