package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class RepresentationLegaleValidator extends RapportEntreTiersValidator<RepresentationLegale> {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	protected Class<RepresentationLegale> getValidatedClass() {
		return RepresentationLegale.class;
	}

	@Override
	public ValidationResults validate(RepresentationLegale ret) {

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
