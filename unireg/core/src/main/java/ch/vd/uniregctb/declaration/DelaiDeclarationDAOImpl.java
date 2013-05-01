package ch.vd.uniregctb.declaration;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class DelaiDeclarationDAOImpl extends GenericDAOImpl<DelaiDeclaration, Long> implements DelaiDeclarationDAO {

	public DelaiDeclarationDAOImpl() {
		super(DelaiDeclaration.class);
	}

}
