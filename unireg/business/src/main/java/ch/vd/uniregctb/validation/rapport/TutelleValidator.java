package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.Tutelle;

public class TutelleValidator extends RepresentationLegaleValidator<Tutelle> {

	@Override
	protected Class<Tutelle> getValidatedClass() {
		return Tutelle.class;
	}

	@Override
	public ValidationResults validate(Tutelle ret) {

		final ValidationResults vr = super.validate(ret);

		if (!ret.isAnnule()) {
			final Tiers objet = tiersDAO.get(ret.getObjetId());
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

		return vr;
	}
}
