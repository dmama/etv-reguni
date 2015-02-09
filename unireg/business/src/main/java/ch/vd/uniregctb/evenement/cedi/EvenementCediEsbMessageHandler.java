package ch.vd.uniregctb.evenement.cedi;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

public class EvenementCediEsbMessageHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCediEsbMessageHandler.class);

	private Map<Class<?>, DossierElectroniqueHandler<?>> handlers;
	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandlers(List<DossierElectroniqueHandler<?>> handlerList) {
		final Map<Class<?>, DossierElectroniqueHandler<?>> map = new HashMap<>(handlerList.size());
		for (DossierElectroniqueHandler<?> deh : handlerList) {
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
		this.jaxbContext = JAXBContext.newInstance(handlers.keySet().toArray(new Class[handlers.size()]));
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-EvtCedi");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée du message CEDI n°" + businessId);
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
		final Object event = parse(message);
		final DossierElectroniqueHandler handler = handlers.get(event.getClass());
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
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final List<Source> sources = new ArrayList<>(handlers.size());
			for (DossierElectroniqueHandler<?> handler : handlers.values()) {
				final ClassPathResource resource = handler.getRequestXSD();
				sources.add(new StreamSource(resource.getURL().toExternalForm()));
			}
			schemaCache = sf.newSchema(sources.toArray(new Source[sources.size()]));
		}
	}

	private Object parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return u.unmarshal(message);
	}
}
