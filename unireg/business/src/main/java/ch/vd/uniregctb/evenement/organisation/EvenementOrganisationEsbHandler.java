package ch.vd.uniregctb.evenement.organisation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0001.v5.ObjectFactory;
import ch.vd.evd0022.v1.Header;
import ch.vd.evd0022.v1.Notice;
import ch.vd.evd0022.v1.NoticeOrganisation;
import ch.vd.evd0022.v1.NoticeRoot;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Listener des événements organisation envoyés par RCEnt au travers de l'ESB
 */
public class EvenementOrganisationEsbHandler implements EsbMessageHandler, InitializingBean, SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationEsbHandler.class);

	/*
		Configuration des schémas applicables pour le décodage des annonces RCEnt
	 */
	private static final String[] RCENT_XSDS = new String[]{"eVD-0021-1-0.xsd", "eVD-0022-1-0.xsd", "eVD-0023-1-0.xsd", "eVD-0024-1-0.xsd"};

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private EvenementOrganisationReceptionHandler receptionHandler;
	private Set<TypeEvenementOrganisation> ignoredEventTypes;
	private EvenementOrganisationProcessingMode processingMode;
	private boolean running;

	/**
	 * Renderer d'un événement organisation à la réception (on ne renvoie que les données fournies à la réception de l'événement)
	 */
	private static final StringRenderer<NoticeRoot> RECEPTION_EVT_ORGANISATION_RENDERER = new StringRenderer<NoticeRoot>() {
		@Override
		public String toString(NoticeRoot message) {
			Header header = message.getHeader();
			Notice notice = header.getNotice();
			NoticeOrganisation content = message.getNoticeOrganisation().get(0);
			return String.format("id=%d, type=%s, date=%s, senderId=%s, refData='%s', noOrganisation=%d, nom='%s'",
			                     notice.getNoticeId(),
			                     notice.getTypeOfNotice(),
			                     notice.getNoticeDate(),
			                     header.getSenderIdentification(),
			                     header.getSenderReferenceData(),
			                     content.getOrganisationIdentification().getCantonalId(),
			                     content.getOrganisation().getOrganisationName()
			);
		}
	};

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReceptionHandler(EvenementOrganisationReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIgnoredEventTypes(Set<TypeEvenementOrganisation> ignoredEventTypes) {
		this.ignoredEventTypes = ignoredEventTypes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessingMode(EvenementOrganisationProcessingMode processingMode) {
		this.processingMode = processingMode;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws EsbBusinessException {

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS événement organisation {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			onEvenementOrganisation(message.getBodyAsSource());
		}
		catch (EvenementOrganisationEsbException e) {
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
				LOGGER.info(String.format("Réception du message JMS événement organisation {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	/**
	 * Déroulement des opérations :
	 * <ol>
	 *     <li>Décodage de l'XML reçu</li>
	 *     <li>Les événements de types ignorés provoquent un log et c'est tout...</li>
	 *     <li>Création de l'objet de l'evenement, avec une validation plus poussée.</li>
	 *     <li>Sauvegarde de l'événement (dans une transaction séparée) dans l'état {@link ch.vd.uniregctb.type.EtatEvenementOrganisation#A_TRAITER A_TRAITER}</li>
	 *     <li>Notification du moteur de traitement de l'arrivée d'un nouvel événement pour l'organisation</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre entreprises
	 * @throws EvenementOrganisationEsbException en cas de problème <i>métier</i>
	 */
	private void onEvenementOrganisation(Source xml) throws EvenementOrganisationEsbException {

		NoticeRoot message = decodeEvenementOrganisation(xml);

		if (isIgnored(message)) {
			onIgnoredEvent(message);
			return;
		}

		final EvenementOrganisation event = createEvenementOrganisation(message);

		saveIncomingEvent(event);

		// à partir d'ici, l'événement est sauvegardé en base... Il n'est donc plus question
		// de rejetter en erreur (ou exception) le message entrant...
		try {
			receptionHandler.handleEvent(event, processingMode);
		}
		catch (Exception e) {
			// le traitement sera re-tenté au plus tard au prochain démarrage de l'application...
			LOGGER.error(String.format("Erreur à la réception de l'événement organisation %d", event.getId()), e);
		}
	}

	/*
		Décodage de l'événement brut, et si c'est ok, on crée l'événement. Dans tous les cas pas ok
		une exception adéquate doit être lancée pour être remontée à l'ESB. Interdit de sortir d'ici sans un objet.
	 */
	@NotNull
	private NoticeRoot decodeEvenementOrganisation(Source xml) throws EvenementOrganisationEsbException {
		try {
			return parse(xml);
		}
		catch (JAXBException e) {
			final Throwable src = e.getLinkedException();
			throw new EvenementOrganisationEsbException(EsbBusinessCode.XML_INVALIDE, src != null ? src : e);
		}
		catch (SAXException | IOException e) {
			throw new EvenementOrganisationEsbException(EsbBusinessCode.XML_INVALIDE, e);
		}
	}

	/*
		Validation du message décodé, et seulement si c'est ok, on crée l'événement. Dans tous les cas pas ok
		une exception adéquate doit être lancée pour être remontée à l'ESB. Interdit de sortir d'ici sans un objet.
	 */
	@NotNull
	private EvenementOrganisation createEvenementOrganisation(NoticeRoot message) throws EvenementOrganisationEsbException {

		try {
			checkValidIncomingEventData(message);

			return new EvenementOrganisation(message);
		}
		catch (RuntimeException e) {
			throw new EvenementOrganisationEsbException(EsbBusinessCode.EVT_ORGANISATION, e);
		}
	}

	/*
	 On valide sur le message brut car il peut y avoir NullPointerException à la création de l'instance de l'événement.

	 En réalité, l'absence d'une bonne partie de ces champs entrainera une erreur de déserialisation JAXB en amont, car
	 ils sont obligatoire de part le xsd. Mais on ne contrôle pas le xsd.
	  */
	private static void checkValidIncomingEventData(NoticeRoot message) throws EvenementOrganisationEsbException {
		final List<String> attributs = new ArrayList<>();

		final Notice notice = message.getHeader().getNotice();
		if (notice.getNoticeId() == null) {
			attributs.add("notice id");
		}
		if (notice.getNoticeDate() == null) {
			attributs.add("notice date");
		}
		if (message.getNoticeOrganisation().get(0).getOrganisationIdentification().getCantonalId() == null) {
			attributs.add("cantonal id");
		}
		if (notice.getTypeOfNotice() == null) {
			attributs.add("type of notice");
		}
		if (message.getHeader().getSenderIdentification() == null) {
			attributs.add("sender identification");
		}

		final int size = attributs.size();
		if (size > 0) {
			final String details = RECEPTION_EVT_ORGANISATION_RENDERER.toString(message);
			final String msg;
			if (size == 1) {
				msg = String.format("L'attribut '%s' est obligatoire pour un événement organisation à l'entrée dans Unireg (%s).", attributs.get(0), details);
			}
			else {
				final StringBuilder b = new StringBuilder("Les attributs ");
				for (int i = 0 ; i < size; ++ i) {
					final String attr = attributs.get(i);
					if (i > 0) {
						if (i < size - 1) {
							b.append(", ");
						}
						else {
							b.append(" et ");
						}
					}
					b.append('\'').append(attr).append('\'');
				}
				b.append(" sont obligatoires pour un événement organisation à l'entrée dans Unireg (");
				b.append(details);
				b.append(").");
				msg = b.toString();
			}
			throw new EvenementOrganisationEsbException(EsbBusinessCode.EVT_ORGANISATION, msg);
		}
	}

	/*
		Action lorsqu'on ignore le message entrant. On tient compte du fait que des exceptions peuvent être lancées en parcourant le message.
	 */
	protected void onIgnoredEvent(NoticeRoot message) throws EvenementOrganisationEsbException {
		try {
			Notice notice = message.getHeader().getNotice();
			Audit.info(notice.getNoticeId().longValue(), String.format("Evénement organisation ignoré (id=%d, type=%s)",
			                                                           notice.getNoticeId(),
			                                                           notice.getTypeOfNotice().name()));
		}
		catch (RuntimeException e) {
			throw new EvenementOrganisationEsbException(EsbBusinessCode.EVT_ORGANISATION, e);
		}
	}

	/*
		Contrôle si on doit ignorer le message entrant. On tient compte du fait que des exceptions peuvent être lancées en parcourant le message.
	 */
	private boolean isIgnored(NoticeRoot message) throws EvenementOrganisationEsbException {
		try {
			return ignoredEventTypes != null && ignoredEventTypes.contains(TypeEvenementOrganisation.valueOf(message.getHeader().getNotice().getTypeOfNotice().name()));
		}
		catch (RuntimeException e) {
			throw new EvenementOrganisationEsbException(EsbBusinessCode.EVT_ORGANISATION, e);
		}
	}

	public static StringRenderer<NoticeRoot> getReceptionEvtOrganisationRenderer() {
		return RECEPTION_EVT_ORGANISATION_RENDERER;
	}

	private EvenementOrganisation saveIncomingEvent(EvenementOrganisation event) {
		final Long id = event.getId();
		Audit.info(id, String.format("Arrivée de l'événement organisation %d (%s au %s)", id, event.getType(), RegDateHelper.dateToDisplayString(event.getDateEvenement())));
		return receptionHandler.saveIncomingEvent(event);
	}

	private NoticeRoot parse(Source xml) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (NoticeRoot) u.unmarshal(xml);
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
			final Source[] source = getClasspathSources(RCENT_XSDS);
			schemaCache = sf.newSchema(source);
		}
	}

	private static Source[] getClasspathSources(String... pathes) throws IOException {
		final Source[] sources = new Source[pathes.length];
		for (int i = 0, pathLength = pathes.length; i < pathLength; i++) {
			final String path = pathes[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}


	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			final StringRenderer<TypeEvenementOrganisation> renderer = new StringRenderer<TypeEvenementOrganisation>() {
				@Override
				public String toString(TypeEvenementOrganisation type) {
					return String.format("%s (%s)", type, type.getName());
				}
			};
			LOGGER.info(String.format("Liste des événements organisation ignorés en mode %s : %s", processingMode, CollectionsUtils.toString(ignoredEventTypes, renderer, ", ", "Aucun")));
		}

		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}
}
