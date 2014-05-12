package ch.vd.uniregctb.declaration;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EtatDeclarationDAOImpl extends BaseDAOImpl<EtatDeclaration, Long> implements EtatDeclarationDAO {
	public EtatDeclarationDAOImpl() {
		super(EtatDeclaration.class);
	}
}
