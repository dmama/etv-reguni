package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.HistoriqueDestinataireWrapper;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureServiceImpl implements EFactureService {

	public static final Logger LOGGER = Logger.getLogger(EFactureServiceImpl.class);

	private TiersService tiersService;
	private AdresseService adresseService;
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
	public DemandeValidationInscriptionAvecHistorique getDemandeInscriptionEnCoursDeTraitement(long ctbId) {
		List<RegistrationRequestWithHistory> listRequests  = eFactureClient.getHistory(ctbId, EFactureEvent.ACI_BILLER_ID).getHistoryOfRequests().getRequest();
		for (RegistrationRequestWithHistory r : listRequests) {
			final DemandeValidationInscriptionAvecHistorique demande = new DemandeValidationInscriptionAvecHistorique(r);
			if (demande.isEnCoursDeTraitement()) {
				return demande;
			}
		}
		return null;
	}

	@Override
	@Nullable
	public TypeRefusEFacture identifieContribuablePourInscription(long ctbId, String strNoAvs) throws AdresseException {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			return TypeRefusEFacture.NUMERO_CTB_INCOHERENT;
		}
		final long noAvs = AvsHelper.stringToLong(strNoAvs);
		_if: if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun)tiers;
			for(PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (noAvs == AvsHelper.stringToLong(tiersService.getNumeroAssureSocial(pp))) {
					break _if;
				}
			}
			return TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT;
		} else if (tiers instanceof PersonnePhysique) {
			if (noAvs != AvsHelper.stringToLong(tiersService.getNumeroAssureSocial((PersonnePhysique) tiers)) ) {
				return TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT;
			}
		} else {
			return TypeRefusEFacture.NUMERO_CTB_INCOHERENT;
		}
		if (adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false) == null) {
			return TypeRefusEFacture.ADRESSE_COURRIER_INEXISTANTE;
		}
		return null;
	}

	@Override
	public void updateEmailContribuable(long ctbId, String email) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et retourne non-null");
		}
		tiers.setAdresseCourrierElectroniqueEFacture(email);
	}

	@Override
	public boolean valideEtatContribuablePourInscription(long ctbId) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et retourne non null");
		}
		// Verification de l'historique e-facture
		PayerWithHistory payer = eFactureClient.getHistory(ctbId, EFactureEvent.ACI_BILLER_ID);
		if (payer != null && payer.getHistoryOfSituations() != null
				&& payer.getHistoryOfSituations().getSituation() != null
				&& payer.getHistoryOfSituations().getSituation().size() > 0
		) {
			PayerSituationHistoryEntry situation = payer.getHistoryOfSituations().getSituation().get(payer.getHistoryOfSituations().getSituation().size() - 1);
			if (situation != null && situation.getStatus() == PayerStatus.DESINSCRIT_SUSPENDU) {
				return false;
			}
		}

		// Verification du for principal
		ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(null);
		if (ffp == null) {
			return false;
		}

		final Set<ModeImposition> modesAutorises = EnumSet.of(
				ModeImposition.ORDINAIRE,
				ModeImposition.MIXTE_137_1,
				ModeImposition.MIXTE_137_2,
				ModeImposition.DEPENSE);
		if (!modesAutorises.contains(ffp.getModeImposition())) {
			return false;
		}

		if (ffp.getDateFin() != null) {
			final Set<MotifFor> motifsInterdits = EnumSet.of(
					MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					MotifFor.VEUVAGE_DECES);

			if (motifsInterdits.contains(ffp.getMotifFermeture())) {
				return false;
			}
		}

		return true;
	}

	public HistoriqueDestinataireWrapper getHistoriqueDestinataire(long ctbId){

		PayerWithHistory payerWithHistory =eFactureClient.getHistory(ctbId, EFactureEvent.ACI_BILLER_ID);
		if(payerWithHistory == null){
			return null;
		}
		return new HistoriqueDestinataireWrapper(payerWithHistory, ctbId);
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return eFactureMessageSender.envoieSuspensionContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return eFactureMessageSender.envoieActivationContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return eFactureMessageSender.envoieAcceptationDemandeInscription(idDemande,retourAttendu, description);
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return eFactureMessageSender.envoieRefusDemandeInscription(idDemande,null, description, retourAttendu);
	}

	@Override
	public String notifieMiseEnattenteInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return eFactureMessageSender.envoieMiseEnAttenteDemandeInscription(idDemande, typeAttenteEFacture, description, idArchivage, retourAttendu);
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

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

}
