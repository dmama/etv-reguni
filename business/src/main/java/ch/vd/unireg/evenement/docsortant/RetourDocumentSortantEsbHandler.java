package ch.vd.unireg.evenement.docsortant;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
import ch.vd.unireg.xml.event.docsortant.retour.v3.Quittance;

public class RetourDocumentSortantEsbHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetourDocumentSortantEsbHandler.class);

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;
	private RetourDocumentSortantHandler handler;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setHandler(RetourDocumentSortantHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		LOGGER.info("Arrivée d'un message JMS en retour de l'annonce d'un document sortant : '" + message.getBusinessId() + "' en réponse à '" + message.getBusinessCorrelationId() + "'");

		AuthenticationHelper.pushPrincipal("JMS-RetourDocumentSortant");
		try {
			onMessage(message);

			// flush la session avant d'enlever l'information du principal
			hibernateTemplate.flush();
		}
		catch (EsbBusinessException e) {
			// flush la session avant d'enlever l'information du principal
			hibernateTemplate.flush();
			throw e;
		}
		catch (Exception e) {
			// les erreurs qui restent sont des erreurs transientes ou des bugs...
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws EsbBusinessException {
		final long start = System.nanoTime();
		try {
			final Quittance quittance = parse(message.getBodyAsSource());
			handler.onQuittance(quittance, EsbMessageHelper.extractCustomHeaders(message));
		}
		catch (JAXBException | SAXException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		finally {
			final long end = System.nanoTime();
			long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
			if(duration > 1000) {
				LOGGER.error("onMessage = " + duration + " ms");
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.event.docsortant.retour.v3.ObjectFactory.class.getPackage().getName());
	}

	private Quittance parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (Quittance) u.unmarshal(message);
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
			final Source[] source = XmlUtils.toSourcesArray("event/dperm/typeSimpleDPerm-1.xsd",
			                                                "event/docsortant/quittanceRepElec-3.xsd");
			schemaCache = sf.newSchema(source);
		}
	}
}
