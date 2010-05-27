package ch.vd.uniregctb.evenement.cedi;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import ch.vd.fiscalite.cedi.DeclarationImpotType;
import ch.vd.fiscalite.cedi.DossierElectroniqueDocument;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;

public class EvenementCediListenerImpl extends EsbMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementCediListenerImpl.class);

	private EvenementCediHandler handler;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementCediHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.setPrincipal("JMS-EvtCedi");

		try {
			final String body = message.getBodyAsString();
			final String businessId = message.getBusinessId();
			onMessage(body, businessId);
		}
		catch (EvenementCediException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.UNKNOWN, "");
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.resetAuthentication();
		}
	}

	private void onMessage(String message, String businessId) throws XmlException, EvenementCediException {
		final EvenementCedi event = parse(message, businessId);
		handler.onEvent(event);
	}

	private EvenementCedi parse(String message, String businessId) throws XmlException {

		final XmlObject evt = XmlObject.Factory.parse(message);
		if (evt == null) {
			throw new RuntimeException("Unexcepted error");
		}

		// Valide le message
		final XmlOptions validateOptions = new XmlOptions();
		final ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!evt.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}
			throw new XmlException(builder.toString());
		}

		// Crée l'événement correspondant
		if (evt instanceof DossierElectroniqueDocument) {
			final DossierElectroniqueDocument.DossierElectronique dossier = ((DossierElectroniqueDocument) evt).getDossierElectronique();
			final DeclarationImpotType di = dossier.getDeclarationImpot();
			final DeclarationImpotType.Identification.CoordonneesContribuable coordonnes = di.getIdentification().getCoordonneesContribuable();

			final RetourDI scan = new RetourDI();
			scan.setBusinessId(businessId);
			scan.setDateTraitement(new Date());
			scan.setNoContribuable(Long.parseLong(di.getNoContribuable()));
			scan.setPeriodeFiscale(Integer.parseInt(di.getPeriode()));
			scan.setNoSequenceDI(Integer.parseInt(di.getNoSequenceDI()));
			// Téléphone du 27 mai 2010 avec Bernard Gaberell: il faut utiliser le type de saisie pour déterminer
			// le type de document (et non pas le type de document sur la déclaration). Le truc c'est qu'il arrive
			// que des DIs électroniques ne puissent pas être scannées, elles sont alors entrées à la main et dans
			// ce cas le type de document change, mais pas le type de saisie (le nommage porte à confusion).
			scan.setTypeDocument(RetourDI.TypeDocument.fromJms(dossier.getOperation().getTypeSaisie()));
			scan.setEmail(coordonnes.getAdresseMail());
			scan.setIban(coordonnes.getCodeIBAN());
			scan.setNoMobile(coordonnes.getNoTelPortable());
			scan.setNoTelephone(coordonnes.getTelephoneContact());
			scan.setTitulaireCompte(coordonnes.getTitulaireCompte());

			LOGGER.info("Arrivée du message JMS: " + scan);

			return scan;
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + evt.getClass());
		}

	}
}
