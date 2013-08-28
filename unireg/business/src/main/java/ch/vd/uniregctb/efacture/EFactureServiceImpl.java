package ch.vd.uniregctb.efacture;

import javax.jms.JMSException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.UniregModeHelper;

public class EFactureServiceImpl implements EFactureService, InitializingBean {

	public static final Logger LOGGER = Logger.getLogger(EFactureServiceImpl.class);

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
		// Verification de l'historique des situations
		final DestinataireAvecHisto histo = getDestinataireAvecSonHistorique(noCtb);
		if (histo != null && histo.getDernierEtat().getType() == TypeEtatDestinataire.DESINSCRIT_SUSPENDU) {
			return ResultatQuittancement.etatFiscalIncoherent();
		}
		if (!EFactureHelper.valideEtatFiscalContribuablePourInscription(tiers)) {
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

	@Override
	public void afterPropertiesSet() throws Exception {
		// tout ça pour ne logguer cette information que dans les web-app où elle a un sens (qu'est ce que cela veut dire dans NEXUS ou WS ?)
		LOGGER.info(String.format("MODE E-FACTURE %s", UniregModeHelper.isEfactureEnabled() ? "ON" : "OFF"));
	}
}
