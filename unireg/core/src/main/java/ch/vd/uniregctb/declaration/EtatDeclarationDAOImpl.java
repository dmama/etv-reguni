package ch.vd.uniregctb.declaration;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class EtatDeclarationDAOImpl extends GenericDAOImpl<EtatDeclaration, Long> implements EtatDeclarationDAO {
	public EtatDeclarationDAOImpl() {
		super(EtatDeclaration.class);
	}
}
