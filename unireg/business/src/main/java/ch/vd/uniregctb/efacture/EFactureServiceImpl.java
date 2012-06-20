package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureServiceImpl implements EFactureService {

	private TiersService tiersService;
	private EditiqueCompositionService editiqueCompositionService;
	private EFactureMessageSender eFactureMessageSender;
	private EFactureClient eFactureClient;


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
	@Nullable
	public DemandeValidationInscriptionDejaSoumise getDemandeInscritpionEnCoursDeTraitement(long ctbId) {
		List<RegistrationRequestWithHistory> listRequests  = eFactureClient.getHistory(ctbId, EFactureEvent.ACI_BILLER_ID).getHistoryOfRequests().getRequest();
		for (RegistrationRequestWithHistory r : listRequests) {
			final DemandeValidationInscriptionDejaSoumise demande = new DemandeValidationInscriptionDejaSoumise(r);
			if (demande.isEnCoursDeTraitement()) {
				return demande;
			}
		}
		return null;
	}

	@Override
	@Nullable
	public TypeRefusEFacture identifieContribuable(long ctbId, String strNoAvs) {
		Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			return TypeRefusEFacture.NUMERO_CTB_INCOHERENT;
		}
		final long noAvs = AvsHelper.stringToLong(strNoAvs);
		_if: if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun)tiers;
			for(PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (noAvs == AvsHelper.stringToLong(pp.getNumeroAssureSocial()) ) {
					break _if;
				}
			}
			return TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT;
		} else if (tiers instanceof PersonnePhysique) {
			if (noAvs != AvsHelper.stringToLong(((PersonnePhysique)tiers).getNumeroAssureSocial()) ) {
				return TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT;
			}
		} else {
			return TypeRefusEFacture.NUMERO_CTB_INCOHERENT;
		}
		return null;
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

	public void seteFactureClient(EFactureClient eFactureClient) {
		this.eFactureClient = eFactureClient;
	}
}
