package ch.vd.uniregctb.evenement.externe;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;

/**
 * Listener qui reçoit les messages JMS concernant les événements externes, les valide, les transforme et les transmet au handler approprié.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneEsbHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementExterneEsbHandler.class);

	private EvenementExterneHandler handler;
	private HibernateTemplate hibernateTemplate;
	private Schema schemaCache;
	private Map<Class<?>, EvenementExterneConnector> connectorMap;
	private JAXBContext jaxbContext;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementExterneHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setConnectors(List<EvenementExterneConnector> connectors) {
		if (connectors != null && !connectors.isEmpty()) {
			this.connectorMap = new HashMap<>(connectors.size());
			for (EvenementExterneConnector connector : connectors) {
				final Class supportedClass = connector.getSupportedClass();
				final EvenementExterneConnector previous = this.connectorMap.put(supportedClass, connector);
				if (previous != null) {
					throw new IllegalArgumentException("Plusieurs connecteurs pour la même classe : " + supportedClass);
				}
			}
		}
		else {
			this.connectorMap = Collections.emptyMap();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(connectorMap.keySet().toArray(new Class[connectorMap.size()]));
	}

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-EvtExt");

		try {
			final String businessId = esbMessage.getBusinessId();
			LOGGER.info("Arrivée du message externe n°" + businessId);

			onMessage(esbMessage, businessId);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EvenementExterneException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EsbBusinessException(EsbBusinessCode.EVT_EXTERNE, e.getMessage(), e);
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
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

	protected EvenementExterne parse(Source source, String bodyAsString, String businessId) throws IOException, EsbBusinessException {

		try {
			final Unmarshaller u = jaxbContext.createUnmarshaller();
			u.setSchema(getRequestSchema());

			final Object xml = u.unmarshal(source);
			if (xml == null) {
				return null;
			}

			final EvenementExterneConnector connector = connectorMap.get(xml.getClass());
			if (connector == null) {
				throw new EsbBusinessException(EsbBusinessCode.EVT_EXTERNE, "Evénement non supporté", null);
			}

			//noinspection unchecked
			final EvenementExterne event = connector.parse(xml);
			if (event != null) {
				event.setBusinessId(businessId);
				event.setMessage(bodyAsString);
			}
			return event;
		}
		catch (JAXBException | SAXException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
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

			final List<Source> sources = new ArrayList<>(connectorMap.size());
			for (EvenementExterneConnector connector : connectorMap.values()) {
				sources.add(new StreamSource(connector.getRequestXSD().getURL().toExternalForm()));
			}
			schemaCache = sf.newSchema(sources.toArray(new Source[sources.size()]));
		}
	}
}
