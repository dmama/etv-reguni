package ch.vd.uniregctb.evenement.externe;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.ListeType;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Listener qui reçoit les messages JMS concernant les événements externes, les valide, les transforme et les transmet au handler approprié.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneListenerImpl extends EsbMessageListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementExterneListenerImpl.class);

	private EvenementExterneHandler handler;

	private HibernateTemplate hibernateTemplate;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementExterneHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-EvtExt");

		try {
			final String businessId = esbMessage.getBusinessId();
			LOGGER.info("Arrivée du message externe n°" + businessId);

			final String message = esbMessage.getBodyAsString();
			onMessage(message, businessId);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (XmlException e) {
			// problème de validation de l'XML reçu
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(esbMessage, e.getMessage(), e, ErrorType.TECHNICAL, "");
		}
		catch (EvenementExterneException e) {
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(esbMessage, e.getMessage(), e, ErrorType.BUSINESS, "");
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
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
	protected void onMessage(String message, String businessId) throws XmlException, EvenementExterneException {

		final EvenementExterne event = string2event(message, businessId);
		if (event != null) {
			handler.onEvent(event);
		}
		else{
			LOGGER.info("Message ignoré: Evenement de type LC n°" + businessId);
		}
	}

	protected static EvenementExterne string2event(String message, String businessId) throws XmlException, EvenementExterneException {

		final XmlObject evt = XmlObject.Factory.parse(message);
		if (evt == null) {
			throw new RuntimeException("Unexpected error");
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

		if (evt instanceof EvtQuittanceListeDocument) {

			final EvtQuittanceListeDocument documentEvenement = (EvtQuittanceListeDocument) evt;
			final EvtQuittanceListeDocument.EvtQuittanceListe evtQuittanceListe = documentEvenement.getEvtQuittanceListe();
			if (isEvenementLR(evtQuittanceListe)) {

				final QuittanceLR q = new QuittanceLR();
				q.setMessage(message);
				q.setBusinessId(businessId);
				q.setDateEvenement(cal2date(evtQuittanceListe.getTimestampEvtQuittance()));
				q.setDateTraitement(DateHelper.getCurrentDate());
				final Calendar dateDebut = evtQuittanceListe.getIdentificationListe().getPeriodeDeclaration().getDateDebut();
				q.setDateDebut(cal2regdate(dateDebut));
				final Calendar dateFin = evtQuittanceListe.getIdentificationListe().getPeriodeDeclaration().getDateFin();
				q.setDateFin(cal2regdate(dateFin));
				q.setType(TypeQuittance.valueOf(evtQuittanceListe.getTypeEvtQuittance().toString()));
				final int numeroDebiteur = evtQuittanceListe.getIdentificationListe().getNumeroDebiteur();
				q.setTiersId((long) numeroDebiteur);

				event = q;
			}
			else {
				event = null;
			}
		}
		else {
			throw new EvenementExterneException("Type d'événement inconnu = " + evt.getClass());
		}

		return event;
	}

	private static boolean isEvenementLR(EvtQuittanceListeDocument.EvtQuittanceListe evtQuittanceListe) {
		final ListeType.Enum listeType = evtQuittanceListe.getIdentificationListe().getTypeListe();
		return ListeType.LR.equals(listeType);
	}

	private static Date cal2date(Calendar cal) {
		return cal == null ? null : cal.getTime();
	}

	private static RegDate cal2regdate(Calendar cal) {
		return cal == null ? null : RegDate.get(cal.getTime());
	}

	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
