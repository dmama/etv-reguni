package ch.vd.uniregctb.efacture.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.efacture.EvenementEfactureException;
import ch.vd.uniregctb.type.TypeDocument;

public class EfactureManagerImpl implements EfactureManager {

	private EFactureService eFactureService;


	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void envoyerDocumentAvecNotificationEFacture(Long ctbId, TypeDocument typeDocument, Long idDemande, RegDate dateDemande) throws EditiqueException, EvenementEfactureException {

		final String idArchivage = eFactureService.imprimerDocumentEfacture(ctbId, typeDocument, dateDemande);
		String strIdDemande = idDemande.toString();
		eFactureService.notifieMiseEnattenteInscription(strIdDemande,typeDocument,idArchivage, true);

	}

	public void seteFactureService(EFactureService eFactureService) {
		this.eFactureService = eFactureService;
	}
}
