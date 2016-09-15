package ch.vd.uniregctb.documentfiscal;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueResultat;

public interface AutreDocumentFiscalManager {

	@Transactional(rollbackFor = Throwable.class)
	ResultatQuittancement quittanceLettreBienvenue(long noCtb, RegDate dateRetour);

	@Transactional(rollbackFor = Throwable.class)
	List<AutreDocumentFiscalView> getAutresDocumentsFiscauxSansSuivi(long noCtb);

	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat createAndPrint(ImprimerAutreDocumentFiscalView view) throws AutreDocumentFiscalException;
}
