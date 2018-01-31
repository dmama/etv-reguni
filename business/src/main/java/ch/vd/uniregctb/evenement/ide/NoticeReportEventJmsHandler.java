package ch.vd.uniregctb.evenement.ide;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestIdentification;
import ch.vd.evd0022.v3.NoticeRequestREEHeader;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0024.v3.ObjectFactory;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntSchemaHelper;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;

/**
 * @author Raphaël Marmier, 2016-08-22, <raphael.marmier@vd.ch>
 */
public class NoticeReportEventJmsHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoticeReportEventJmsHandler.class);

	private ReponseIDEProcessor reponseIDEProcessor;

	private Schema schemaCache;

	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReponseIDEProcessor(ReponseIDEProcessor reponseIDEProcessor) {
		this.reponseIDEProcessor = reponseIDEProcessor;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws EsbBusinessException {

		// traitement du message
		AuthenticationHelper.pushPrincipal("JMS-rapportAnnonceIDE(" + message.getMessageId() + ')');

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS événement rapport d'annonce à l'IDE {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			final Source content = message.getBodyAsSource();
			onEvenementRapportAnnonceIDE(content);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EsbBusinessException e) {
			// on a un truc qui a sauté au moment de l'arrivée de l'événement (test métier)
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		catch (RuntimeException e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Réception du message JMS événement rapport d'annonce à l'IDE {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Déroulement des opérations :
	 * <ol>
	 *     <li>Décodage de l'XML reçu</li>
	 *     <li>Création de l'objet de l'evenement, avec une validation plus poussée.</li>
	 *     <li>Lancement du traitement</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre entreprises
	 * @throws EsbBusinessException en cas de problème <i>métier</i>
	 */
	private void onEvenementRapportAnnonceIDE(Source xml) throws EsbBusinessException {

		final NoticeRequestReport message = decodeNoticeRequestReport(xml);
		if (message.getNoticeRequest().getNoticeRequestHeader() == null) {
			// SIFISC-27766 on ignore les annonces REE pour l'instant
			final String requestId = Optional.of(message)
					.map(NoticeRequestReport::getNoticeRequest)
					.map(NoticeRequest::getNoticeRequestREEHeader)
					.map(NoticeRequestREEHeader::getNoticeRequestIdentification)
					.map(NoticeRequestIdentification::getNoticeRequestId)
					.orElse(null);
			Audit.warn("Arrivée d'un événement d'annonce au REE (requestId=[" + requestId + "]). Le traitement des événements REE n'est pas implémenté, on l'ignore.");
			return;
		}

		final AnnonceIDEEnvoyee annonce;
		// En lieu et place d'une pénible validation, pour attrapper les NPE en cas de champs métiers pas correctement remplis par RCEnt.
		try {
			annonce = RCEntAnnonceIDEHelper.buildAnnonceIDE(message);
		} catch (RuntimeException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e); // Avec les amitiés de la baronne
		}

		final String texteErreurs = annonce.getStatut().getTexteErreurs();

		Audit.info(annonce.getNumero(),
		           String.format("Arrivée de l'événement de rapport d'annonce à l'IDE %d (%s du %s), no IDE %s%s, statut %s le %s.%s",
		                         annonce.getNumero(),
		                         annonce.getType(),
		                         annonce.getDateAnnonce() == null ? null : DateHelper.dateTimeToDisplayString(annonce.getDateAnnonce()),
		                         annonce.getNoIde(),
		                         annonce.getNoIdeRemplacant() == null ? "" : ", no IDE remplacant " + annonce.getNoIdeRemplacant(),
		                         annonce.getStatut() == null ? null : annonce.getStatut().getStatut(),
		                         annonce.getStatut() == null ? null : DateHelper.dateTimeToDisplayString(annonce.getStatut().getDateStatut()),
		                         StringUtils.isBlank(texteErreurs) ? " Pas de messages d'erreur." : " Erreurs: " + texteErreurs + "."
		           )
		);

		try {
			reponseIDEProcessor.traiterReponseAnnonceIDE(annonce);
		}
		catch (RuntimeException | ReponseIDEProcessorException e) {
			Audit.error(annonce.getNumero(), String.format("Erreur au cours du traitement de l'événement de rapport d'annonce à l'IDE %d:", annonce.getNumero()));
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e); // On est conscient qu'on est au delà du simple xml invalide. TODO: Demander l'ajout d'un code spécifique?
		}
	}

	/*
		Décodage de l'événement brut, et si c'est ok, on crée l'événement. Dans tous les cas pas ok
		une exception adéquate doit être lancée pour être remontée à l'ESB. Interdit de sortir d'ici sans un objet.
	 */
	@NotNull
	private NoticeRequestReport decodeNoticeRequestReport(Source xml) throws EsbBusinessException {
		try {
			return parse(xml);
		}
		catch (JAXBException e) {
			final Throwable src = e.getLinkedException();
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, src != null ? src : e);
		}
		catch (SAXException | IOException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e);
		}
	}

	private NoticeRequestReport parse(Source xml) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (NoticeRequestReport) ((JAXBElement) u.unmarshal(xml)).getValue();
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
			final Source[] source = RCEntSchemaHelper.getRCEntClasspathSources();
			schemaCache = sf.newSchema(source);
		}
	}
}
