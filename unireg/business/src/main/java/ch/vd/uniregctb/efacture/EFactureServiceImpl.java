package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureServiceImpl implements EFactureService {

	private TiersService tiersService;
	private EditiqueCompositionService editiqueCompositionService;
	private EFactureMessageSender eFactureMessageSender;

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException {
		final Tiers tiers = tiersService.getTiers(ctbId);
		final Date dateTraitement = DateHelper.getCurrentDate();

		try {
			return editiqueCompositionService.imprimeDocumentEfacture(tiers, typeDocument, dateTraitement, dateDemande);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	public void notifieMiseEnattenteInscription(String idDemande, TypeDocument typeDocument, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		final TypeAttenteEFacture typeAttenteEFacture = determineTypeAttenteEfacture(typeDocument);
		eFactureMessageSender.envoieMiseEnAttenteDemandeInscription(idDemande, typeAttenteEFacture, idArchivage, retourAttendu);
	}

	private TypeAttenteEFacture determineTypeAttenteEfacture(TypeDocument typeDocument) throws IllegalArgumentException {
		switch (typeDocument) {
		case E_FACTURE_ATTENTE_CONTACT:
			return TypeAttenteEFacture.EN_ATTENTE_CONTACT;
		case E_FACTURE_ATTENTE_SIGNATURE:
			return TypeAttenteEFacture.EN_ATTENTE_SIGNATURE;
		default:
			throw new IllegalArgumentException("Le type de document " + typeDocument.name() + " est inconnue");
		}
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void seteFactureMessageSender(EFactureMessageSender eFactureMessageSender) {
		this.eFactureMessageSender = eFactureMessageSender;
	}
}
