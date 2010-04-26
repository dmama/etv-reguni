package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Listener qui reçoit les messages JMS concernant les événements externes, les valide, les transforme et les transmet au handler approprié.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneListenerImpl extends EsbMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementExterneListenerImpl.class);

	private EvenementExterneHandler handler;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementExterneHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		AuthenticationHelper.setPrincipal("JMS-EvtExt");

		try {
			final String message = esbMessage.getBodyAsString();
			final String businessId = esbMessage.getBusinessId();
			onMessage(message, businessId);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.resetAuthentication();
		}
	}

	/**
	 * Traite le message XML reçu pour en extraire les informations de l'événement externe et les persister en base. La methode onMessage() ne doit être appelée explicitement Seul le mechanisme JMS doit
	 * l'appeler
	 *
	 * @param message    le message JMS sous forme string
	 * @param businessId l'identifiant métier du message
	 * @throws Exception en cas d'erreur
	 */
	protected void onMessage(String message, String businessId) throws Exception {

		final EvenementExterne event = string2event(message, businessId);
		handler.onEvent(event);
	}

	public static EvenementExterne string2event(String message, String businessId) throws XmlException {

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

		final EvenementExterne event;

		if (evt instanceof EvenementImpotSourceQuittanceDocument) {

			final EvenementImpotSourceQuittanceType type = ((EvenementImpotSourceQuittanceDocument) evt).getEvenementImpotSourceQuittance();

			QuittanceLR q = new QuittanceLR();
			q.setMessage(message);
			q.setBusinessId(businessId);
			q.setDateEvenement(cal2date(type.getDateQuittance()));
			q.setDateTraitement(new Date());
			q.setDateDebut(cal2regdate(type.getDateDebutPeriode()));
			q.setDateFin(cal2regdate(type.getDateFinPeriode()));
			q.setType(TypeQuittance.valueOf(type.getTypeQuittance().toString()));
			q.setTiersId(Long.valueOf(type.getNumeroTiers()));

			event = q;
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + evt.getClass());
		}
		
		return event;
	}

	private static Date cal2date(Calendar cal) {
		return cal == null ? null : cal.getTime();
	}

	private static RegDate cal2regdate(Calendar cal) {
		return cal == null ? null : RegDate.get(cal.getTime());
	}
}
