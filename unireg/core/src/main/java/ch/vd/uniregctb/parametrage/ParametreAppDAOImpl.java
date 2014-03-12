package ch.vd.uniregctb.parametrage;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class ParametreAppDAOImpl extends GenericDAOImpl<ParametreApp, String> implements ParametreAppDAO {

	public ParametreAppDAOImpl() {
		super(ParametreApp.class);
	}

}
