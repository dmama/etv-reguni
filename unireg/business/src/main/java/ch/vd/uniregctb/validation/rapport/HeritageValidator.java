package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class HeritageValidator extends RapportEntreTiersValidator<Heritage> {

	@Override
	protected Class<Heritage> getValidatedClass() {
		return Heritage.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le tiers défunt n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le tiers héritier n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	@Override
	public ValidationResults validate(Heritage ret) {
		final ValidationResults vr = super.validate(ret);
		if (!ret.isAnnule()) {
			// TODO ne faut-il pas vérifier quelques trucs par rapport aux positions relatives de la date de décès du défunt et de la date de début du rapport ?
		}
		return vr;
	}
}
