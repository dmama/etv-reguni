package ch.vd.unireg.efacture;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.utils.UniregModeHelper;

public class ReadOnlyEFactureServiceImpl implements EFactureService, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReadOnlyEFactureServiceImpl.class);

	private EFactureClient eFactureClient;

	public void seteFactureClient(EFactureClient eFactureClient) {
		this.eFactureClient = eFactureClient;
	}

	@Override
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		if (UniregModeHelper.isEfactureEnabled()) {
			final PayerWithHistory payerWithHistory = eFactureClient.getHistory(ctbId, ACI_BILLER_ID);
			if (payerWithHistory == null) {
				return null;
			}
			return new DestinataireAvecHisto(payerWithHistory, ctbId);
		}
		else {
			LOGGER.warn("Service e-Facture désactivé");
			return null;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// tout ça pour ne logguer cette information que dans les web-app où elle a un sens (qu'est ce que cela veut dire dans NEXUS ?)
		LOGGER.info(String.format("MODE E-FACTURE %s", UniregModeHelper.isEfactureEnabled() ? "ON" : "OFF"));
	}

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String modifierEmailContribuable(long noCtb, @Nullable String newEmail, boolean retourAttendu, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void demanderDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EvenementEfactureException {
		throw new UnsupportedOperationException();
	}
}
