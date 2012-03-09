package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class AdresseTiersDAOImpl extends GenericDAOImpl<AdresseTiers, Long> implements AdresseTiersDAO {

	public AdresseTiersDAOImpl() {
		super(AdresseTiers.class);
	}

}
