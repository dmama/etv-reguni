package ch.vd.unireg.evenement.declaration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.xml.event.declaration.v2.DeclarationEvent;

public class EvenementDeclarationEsbHandlerV2 implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDeclarationEsbHandlerV2.class);

	private Map<Class<? extends DeclarationEvent>, EvenementDeclarationHandler<? extends DeclarationEvent>> handlers;
	private HibernateTemplate hibernateTemplate;

	private JAXBContext jaxbContext;
	private Schema schemaCache;

	public void setHandlers(Map<Class<? extends DeclarationEvent>, EvenementDeclarationHandler<? extends DeclarationEvent>> handlers) {
		this.handlers = handlers;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(handlers.keySet().toArray(new Class[0]));
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		AuthenticationHelper.pushPrincipal("JMS-EvtDeclaration");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée de l'événement de Déclaration n°" + businessId);
			onMessage(message);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EsbBusinessException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
			throw e;
		}
		catch (JAXBException | SAXException | IOException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou des bugs
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws IOException, JAXBException, SAXException, EsbBusinessException {
		try {
			// on décode l'événement entrant
			final DeclarationEvent event = parse(message.getBodyAsSource());
			LOGGER.info(String.format("Arrivée d'un événement de déclaration (BusinessID = '%s') %s", message.getBusinessId(), event));

			// on le traite
			handle(event, EsbMessageHelper.extractCustomHeaders(message));
		}
		catch (UnmarshalException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
	}

	private DeclarationEvent parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (DeclarationEvent) element.getValue();
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
			final LinkedHashSet<String> pathes = new LinkedHashSet<>();
			for (EvenementDeclarationHandler<?> handler : handlers.values()) {
				pathes.addAll(handler.getXSDs());
			}
			schemaCache = sf.newSchema(XmlUtils.toSourcesArray(pathes));
		}
	}

	private void handle(DeclarationEvent event, Map<String, String> headers) throws EsbBusinessException {
		final EvenementDeclarationHandler handler = handlers.get(event.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Handler inconnu pour l'événement [" + event.getClass() + "]");
		}
		//noinspection unchecked
		handler.handle(event, headers);
	}
}
