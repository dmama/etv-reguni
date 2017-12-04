package ch.vd.uniregctb.declaration;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscal;

public class EtatDeclarationDAOImpl extends BaseDAOImpl<EtatDocumentFiscal, Long> implements EtatDeclarationDAO {
	public EtatDeclarationDAOImpl() {
		super(EtatDocumentFiscal.class);
	}
}
