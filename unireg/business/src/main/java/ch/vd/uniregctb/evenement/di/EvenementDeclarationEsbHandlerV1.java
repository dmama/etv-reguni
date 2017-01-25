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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.event.di.common.v1.EvenementDeclarationImpotContext;
import ch.vd.unireg.xml.event.di.input.v1.ObjectFactory;
import ch.vd.unireg.xml.event.di.input.v1.QuittancementDeclarationImpot;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.declaration.EvenementDeclarationException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

public class EvenementDeclarationEsbHandlerV1 implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDeclarationEsbHandlerV1.class);

	private EvenementDeclarationHandler handler;

	private HibernateTemplate hibernateTemplate;

	private Schema schemaCache;

	private JAXBContext jaxbContext;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementDeclarationHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws EsbBusinessException {

		AuthenticationHelper.pushPrincipal("JMS-EvtDeclaration");

		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée de l'événement de Déclaration n°" + businessId);
			onMessage(message, EsbMessageHelper.extractCustomHeaders(message));

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EvenementDeclarationException e) {
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
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private EvenementDeclaration parse(Source message, String businessId) throws JAXBException, SAXException, IOException {

		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);

		// Crée l'événement correspondant
		final Object event = element.getValue();
		if (event == null) {
			return null;
		}
		if (event instanceof QuittancementDeclarationImpot) {
			final QuittancementDeclarationImpot evtQuittancement = ((QuittancementDeclarationImpot) event);
			final EvenementDeclarationImpotContext contextQuittancement = evtQuittancement.getContext();
			final QuittancementDI quittancementDI = new QuittancementDI();
			ch.vd.unireg.xml.common.v1.Date dateEvt = contextQuittancement.getDate();
			RegDate dateQuittancement = RegDate.get(dateEvt.getYear(), dateEvt.getMonth(), dateEvt.getDay());
			quittancementDI.setNumeroContribuable((long) contextQuittancement.getNumeroContribuable());
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

	private void onMessage(EsbMessage message, Map<String, String> incomingHeaders) throws IOException, JAXBException, SAXException, EvenementDeclarationException {
		final EvenementDeclaration event = parse(message.getBodyAsSource(), message.getBusinessId());
		handler.onEvent(event, incomingHeaders);
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
}
