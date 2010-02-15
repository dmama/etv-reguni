package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.fiscalite.registre.evenementFiscalV1.*;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.spring.EsbTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;import ch.vd.uniregctb.evenement.*;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;

/**
 * Bean qui permet d'envoyer des événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public final class EvenementFiscalSenderImpl implements EvenementFiscalSender {

	private static final Logger LOGGER = Logger.getLogger(EvenementFiscalSenderImpl.class);

	private String outputQueue;
	private EsbTemplate esbTemplate;
	private String serviceDestination;

	/**
	 * permet d'activer/désactiver l'envoi des événements fiscaux
	 */
	private boolean enabled = true;

	/**
	 * {@inheritDoc}
	 *
	 * @throws EvenementFiscalException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		if (!enabled) {
			LOGGER.info("Evénements fiscaux désactivé: l'événement n° " + evenement.getId() + " n'est pas envoyé.");
			return;
		}

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);
		
		// Crée la représenation XML de l'événement
		final XmlObject document = core2xml(evenement);
		validateXml(document);

		// Envoi l'événement sous forme de message JMS à travers l'ESB
		EsbMessage m = new EsbMessage();
		m.setBusinessId(String.valueOf(evenement.getId()));
		m.setBusinessUser(principal);
		m.setBusinessCorrelationId(String.valueOf(evenement.getId()));
		m.setServiceDestination(serviceDestination);
		m.setDomain("fiscalite");
		m.setContext("registreFiscal");
		m.setApplication("unireg");
		final Node node = document.newDomNode();
		m.setBody((Document) node);

		try {
			if (outputQueue != null) {
				esbTemplate.sendEsbMessage(outputQueue, m); // for testing only
			}
			else {
				esbTemplate.sendEsbMessage(m);
			}
		}
		catch (Exception e) {
			String message = "Exception lors du processus d'envoi d'un événement fiscal.";
			LOGGER.fatal(message, e);

			throw new EvenementFiscalException(message, e);
		}
	}

	private static XmlObject core2xml(EvenementFiscal evenement) throws EvenementFiscalException {

		final XmlObject object;

		if (evenement instanceof EvenementFiscalSituationFamille) {
			object = creerEvenementFiscal((EvenementFiscalSituationFamille) evenement);
		}
		else if (evenement instanceof EvenementFiscalFor) {
			object = creerEvenementFiscal((EvenementFiscalFor) evenement);
		}
		else if (evenement instanceof EvenementFiscalDI) {
			object = creerEvenementFiscal((EvenementFiscalDI) evenement);
		}
		else if (evenement instanceof EvenementFiscalLR) {
			object = creerEvenementFiscal((EvenementFiscalLR) evenement);
		}
		else {
			throw new EvenementFiscalException("Type d'événement inconnu = ["+evenement.getClass()+"]");
		}

		return object;
	}

	private static EvenementFiscalSituationFamilleDocument creerEvenementFiscal(EvenementFiscalSituationFamille evenementSituationFamille) {

		final EvenementFiscalSituationFamilleDocument document = EvenementFiscalSituationFamilleDocument.Factory.newInstance();
		final EvenementFiscalSituationFamilleType evt = document.addNewEvenementFiscalSituationFamille();
		evt.setCodeEvenement(EvenementFiscalSituationFamilleEnumType.Enum.forString(evenementSituationFamille.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementSituationFamille.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementSituationFamille.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementSituationFamille.getId());

		return document;
	}

	private static EvenementFiscalForDocument creerEvenementFiscal(EvenementFiscalFor evenementFor) {

		final EvenementFiscalForDocument document = EvenementFiscalForDocument.Factory.newInstance();
		final EvenementFiscalForType evt = document.addNewEvenementFiscalFor();
		evt.setCodeEvenement(EvenementFiscalForEnumType.Enum.forString(evenementFor.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementFor.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementFor.getTiers().getNumero()));
		if (evenementFor.getMotifFor() != null) {
			evt.setMotifFor(MotifForEnumType.Enum.forString(evenementFor.getMotifFor().toString()));
		}
		else if (evenementFor.getModeImposition() != null) {
			evt.setModeImposition(ModeImpositionEnumType.Enum.forString(evenementFor.getModeImposition().name()));
		}
		evt.setNumeroTechnique(evenementFor.getId());

		return document;
	}

	private static EvenementFiscalDIDocument creerEvenementFiscal(EvenementFiscalDI evenementDI) {

		final EvenementFiscalDIDocument document = EvenementFiscalDIDocument.Factory.newInstance();
		final EvenementFiscalDIType evt = document.addNewEvenementFiscalDI();
		evt.setCodeEvenement(EvenementFiscalDIEnumType.Enum.forString(evenementDI.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementDI.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementDI.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementDI.getId());
		evt.setDateDebutPeriode(DateUtils.calendar(evenementDI.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.calendar(evenementDI.getDateFinPeriode().asJavaDate()));

		return document;

	}

	private static EvenementFiscalLRDocument creerEvenementFiscal(EvenementFiscalLR evenementLR) {

		final EvenementFiscalLRDocument document = EvenementFiscalLRDocument.Factory.newInstance();
		final EvenementFiscalLRType evt = document.addNewEvenementFiscalLR();
		evt.setCodeEvenement(EvenementFiscalLREnumType.Enum.forString(evenementLR.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementLR.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementLR.getTiers().getNumero()));
		evt.setDateDebutPeriode(DateUtils.calendar(evenementLR.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.calendar(evenementLR.getDateFinPeriode().asJavaDate()));
		evt.setNumeroTechnique(evenementLR.getId());

		return document;

	}

	/**
	 * Valide une instance d'un objet XML à partir de son schéma (embeddé dans l'objet)
	 *
	 * @param object l'objet à valider
	 * @throws EvenementFiscalException levée si l'objet n'est pas valide
	 */
	private static void validateXml(XmlObject object) throws EvenementFiscalException {

		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}
			throw new EvenementFiscalException(builder.toString());
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbTemplate(EsbTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}
}
