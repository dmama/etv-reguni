package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class SituationFamilleDAOImpl extends GenericDAOImpl<SituationFamille, Long> implements SituationFamilleDAO {

	public SituationFamilleDAOImpl() {
		super(SituationFamille.class);
	}

}
