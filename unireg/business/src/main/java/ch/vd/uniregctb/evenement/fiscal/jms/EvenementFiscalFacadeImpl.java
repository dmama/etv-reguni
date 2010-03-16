package ch.vd.uniregctb.evenement.fiscal.jms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDI;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.EvenementFiscalLR;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFacade;

/**
 * Implémentation standard de {@link EvenementFiscalFacade}.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public final class EvenementFiscalFacadeImpl implements EvenementFiscalFacade {

	/**
	 * Le logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(EvenementFiscalFacadeImpl.class);

	/**
	 * permet de publier sur un topic.
	 */
	private JmsOperations publisher;

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
	public void publierEvenement(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		if (!enabled) {
			LOGGER.info("Evénements fiscaux désactivé: l'événement n° " + evenement.getId() + " n'est pas envoyé.");
			return;
		}

		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		try {
			if (evenement instanceof EvenementFiscalSituationFamille) {
				writeXml(writer, creerEvenementFiscal((EvenementFiscalSituationFamille) evenement));
			} else if (evenement instanceof EvenementFiscalFor) {
				writeXml(writer, creerEvenementFiscal((EvenementFiscalFor) evenement));
			} else if (evenement instanceof EvenementFiscalDI) {
				writeXml(writer, creerEvenementFiscal((EvenementFiscalDI) evenement));
			} else if (evenement instanceof EvenementFiscalLR) {
				writeXml(writer, creerEvenementFiscal((EvenementFiscalLR) evenement));
			}
		}
		catch (Exception e) {
			String message = "Exception lors de la sérialisation xml";
			LOGGER.fatal(message, e);
			throw new EvenementFiscalException(message);
		}
		try {
			publisher.send(new MessageCreator() {

				public Message createMessage(Session session) throws JMSException {
					TextMessage message = session.createTextMessage();
					message.setText(writer.toString());
					return message;
				}
			});
		}
		catch (Exception e) {
			String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.fatal(message, e);

			throw new EvenementFiscalException(message);
		}
	}

	/**
	 * Définit le template JMS spécifique à la publication d'un évévenement fiscale.
	 *
	 * @param jmsTemplateOutput
	 *            le template JMS.
	 */
	public void setPublisher(JmsOperations publisher) {
		this.publisher = publisher;
	}

	/**
	 * {@inheritDoc}
	 */
	public ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleType creerEvenementFiscal(
			EvenementFiscalSituationFamille evenementSituationFamille) {

		ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleType evt =
			ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleDocument.
			Factory.newInstance().addNewEvenementFiscalSituationFamille();
		evt.setCodeEvenement(ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleEnumType.
			Enum.forString(evenementSituationFamille.getType().toString()));
		evt.setDateEvenement(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementSituationFamille.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementSituationFamille.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementSituationFamille.getId());
		return evt;

	}

	/**
	 * {@inheritDoc}
	 */
	public ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForType creerEvenementFiscal(
			EvenementFiscalFor evenementFor) {

		ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForType evt =
			ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForDocument.
			Factory.newInstance().addNewEvenementFiscalFor();
		evt.setCodeEvenement(ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForEnumType.
			Enum.forString(evenementFor.getType().toString()));
		evt.setDateEvenement(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementFor.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementFor.getTiers().getNumero()));
		if (evenementFor.getMotifFor() != null) {
			evt.setMotifFor(ch.vd.fiscalite.registre.evenementFiscalV1.MotifForEnumType.Enum.forString(evenementFor.getMotifFor().toString()));
		}
		evt.setNumeroTechnique(evenementFor.getId());
		return evt;

	}

	/**
	 * {@inheritDoc}
	 */
	public ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIType creerEvenementFiscal(
			EvenementFiscalDI evenementDI) {

		ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIType evt =
			ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIDocument.
			Factory.newInstance().addNewEvenementFiscalDI();
		evt.setCodeEvenement(ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIEnumType.
			Enum.forString(evenementDI.getType().toString()));
		evt.setDateEvenement(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementDI.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementDI.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementDI.getId());
		evt.setDateDebutPeriode(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementDI.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementDI.getDateFinPeriode().asJavaDate()));
		return evt;

	}

	/**
	 * {@inheritDoc}
	 */
	public ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLRType creerEvenementFiscal(
			EvenementFiscalLR evenementLR) {

		ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLRType evt =
			ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLRDocument.
			Factory.newInstance().addNewEvenementFiscalLR();
		evt.setCodeEvenement(ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLREnumType.
			Enum.forString(evenementLR.getType().toString()));
		evt.setDateEvenement(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementLR.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementLR.getTiers().getNumero()));
		evt.setDateDebutPeriode(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementLR.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(ch.vd.infrastructure.model.impl.DateUtils.calendar(
			evenementLR.getDateFinPeriode().asJavaDate()));
		evt.setNumeroTechnique(evenementLR.getId());
		return evt;

	}

	/**
	 * Sérialise une objet.
	 * @param writer stream
	 * @param object objet à sérialiser
	 * @throws IOException Arrive lors de la sérialisation
	 * @throws EvenementFiscalException Cette exception survient si l'object à sérialiser n'est pas valide.
	 */
	private void writeXml(OutputStream writer, XmlObject object) throws IOException, EvenementFiscalException {
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: " + error.getErrorCode() + " " + error.getMessage() + "\n");
				builder.append("Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
				throw new EvenementFiscalException( builder.toString());
			}
		}
		object.save(writer, new XmlOptions().setSaveOuter());
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
