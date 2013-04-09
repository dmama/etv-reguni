package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class ConseilLegalValidator extends RepresentationLegaleValidator<ConseilLegal> {

	@Override
	protected Class<ConseilLegal> getValidatedClass() {
		return ConseilLegal.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le conseiller légal n'existe pas");
		}
		else if (objet instanceof PersonnePhysique || objet instanceof CollectiviteAdministrative) { // [SIFISC-2483]
			// ok
		}
		else {
			vr.addError("Un conseiller légal ne peut être qu'une personne physique ou une collectivité administrative");
		}
	}
}
