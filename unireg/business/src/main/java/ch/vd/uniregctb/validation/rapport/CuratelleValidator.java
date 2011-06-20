package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class CuratelleValidator extends RepresentationLegaleValidator<Curatelle> {

	@Override
	protected Class<Curatelle> getValidatedClass() {
		return Curatelle.class;
	}

	@Override
	public ValidationResults validate(Curatelle ret) {

		final ValidationResults vr = super.validate(ret);

		if (!ret.isAnnule()) {
			final Tiers objet = tiersDAO.get(ret.getObjetId());
			if (objet == null) {
				vr.addError("Le curateur n'existe pas");
			}
			else if (!(objet instanceof PersonnePhysique)) { // [SIFISC-719]
				vr.addError("Un curateur ne peut être qu'une personne physique");
			}
		}

		return vr;
	}
}
