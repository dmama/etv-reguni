package ch.vd.uniregctb.evenement.di;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.xml.sax.SAXException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.unireg.xml.event.di.common.v1.EvenementDeclarationImpotContext;
import ch.vd.unireg.xml.event.di.input.v1.ObjectFactory;
import ch.vd.unireg.xml.event.di.input.v1.QuittancementDeclarationImpot;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

public class EvenementDeclarationListenerImpl extends EsbMessageListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementDeclarationListenerImpl.class);

	private EvenementDeclarationHandler handler;

	private HibernateTemplate hibernateTemplate;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	private Schema schemaCache;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementDeclarationHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-EvtDeclaration");

		try {
			final String businessId = message.getBusinessId();
			onMessage(message);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EvenementDeclarationException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.BUSINESS, "");

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

	private void onMessage(EsbMessage message) throws Exception {
		final EvenementDeclaration event = parse(message.getBodyAsSource(), message.getBusinessId());
		handler.onEvent(event);
	}

	private EvenementDeclaration parse(Source message, String businessId) throws JAXBException, SAXException, IOException {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);

		// Crée l'événement correspondant
		final Object event = element.getValue();
		if (event == null) {
			return null;
		}
		if (event instanceof QuittancementDeclarationImpot) {
				LOGGER.info("Arrivée du message de quittancement de DI n°" + businessId);
			final QuittancementDeclarationImpot evtQuittancement = ((QuittancementDeclarationImpot) event);
			final EvenementDeclarationImpotContext contextQuittancement = evtQuittancement.getContext();
			final QuittancementDI quittancementDI = new QuittancementDI();
			ch.vd.unireg.xml.common.v1.Date dateEvt = contextQuittancement.getDate();
			RegDate dateQuittancement = RegDate.get(dateEvt.getYear(), dateEvt.getMonth(), dateEvt.getDay());
			quittancementDI.setNumeroContribuable(new Long(contextQuittancement.getNumeroContribuable()));
			quittancementDI.setDate(dateQuittancement);
			quittancementDI.setPeriodeFiscale(contextQuittancement.getPeriodeFiscale());
			quittancementDI.setSource(evtQuittancement.getSource());
			quittancementDI.setBusinessId(businessId);
			LOGGER.info("Contenu du message : " + quittancementDI);

			return quittancementDI;
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
			final ClassPathResource resource = handler.getRequestXSD();
			Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}


	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
