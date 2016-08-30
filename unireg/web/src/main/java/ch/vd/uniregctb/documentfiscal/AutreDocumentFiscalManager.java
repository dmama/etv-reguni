package ch.vd.uniregctb.documentfiscal;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;

public interface AutreDocumentFiscalManager {

	@Transactional(rollbackFor = Throwable.class)
	ResultatQuittancement quittanceLettreBienvenue(long noCtb, RegDate dateRetour);
}
