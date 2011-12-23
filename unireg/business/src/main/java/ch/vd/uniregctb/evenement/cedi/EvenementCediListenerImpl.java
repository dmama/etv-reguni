package ch.vd.uniregctb.evenement.cedi;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.fiscalite.taxation.dossierElectronique.x1.DeclarationImpotType;
import ch.vd.fiscalite.taxation.dossierElectronique.x1.DossierElectroniqueDocument;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

public class EvenementCediListenerImpl extends EsbMessageEndpointListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementCediListenerImpl.class);

	private EvenementCediHandler handler;

	private HibernateTemplate hibernateTemplate;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementCediHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-EvtCedi");

		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée du message CEDI n°" + businessId);
			final String body = message.getBodyAsString();
			onMessage(body, businessId, EsbMessageHelper.extractCustomHeaders(message));

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EvenementCediException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.UNKNOWN, "");

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (XmlException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.TECHNICAL, "");

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(String message, String businessId, Map<String, String> incomingHeaders) throws XmlException, EvenementCediException {
		final EvenementCedi event = parse(message, businessId);
		handler.onEvent(event, incomingHeaders);
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
			boolean first = true;
			final StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				if (!first) {
					builder.append('\n');
				}
				builder.append("Message: ").append(error.getErrorCode()).append(' ').append(error.getMessage()).append('\n');
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append('\n');
				first = false;
			}
			throw new XmlException(builder.toString());
		}

		// Crée l'événement correspondant
		if (evt instanceof DossierElectroniqueDocument) {
			final DossierElectroniqueDocument.DossierElectronique dossier = ((DossierElectroniqueDocument) evt).getDossierElectronique();
			final DeclarationImpotType di = dossier.getDeclarationImpot();
			final DeclarationImpotType.Identification.CoordonneesContribuable coordonnes = di.getIdentification() == null ? null : di.getIdentification().getCoordonneesContribuable(); // [UNIREG-2603]

			final RetourDI scan = new RetourDI();
			scan.setBusinessId(businessId);
			scan.setDateTraitement(DateHelper.getCurrentDate());
			scan.setNoContribuable(Long.parseLong(di.getNoContribuable()));
			scan.setPeriodeFiscale(Integer.parseInt(di.getPeriode()));
			scan.setNoSequenceDI(Integer.parseInt(di.getNoSequenceDI()));

			// Téléphone du 27 mai 2010 avec Bernard Gaberell: il faut utiliser le type de saisie pour déterminer
			// le type de document (et non pas le type de document sur la déclaration). Le truc c'est qu'il arrive
			// que des DIs électroniques ne puissent pas être scannées, elles sont alors entrées à la main et dans
			// ce cas le type de document change, mais pas le type de saisie.
			//
			// Précision par email du 27 mai 2010 de Bernard Gaberell:
			//  - Depuis la DI2009 (actuellement scannage en production) :
			//    La balise <TypeSaisie>M</TypeSaisie> détermine si l'on a à faire à une DI manuelle (ordinaire) ou électronique (VaudTax ou autres éditeurs).
			// 	    M - déclaration manuelle
			//      E - déclaration électronique (vaudtax ou autres éditeurs)
			//   Cette balise est valable seulement depuis la DI 2009.
			//  - Pour les DI antérieures à 2009 (2008, 2007, etc..) :
			//    Le type de document peut être déterminé par :
			//      100 - déclaration manuelle
			//      109 - déclaration vaudtax (ou autres éditeurs)
			RetourDI.TypeDocument typeDocument = RetourDI.TypeDocument.fromTypeSaisie(dossier.getOperation().getTypeSaisie().toString());
			if (typeDocument == null) {
				// il s'agit peut-être d'une ancienne DI
				typeDocument = RetourDI.TypeDocument.fromTypeDocument(di.getTypeDocument());
			}
			scan.setTypeDocument(typeDocument);

			if (coordonnes != null) { // le XSD permet de ne pas renseigner ces coordonnées
				scan.setEmail(coordonnes.getAdresseMail());
				scan.setIban(coordonnes.getCodeIBAN());
				scan.setNoMobile(coordonnes.getNoTelPortable());
				scan.setNoTelephone(coordonnes.getTelephoneContact());
				scan.setTitulaireCompte(coordonnes.getTitulaireCompte());
			}

			LOGGER.info("Contenu du message : " + scan);

			return scan;
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + evt.getClass());
		}
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
