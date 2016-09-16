package ch.vd.uniregctb.documentfiscal;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public interface AutreDocumentFiscalManager {

	@Transactional(rollbackFor = Throwable.class)
	ResultatQuittancement quittanceLettreBienvenue(long noCtb, RegDate dateRetour);

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	List<AutreDocumentFiscalView> getAutresDocumentsFiscauxSansSuivi(long noCtb);

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	boolean hasAnyEtat(long noCtb, TypeEtatEntreprise... types);

	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat createAndPrint(ImprimerAutreDocumentFiscalView view) throws AutreDocumentFiscalException;
}
