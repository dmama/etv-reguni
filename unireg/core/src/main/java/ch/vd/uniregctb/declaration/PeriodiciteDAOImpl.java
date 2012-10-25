package ch.vd.uniregctb.declaration;


import ch.vd.registre.base.dao.GenericDAOImpl;

public class PeriodiciteDAOImpl extends GenericDAOImpl<Periodicite,Long> implements PeriodiciteDAO {
	public PeriodiciteDAOImpl(){
		super(Periodicite.class);
	}
		
}
