package ch.vd.uniregctb.dpermRattrapage;

import java.util.ArrayList;
import java.util.List;

import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.impl.EditiqueCompositionServiceImpl;
import ch.vd.uniregctb.evenement.docsortant.EvenementDocumentSortantService;

/**
 * Rattraper l'envoi au DPerm des documents émis de la 16R2 à la 16R3 incluses.
 * @author Raphaël Marmier, 2017-02-01, <raphael.marmier@vd.ch>
 */
public class RattrapageDPerm {
	EvenementDocumentSortantService evenementDocumentSortantService;

	EditiqueCompositionServiceImpl editiqueCompositionService;

	private DelaiDeclarationDAO delaiDeclarationDAO;

	public RattrapageDPerm(EvenementDocumentSortantService evenementDocumentSortantService, EditiqueCompositionService editiqueCompositionService, DelaiDeclarationDAO delaiDeclarationDAO) {
		this.evenementDocumentSortantService = evenementDocumentSortantService;
		this.editiqueCompositionService = editiqueCompositionService;
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}

	public void rattrapageDPerm() {

	}

	public void rattraperDecisionsDelaiPM() {
		List<Integer> decisionsDelaiRattrapees = new ArrayList<>();
		for (Integer delaiId : DelaiDeclarationARattraperData.getDelaiDeclarationARattraper()) {
			final DelaiDeclaration delaiDeclaration = delaiDeclarationDAO.get(delaiId.longValue());
			try {
				rattraperDecisionDelaiPM(delaiDeclaration);
			} catch (Exception e) {
				Audit.error(String .format("Echec du rattrapage de la Décision Délai n°%s: %s", delaiId, e.getMessage()));
			}
		}
	}

	public void rattraperDecisionDelaiPM(DelaiDeclaration delai) {
		final DeclarationImpotOrdinairePM declaration = (DeclarationImpotOrdinairePM) delai.getDeclaration();

		final CTypeInfoArchivage cTypeInfoArchivage = new CTypeInfoArchivage();
		cTypeInfoArchivage.

		editiqueCompositionService.envoieNotificationLettreDecisionDelai(declaration, delai, cTypeInfoArchivage, true)
	}


}
