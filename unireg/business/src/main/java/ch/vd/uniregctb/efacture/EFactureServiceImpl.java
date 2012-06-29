package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeHistorisee;
import ch.vd.unireg.interfaces.efacture.data.DestinataireHistorise;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
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
import ch.vd.uniregctb.type.TypeEtatDestinataire;

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
	public DemandeHistorisee getDemandeInscriptionEnCoursDeTraitement(long ctbId) {
		List<DemandeHistorisee> listDemandes = getHistoriqueDestinataire(ctbId).getHistoriqueDemandes();
		for (DemandeHistorisee demande : listDemandes) {
			if (demande.isEnCoursDeTraitement()) {
				return demande;
			}
		}
		return null;
	}

	@Override
	@Nullable
	public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String strNoAvs) throws AdresseException {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			return TypeRefusDemande.NUMERO_CTB_INCOHERENT;
		}
		final long noAvs = AvsHelper.stringToLong(strNoAvs);
		_if: if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun)tiers;
			for(PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (noAvs == AvsHelper.stringToLong(tiersService.getNumeroAssureSocial(pp))) {
					break _if;
				}
			}
			return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
		} else if (tiers instanceof PersonnePhysique) {
			if (noAvs != AvsHelper.stringToLong(tiersService.getNumeroAssureSocial((PersonnePhysique) tiers)) ) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
			}
		} else {
			return TypeRefusDemande.NUMERO_CTB_INCOHERENT;
		}
		if (adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false) == null) {
			return TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE;
		}
		return null;
	}

	@Override
	public void updateEmailContribuable(long ctbId, String email) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et n'est pas non-null");
		}
		tiers.setAdresseCourrierElectroniqueEFacture(email);
	}

	@Override
	public boolean valideEtatContribuablePourInscription(long ctbId) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et n'est pas null");
		}
		// Verification de l'historique des situations
		DestinataireHistorise histo = getHistoriqueDestinataire(ctbId);

		if (histo.getDernierEtat() != null && histo.getDernierEtat().getEtatDestinataire() == TypeEtatDestinataire.DESINSCRIT_SUSPENDU) {
			return false;
		}

		// Verification du for principal
		ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
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

	public DestinataireHistorise getHistoriqueDestinataire(long ctbId){

		PayerWithHistory payerWithHistory =eFactureClient.getHistory(ctbId, ACI_BILLER_ID);
		if(payerWithHistory == null){
			return null;
		}
		return new DestinataireHistorise(payerWithHistory, ctbId);
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
		return eFactureMessageSender.envoieAcceptationDemandeInscription(idDemande, retourAttendu, description);
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return eFactureMessageSender.envoieRefusDemandeInscription(idDemande, null, description, retourAttendu);
	}

	@Override
	public String notifieMiseEnattenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return eFactureMessageSender.envoieMiseEnAttenteDemandeInscription(idDemande, typeAttenteEFacture, description, idArchivage, retourAttendu);
	}

	private TypeAttenteDemande determineTypeAttenteEfacture(TypeDocument typeDocument) throws IllegalArgumentException {
		switch (typeDocument) {
		case E_FACTURE_ATTENTE_CONTACT:
			return TypeAttenteDemande.EN_ATTENTE_CONTACT;
		case E_FACTURE_ATTENTE_SIGNATURE:
			return TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
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
