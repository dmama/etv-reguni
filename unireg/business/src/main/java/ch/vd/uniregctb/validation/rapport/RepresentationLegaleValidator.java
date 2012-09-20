package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class RepresentationLegaleValidator<T extends RepresentationLegale> extends RapportEntreTiersValidator<T> {

	protected TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public ValidationResults validate(T ret) {

		final ValidationResults vr = super.validate(ret);

		if (!ret.isAnnule()) {
			final Tiers sujet = tiersDAO.get(ret.getSujetId());
			if (sujet == null) {
				vr.addError("Le tiers sous représentation légale n'existe pas");
			}
			else if (!(sujet instanceof PersonnePhysique)) { // [SIFISC-719]
				vr.addError("Une représentation légale ne peut s'appliquer que sur une personne physique");
			}
		}

		return vr;
	}
}
