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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0001.v5.ObjectFactory;
import ch.vd.evd0022.v1.NoticeRoot;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
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

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private EvenementOrganisationReceptionHandler receptionHandler;
	private EvenementOrganisationRecuperateur recuperateur;
	private int delaiRecuperationMinutes = 0;
	private RecuperationThread recuperationThread;
	private Set<TypeEvenementOrganisation> ignoredEventTypes;
	private Set<TypeEvenementOrganisation> eventTypesWithNullEventDateReplacement;
	private EvenementOrganisationProcessingMode processingMode;
	private boolean running;

	/**
	 * Renderer d'un événement organisation à la réception (on ne renvoie que les données fournies à la réception de l'événement)
	 */
	private static final StringRenderer<EvenementOrganisation> RECEPTION_EVT_ORGANISATION_RENDERER = new StringRenderer<EvenementOrganisation>() {
		@Override
		public String toString(EvenementOrganisation evt) {
			return String.format("id=%d, refData=%s, type=%s, date=%s", evt.getId(), evt.getRefDataEmetteur(), evt.getType(), evt.getDateEvenement());
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
	public void setEventTypesWithNullEventDateReplacement(Set<TypeEvenementOrganisation> eventTypesWithNullEventDateReplacement) {
		this.eventTypesWithNullEventDateReplacement = eventTypesWithNullEventDateReplacement;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRecuperateur(EvenementOrganisationRecuperateur recuperateur) {
		this.recuperateur = recuperateur;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDelaiRecuperationMinutes(int delaiRecuperationMinutes) {
		this.delaiRecuperationMinutes = delaiRecuperationMinutes;
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
			final Source content = message.getBodyAsSource();
			final String visaMutation = EvenementOrganisationSourceHelper.getVisaCreation(message);
			AuthenticationHelper.pushPrincipal(visaMutation);
			try {
				onEvenementOrganisation(content);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
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
	 *     <li>Sauvegarde de l'événement correspondant (dans une transaction séparée) dans l'état {@link ch.vd.uniregctb.type.EtatEvenementOrganisation#A_TRAITER A_TRAITER}</li>
	 *     <li>Notification du moteur de traitement de l'arrivée d'un nouvel événement pour l'organisation</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre entreprises
	 * @throws EvenementOrganisationEsbException en cas de problème <i>métier</i>
	 */
	private void onEvenementOrganisation(Source xml) throws EvenementOrganisationEsbException {
		final EvenementOrganisation event = decodeEvenementOrganisation(xml);
		if (event != null) {

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
	}

	private void fillCurrentDateOnSpecificIncomingEvent(EvenementOrganisation evt) throws EvenementOrganisationEsbException {
		// on ne s'intéresse qu'à des événements organisation qui n'auraient pas de date en entrée
		// (ou une date tellement hors des clous que cela ne fait pas de différence)
		if (evt.getDateEvenement() == null) {
			if (eventTypesWithNullEventDateReplacement.contains(evt.getType())) {
				evt.setDateEvenement(RegDate.get());
				LOGGER.info(String.format("La date de l'événement %d de type %s a été modifiée pour correspondre à la date de réception.", evt.getId(), evt.getType()));
			}
		}
	}

	private static void checkValidIncomingEventData(EvenementOrganisation evt) throws EvenementOrganisationEsbException {
		final List<String> attributs = new ArrayList<>();
		if (evt.getDateEvenement() == null) {
			attributs.add("date");
		}
		attributs.add("identifiant");
		if (evt.getType() == null) {
			attributs.add("type");
		}

		final int size = attributs.size();
		if (size > 0) {
			final String details = RECEPTION_EVT_ORGANISATION_RENDERER.toString(evt);
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

	private EvenementOrganisation decodeEvenementOrganisation(Source xml) throws EvenementOrganisationEsbException {

		try {
			// 1. décodage de l'événement reçu
			final NoticeRoot message = parse(xml);

			final EvenementOrganisation ech;
			try {
				ech = new EvenementOrganisation(message);
			}
			catch (IllegalArgumentException e) {
				throw new EvenementOrganisationEsbException(EsbBusinessCode.EVT_ORGANISATION, e);
			}

			// 2. rattrapage de la date nulle
			fillCurrentDateOnSpecificIncomingEvent(ech);

			// 3. validation des données de base
			checkValidIncomingEventData(ech);

			// 4. événement ignoré ?
			if (isIgnored(ech)) {
				onIgnoredEvent(ech);
				return null;
			}
			else {
				// 5. sauvegarde de l'événement dès son arrivée
				return saveIncomingEvent(ech);
			}
		}
		catch (JAXBException e) {
			final Throwable src = e.getLinkedException();
			throw new EvenementOrganisationEsbException(EsbBusinessCode.XML_INVALIDE, src != null ? src : e);
		}
		catch (SAXException | IOException e) {
			throw new EvenementOrganisationEsbException(EsbBusinessCode.XML_INVALIDE, e);
		}
	}

	protected void onIgnoredEvent(EvenementOrganisation evt) {
		Audit.info(evt.getId(), String.format("Evénement organisation ignoré (id=%d, type=%s)", evt.getId(), evt.getType()));
	}

	private boolean isIgnored(EvenementOrganisation event) {
		return ignoredEventTypes != null && ignoredEventTypes.contains(event.getType());
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
			final Source[] source = getClasspathSources("eVD-0021-1-0.xsd", "eVD-00022-1-0.xsd", "eVD-0023-1-0.xsd", "eVD-0024-1-0.xsd");
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

	/**
	 * Thread séparé pour la tentative de relance au démarrage des événements organisation "A_TRAITER" afin
	 * de laisser le temps à la web-app NEXUS de se mettre en place. // FIXME: Plus strictement nécessaire car on ne passe plus
	 * par NEXUS, les événements organisation comprennent l'identifiant.
	 */
	private final class RecuperationThread extends Thread {

		private volatile boolean stopping = false;

		private RecuperationThread() {
			super("EvtOrganisationRecup");
		}

		@Override
		public void run() {
			AuthenticationHelper.pushPrincipal("Récupération-démarrage");
			try {
				waitDelay();
				if (!stopping) {
					recuperateur.recupererEvenementsOrganisation();
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Erreur pendant l'attente préalable à la récupération des événements organisation à traiter", e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}

		private void waitDelay() throws InterruptedException {
			if (delaiRecuperationMinutes > 0) {
				LOGGER.info(String.format("On attend %d minute%s avant de démarrer la récupération des événements organisation à traiter",
				                          delaiRecuperationMinutes, delaiRecuperationMinutes > 1 ? "s" : StringUtils.EMPTY));
				final long recupDelayMillis = TimeUnit.MINUTES.toMillis(delaiRecuperationMinutes);
				final long start = System.currentTimeMillis();
				synchronized (this) {
					while (!stopping) {
						final long now = System.currentTimeMillis();
						if (now - start < recupDelayMillis) {
							wait(recupDelayMillis - (now - start));
						}
						else {
							// l'attente est finie !
							LOGGER.info("Attente terminée...");
							break;
						}
					}
					if (stopping) {
						LOGGER.info("Attente interrompue pour cause d'arrêt de l'application.");
					}
				}
			}
		}

		public void stopNow() {
			synchronized (this) {
				stopping = true;
				notifyAll();
			}
		}
	}

	@Override
	public void start() {
		// si on doit récupérer les anciens événements au démarrage, faisons-le maintenant
		if (recuperateur != null) {
			// FIXME: Plus nécessaire, cf. ci-dessus. Est-ce qu'on le garde pour le jour où on a besoin de chercher une info de
			// NEXUS à la reception des messages organisation?
			// msi (24.05.2012) : on le fait dans un thread séparé pour éviter le deadlock suivant : l'application web est
			// en cours de démarrage et a besoin des individus stockés dans nexus, mais nexus ne peut pas répondre
			// à la moindre requête tant que toutes les webapps ne sont pas démarrées.
			recuperationThread = new RecuperationThread();
			recuperationThread.start();
		}
		running = true;
	}

	@Override
	public void stop() {
		running = false;
		if (recuperationThread != null && recuperationThread.isAlive()) {
			recuperationThread.stopNow();
		}
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
			LOGGER.info(String.format("Liste des événements organisation dont une date nulle sera remplacée par la date de réception en mode %s : %s", processingMode, CollectionsUtils.toString(eventTypesWithNullEventDateReplacement, renderer, ", ", "Aucun")));
		}

		if (recuperateur != null && delaiRecuperationMinutes < 0) {
			throw new IllegalArgumentException("La propriété 'delaiRecuperationMinutes' devrait être positive ou nulle");
		}

		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}
}
