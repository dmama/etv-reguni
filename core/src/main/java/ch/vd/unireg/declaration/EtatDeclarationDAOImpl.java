package ch.vd.unireg.declaration;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;

public class EtatDeclarationDAOImpl extends BaseDAOImpl<EtatDocumentFiscal, Long> implements EtatDeclarationDAO {
	public EtatDeclarationDAOImpl() {
		super(EtatDocumentFiscal.class);
	}
}
