package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;
import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.AuthenticationHelper;
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
import ch.vd.uniregctb.utils.UniregModeHelper;

public class EFactureServiceImpl implements EFactureService, InitializingBean {

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
	public DemandeAvecHisto getDemandeEnAttente(long ctbId) {
		final DestinataireAvecHisto dest = getDestinataireAvecSonHistorique(ctbId);
		if (dest != null) {
			for (DemandeAvecHisto demande : dest.getHistoriqueDemandes()) {
				if (demande.isEnAttente()) {
					return demande;
				}
			}
		}
		return null;
	}

	@Override
	@Nullable
	public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String strNoAvs) throws AdresseException {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (isTiersIncoherent(tiers)) {
			return TypeRefusDemande.NUMERO_CTB_INCOHERENT;
		}

		if (numeroAvsIsRenseigneForTiers(tiers)) {
			final long noAvs;
			try {
				noAvs = AvsHelper.stringToLong(strNoAvs);
			}
			catch (IllegalArgumentException e) {
				return TypeRefusDemande.NUMERO_AVS_INVALIDE;
			}
			final TypeRefusDemande refusDemande= controlerNumeroAVS(noAvs,tiers);
			if (refusDemande!=null) {
				return refusDemande;
			}
		}

		if (adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false) == null) {
			return TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE;
		}
		return null;
	}

	/**
	 * Permet de tester les conditions suivante issues du JIRA-7326
	 * Navs renseigné pour le ctb personne physique
	 * Navs renseigné pour les deux membres d'un ctb ménage commun
	 * */


	private boolean numeroAvsIsRenseigneForTiers(Tiers tiers){
		if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun)tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				final String avs = tiersService.getNumeroAssureSocial(pp);
				if (StringUtils.isBlank(avs) ) {
					return false;
				}
			}
		}
		else if(tiers instanceof PersonnePhysique){
			final String avs = tiersService.getNumeroAssureSocial((PersonnePhysique) tiers);
			if (StringUtils.isBlank(avs) ) {
				return false;
			}
		}
		return true;
	}

	private TypeRefusDemande controlerNumeroAVS(long noAvs, Tiers tiers) {


		if (tiers instanceof MenageCommun) {
			boolean found = false;
			final MenageCommun menage = (MenageCommun)tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				final String avs = tiersService.getNumeroAssureSocial(pp);
				if (noAvs == AvsHelper.stringToLong(avs)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
			}
		}
		else if (tiers instanceof PersonnePhysique) {
			final String avs = tiersService.getNumeroAssureSocial((PersonnePhysique) tiers);
			if (noAvs != AvsHelper.stringToLong(avs)) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
			}
		}
		return null;
	}

	private boolean isTiersIncoherent(Tiers tiers) {
		final boolean tiersIsNull = tiers == null;
		final boolean tiersTypeIncoherent = !(tiers instanceof MenageCommun) && !(tiers instanceof PersonnePhysique);
		return tiersIsNull || tiersTypeIncoherent;
	}

	@Override
	public void updateEmailContribuable(long ctbId, String email) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et n'est pas non-null");
		}
		tiers.setAdresseCourrierElectroniqueEFacture(email);
	}

	private static final EnumSet<ModeImposition> MODE_IMPOSITIONS_AUTORISES = EnumSet.of(
			ModeImposition.ORDINAIRE,
			ModeImposition.MIXTE_137_1,
			ModeImposition.MIXTE_137_2,
			ModeImposition.DEPENSE);

	private static final EnumSet<MotifFor> MOTIF_FORS_INTERDITS = EnumSet.of(
			MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			MotifFor.VEUVAGE_DECES);

	@Override
	public boolean valideEtatFiscalContribuablePourInscription(long ctbId) {
		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers == null) {
			throw new AssertionError("Impossible d'atterrir ici, l'appel à getTiers(" + ctbId + ") à déja été fait et n'est pas null");
		}

		// Verification de l'historique des situations
		final DestinataireAvecHisto histo = getDestinataireAvecSonHistorique(ctbId);
		if (histo != null && histo.getDernierEtat().getType() == TypeEtatDestinataire.DESINSCRIT_SUSPENDU) {
			return false;
		}

		// Verification du for principal
		final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
		if (ffp == null) {
			return false;
		}

		if (ffp.getDateFin() != null && MOTIF_FORS_INTERDITS.contains(ffp.getMotifFermeture())) {
			return false;
		}

		if (!MODE_IMPOSITIONS_AUTORISES.contains(ffp.getModeImposition())) {
			return false;
		}

		return true;
	}

	@Override
	@Nullable
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		final PayerWithHistory payerWithHistory = eFactureClient.getHistory(ctbId, ACI_BILLER_ID);
		if (payerWithHistory == null) {
			return null;
		}
		return new DestinataireAvecHisto(payerWithHistory, ctbId);
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
		return eFactureMessageSender.envoieRefusDemandeInscription(idDemande, description, retourAttendu);
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		final Tiers tiers = tiersService.getTiers(noCtb);
		if (tiers == null) {
			return ResultatQuittancement.contribuableInexistant();
		}
		if (!valideEtatFiscalContribuablePourInscription(noCtb)) {
			return ResultatQuittancement.etatFiscalIncoherent();
		}
		final PayerWithHistory payerWithHistory = eFactureClient.getHistory(noCtb,EFactureService.ACI_BILLER_ID);
		if (payerWithHistory == null) {
			return ResultatQuittancement.aucuneDemandeEnAttenteDeSignature();
		}
		final DestinataireAvecHisto destinataireAvecHisto = new DestinataireAvecHisto(payerWithHistory, noCtb);
		if (destinataireAvecHisto.getDernierEtat().getType() == TypeEtatDestinataire.INSCRIT) {
			return ResultatQuittancement.dejaInscrit();
		}
		for (DemandeAvecHisto dem : destinataireAvecHisto.getHistoriqueDemandes()) {
			if (dem.getDernierEtat().getType() == TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE) {
				final String businessId = eFactureMessageSender.envoieAcceptationDemandeInscription(dem.getIdDemande(), true, String.format("Traitement manuel par %s.", AuthenticationHelper.getCurrentPrincipal()));
				return ResultatQuittancement.enCours(businessId);
			}
		}
		return ResultatQuittancement.aucuneDemandeEnAttenteDeSignature();
	}

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return eFactureMessageSender.envoieMiseEnAttenteDemandeInscription(idDemande, typeAttenteEFacture, description, idArchivage, retourAttendu);
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

	public void setUniregModeHelper(UniregModeHelper helper) {
		// rien à faire, les méthodes sont de toute façon statiques sur cet objet, qui n'a été introduit ici que pour forcer la dépendance,
		// de telle sorte que lors de l'appel à la méthode afterPropertiesSet(), on soit sûr que le mode est dans un état complètement configuré
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// tout ça pour ne logguer cette information que dans les web-app où elle a un sens (qu'est ce que cela veut dire dans NEXUS ou WS ?)
		LOGGER.info(String.format("MODE E-FACTURE %s", UniregModeHelper.isEfactureEnabled() ? "ON" : "OFF"));
	}
}
