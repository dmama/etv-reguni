package ch.vd.unireg.evenement.retourdi;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

public class EvenementRetourDiEsbMessageHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementRetourDiEsbMessageHandler.class);

	private Map<Class<?>, RetourDiHandler<?>> handlers;
	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandlers(List<RetourDiHandler<?>> handlerList) {
		final Map<Class<?>, RetourDiHandler<?>> map = new HashMap<>(handlerList.size());
		for (RetourDiHandler<?> deh : handlerList) {
			map.put(deh.getHandledClass(), deh);
		}
		this.handlers = map;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(handlers.keySet().toArray(new Class[0]));
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-EvtRetourDI");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée du message de retour de DI n°" + businessId);
			final Source body = message.getBodyAsSource();
			onMessage(body, EsbMessageHelper.extractCustomHeaders(message));

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
		catch (UnmarshalException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(Source message, Map<String, String> incomingHeaders) throws JAXBException, SAXException, IOException, EsbBusinessException {
		final Object parsingResult = parse(message);
		final Object event;
		if (parsingResult instanceof JAXBElement) {
			// des fois, le parsing renvoie un JAXBElement...
			event = ((JAXBElement) parsingResult).getValue();
		}
		else {
			// ... et des fois pas ...
			event = parsingResult;
		}
		final RetourDiHandler handler = handlers.get(event.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Aucun handler connu pour la requête [" + event.getClass() + ']');
		}

		//noinspection unchecked
		handler.doHandle(event, incomingHeaders);
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
			for (RetourDiHandler handler : handlers.values()) {
				pathes.add(handler.getRequestXSD());
			}
			schemaCache = sf.newSchema(XmlUtils.toSourcesArray(pathes));
		}
	}

	private Object parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return u.unmarshal(message);
	}
}
