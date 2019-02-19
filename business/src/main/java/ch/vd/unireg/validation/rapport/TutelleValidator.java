package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.Tutelle;

public class TutelleValidator extends RepresentationLegaleValidator<Tutelle> {

	@Override
	protected Class<Tutelle> getValidatedClass() {
		return Tutelle.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le tuteur n'existe pas");
		}
		else if (objet instanceof PersonnePhysique || objet instanceof CollectiviteAdministrative) { // [SIFISC-719]
			// ok
		}
		else {
			vr.addError("Un tuteur ne peut être qu'une personne physique ou une collectivité administrative");
		}
	}
}
