package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Curatelle;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

public class CuratelleValidator extends RepresentationLegaleValidator<Curatelle> {

	@Override
	protected Class<Curatelle> getValidatedClass() {
		return Curatelle.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le curateur n'existe pas");
		}
		else if (objet instanceof PersonnePhysique || objet instanceof CollectiviteAdministrative) { // [SIFISC-2483]
			// ok
		}
		else {
			vr.addError("Un curateur ne peut être qu'une personne physique ou une collectivité administrative");
		}
	}
}
