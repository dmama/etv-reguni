package ch.vd.uniregctb.documentfiscal;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public interface AutreDocumentFiscalManager {

	@Transactional(rollbackFor = Throwable.class)
	ResultatQuittancement quittanceLettreBienvenuePourCtb(long noCtb, RegDate dateRetour);

	/**
	 * Quittance manuelle de la lettre de bienvenue.
	 * @param id l'identifiant de la lettre de bienvenue
	 * @param dateRetour la date de retour du document
	 * @return <code>true</code> si un état retourné à été créé, <code>false</code> s'il en existait déjà un.
	 */
	boolean quittanceLettreBienvenue(long id, RegDate dateRetour);

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	List<AutreDocumentFiscalView> getAutresDocumentsFiscauxSansSuivi(long noCtb);

	EditiqueResultat envoieImpressionLocalDuplicataLettreBienvenue(Long id) throws AutreDocumentFiscalException;

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	List<AutreDocumentFiscalView> getAutresDocumentsFiscauxAvecSuivi(long noCtb);

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	boolean hasAnyEtat(long noCtb, TypeEtatEntreprise... types);

	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat createAndPrint(ImprimerAutreDocumentFiscalView view) throws AutreDocumentFiscalException;

	@Transactional(rollbackFor = Throwable.class)
	Long saveNouveauDelai(Long idDoc, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etat);

	@Transactional(rollbackFor = Throwable.class)
	void saveDelai(Long idDelai, EtatDelaiDocumentFiscal etat, RegDate delaiAccordeAu);

	void annulerAutreDocumentFiscal(AutreDocumentFiscal doc);

	void desannulerAutreDocumentFiscal(AutreDocumentFiscal doc);
}
