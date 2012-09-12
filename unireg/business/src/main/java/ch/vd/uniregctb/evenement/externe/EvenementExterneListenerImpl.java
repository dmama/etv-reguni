package ch.vd.uniregctb.evenement.externe;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.xml.sax.SAXException;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.unireg.xml.event.lr.quittance.v1.EvtQuittanceListe;
import ch.vd.unireg.xml.event.lr.quittance.v1.Liste;
import ch.vd.unireg.xml.event.lr.quittance.v1.ObjectFactory;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Listener qui reçoit les messages JMS concernant les événements externes, les valide, les transforme et les transmet au handler approprié.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneListenerImpl extends EsbMessageEndpointListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementExterneListenerImpl.class);

	private EvenementExterneHandler handler;
	private HibernateTemplate hibernateTemplate;
	private Schema schemaCache;

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

			onMessage(esbMessage, businessId);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
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
	protected void onMessage(EsbMessage message, String businessId) throws Exception {

		final EvenementExterne event = parse(message.getBodyAsSource(), message.getBodyAsString(), businessId);
		if (event != null) {
			handler.onEvent(event);
		}
		else{
			LOGGER.info("Message ignoré: Evenement de type LC n°" + businessId);
		}
	}

	protected EvenementExterne parse(Source source, String bodyAsString, String businessId) throws JAXBException, IOException, SAXException {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());

		final Object event = u.unmarshal(source);
		if (event == null) {
			return null;
		}

		// Crée l'événement correspondant
		if (event instanceof EvtQuittanceListe) {
			final EvtQuittanceListe eq = (EvtQuittanceListe) event;
			if (isEvenementLR(eq)) {
				final QuittanceLR quittance = new QuittanceLR();
				quittance.setMessage(bodyAsString);
				quittance.setBusinessId(businessId);
				quittance.setDateEvenement(XmlUtils.xmlcal2date(eq.getTimestampEvtQuittance()));
				quittance.setDateTraitement(DateHelper.getCurrentDate());
				final XMLGregorianCalendar dateDebut = eq.getIdentificationListe().getPeriodeDeclaration().getDateDebut();
				quittance.setDateDebut(XmlUtils.xmlcal2regdate(dateDebut));
				final XMLGregorianCalendar dateFin = eq.getIdentificationListe().getPeriodeDeclaration().getDateFin();
				quittance.setDateFin(XmlUtils.xmlcal2regdate(dateFin));
				quittance.setType(TypeQuittance.valueOf(eq.getTypeEvtQuittance().toString()));
				final int numeroDebiteur = eq.getIdentificationListe().getNumeroDebiteur();
				quittance.setTiersId((long) numeroDebiteur);
				return quittance;
			}
			else {
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	private Schema getRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			buildRequestSchema();
		}
		return schemaCache;
	}

	private synchronized void buildRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final ClassPathResource resource = new ClassPathResource("event/lr/evtQuittanceListe-v1.xsd");
			Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}

	private static boolean isEvenementLR(EvtQuittanceListe event) {
		final Liste type = event.getIdentificationListe().getTypeListe();
		return type == Liste.LR;
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
