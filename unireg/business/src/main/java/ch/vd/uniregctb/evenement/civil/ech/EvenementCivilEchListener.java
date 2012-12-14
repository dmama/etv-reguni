package ch.vd.uniregctb.evenement.civil.ech;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0006.v1.EventIdentification;
import ch.vd.evd0006.v1.EventNotification;
import ch.vd.evd0006.v1.ObjectFactory;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import static ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchReceptionHandler.Mode;

/**
 * Listener des événements civils au format e-CH envoyés par RCPers au travers de l'ESB
 */
public class EvenementCivilEchListener extends EsbMessageEndpointListener implements ErrorMonitorableMessageListener, InitializingBean, SmartLifecycle {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchListener.class);

	private final AtomicInteger nombreMessagesRecus = new AtomicInteger(0);
	private final AtomicInteger nombreMessagesErreurs = new AtomicInteger(0);
	private final AtomicInteger nombreMessagesExceptions = new AtomicInteger(0);

	private Schema schemaCache;

	private EvenementCivilEchReceptionHandler receptionHandler;
	private EvenementCivilEchRethrower rethrower;
	private int rethrowDelayMinutes = 0;
	private RethrowerThread rethrowerThread;
	private Set<TypeEvenementCivilEch> ignoredEventTypes;
	private Mode processingMode;
	private boolean running;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReceptionHandler(EvenementCivilEchReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIgnoredEventTypes(Set<TypeEvenementCivilEch> ignoredEventTypes) {
		this.ignoredEventTypes = ignoredEventTypes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRethrower(EvenementCivilEchRethrower rethrower) {
		this.rethrower = rethrower;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRethrowDelayMinutes(int rethrowDelayMinutes) {
		this.rethrowDelayMinutes = rethrowDelayMinutes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessingMode(Mode processingMode) {
		this.processingMode = processingMode;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nombreMessagesRecus.incrementAndGet();

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS événement civil e-CH {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			final Source content = message.getBodyAsSource();
			final String visaMutation = EvenementCivilEchSourceHelper.getVisaCreation(message);
			AuthenticationHelper.pushPrincipal(visaMutation);
			try {
				onEvenementCivil(content);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
		catch (EvenementCivilException e) {
			// on a un truc qui a sauté au moment de l'arrivée de l'événement (test métier)
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			nombreMessagesErreurs.incrementAndGet();
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.UNKNOWN, "");
		}
		catch (RuntimeException e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			nombreMessagesExceptions.incrementAndGet();
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Réception du message JMS événement civil e-CH {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	/**
	 * Déroulement des opérations :
	 * <ol>
	 *     <li>Décodage de l'XML reçu</li>
	 *     <li>Les événements de types ignorés provoquent un log et c'est tout...</li>
	 *     <li>Sauvegarde de l'événement correspondant (dans une transaction séparée) dans l'état {@link ch.vd.uniregctb.type.EtatEvenementCivil#A_TRAITER A_TRAITER}</li>
	 *     <li>Récupération de l'individu qui se cache derrière l'événement (appel au service "getEvent" de RCPers)</li>
	 *     <li>Sauvegarde du numéro d'individu dans l'événement</li>
	 *     <li>Notification du moteur de traitement de l'arrivée d'un nouvel événement pour l'individu</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre civil
	 * @throws EvenementCivilException en cas de problème <i>métier</i>
	 */
	private void onEvenementCivil(Source xml) throws EvenementCivilException {
		final EvenementCivilEch event = decodeEvenementCivil(xml);
		if (event != null) {

			// à partir d'ici, l'événement est sauvegardé en base... Il n'est donc plus question
			// de rejetter en erreur (ou exception) le message entrant...

			try {
				receptionHandler.handleEvent(event, processingMode);
			}
			catch (Exception e) {
				// le traitement sera re-tenté au plus tard au prochain démarrage de l'application...
				LOGGER.error(String.format("Erreur à la réception de l'événement civil %d", event.getId()), e);
			}
		}
	}

	private EvenementCivilEch decodeEvenementCivil(Source xml) throws EvenementCivilException {

		try {
			// 1. décodage de l'événement reçu
			final EventNotification message = parse(xml);
			final EventIdentification evt = message.getIdentification();

			final EvenementCivilEch ech;
			try {
				ech = new EvenementCivilEch(evt);
			}
			catch (IllegalArgumentException e) {
				throw new EvenementCivilException(e);
			}

			// 2. événement ignoré ?
			if (isIgnored(ech)) {
				Audit.info(evt.getMessageId(), String.format("Evénement civil ignoré (id=%d, type=%s/%s)", evt.getMessageId(), evt.getType(), evt.getAction()));
				return null;
			}
			else {
				// 3. sauvegarde de l'événement dès son arrivée
				return saveIncomingEvent(ech);
			}
		}
		catch (JAXBException e) {
			final Throwable src = e.getLinkedException();
			throw new EvenementCivilException(src != null ? src : e);
		}
		catch (SAXException e) {
			throw new EvenementCivilException(e);
		}
		catch (IOException e) {
			throw new EvenementCivilException(e);
		}
	}

	private boolean isIgnored(EvenementCivilEch event) {
		return ignoredEventTypes != null && ignoredEventTypes.contains(event.getType());
	}

	private EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
		final Long id = event.getId();
		Audit.info(id, String.format("Arrivée de l'événement civil %d (%s/%s au %s)", id, event.getType(), event.getAction(), RegDateHelper.dateToDisplayString(event.getDateEvenement())));
		return receptionHandler.saveIncomingEvent(event);
	}

	private EventNotification parse(Source xml) throws JAXBException, SAXException, IOException {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (EventNotification) u.unmarshal(xml);
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
			final Source[] source = getClasspathSources("eVD-0009-1-0.xsd", "eVD-0004-2-0.xsd", "eVD-0001-3-0.xsd", "eVD-0006-1-0.xsd");
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
	public int getNombreMessagesRenvoyesEnErreur() {
		return nombreMessagesErreurs.get();
	}

	@Override
	public int getNombreMessagesRenvoyesEnException() {
		return nombreMessagesExceptions.get();
	}

	@Override
	public int getNombreMessagesRecus() {
		return nombreMessagesRecus.get();
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
	 * Thread séparé pour la tentative de relance au démarrage des événements civils e-CH "A_TRAITER" afin
	 * de laisser le temps à la web-app NEXUS de se mettre en place (la relance de ces événements fait essentiellement
	 * des appels à RCPers pour récupérer des données sur les individus / les événements, et tout cela passe par NEXUS)
	 */
	private final class RethrowerThread extends Thread {

		private volatile boolean stopping = false;

		private RethrowerThread() {
			super("EvtCivilEchRethrower");
		}

		@Override
		public void run() {
			AuthenticationHelper.pushPrincipal("Relance-démarrage");
			try {
				waitDelay();
				if (!stopping) {
					rethrower.fetchAndRethrowEvents();
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Erreur pendant l'attente préalable à la relance des événements civils à traiter", e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}

		private void waitDelay() throws InterruptedException {
			if (rethrowDelayMinutes > 0) {
				LOGGER.info(String.format("On attend %d minute%s avant de démarrer la relance des événements civils à traiter", rethrowDelayMinutes, rethrowDelayMinutes > 1 ? "s" : StringUtils.EMPTY));
				final long rethrowDelayMillis = TimeUnit.MINUTES.toMillis(rethrowDelayMinutes);
				final long start = System.currentTimeMillis();
				synchronized (this) {
					while (!stopping) {
						final long now = System.currentTimeMillis();
						if (now - start < rethrowDelayMillis) {
							wait(rethrowDelayMillis - (now - start));
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
		if (rethrower != null) {
			// msi (24.05.2012) : on le fait dans un thread séparé pour éviter le deadlock suivant : l'application web est
			// en cours de démarrage et a besoin des individus stockés dans nexus, mais nexus ne peut pas répondre
			// à la moindre requête tant que toutes les webapps ne sont pas démarrées.
			rethrowerThread = new RethrowerThread();
			rethrowerThread.start();
		}
		running = true;
	}

	@Override
	public void stop() {
		running = false;
		if (rethrowerThread != null && rethrowerThread.isAlive()) {
			rethrowerThread.stopNow();
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
			final StringBuilder b = new StringBuilder("Liste des événements civils ignorés : ");
			if (ignoredEventTypes == null) {
				b.append("Aucun");
			}
			else {
				boolean first = true;
				for (TypeEvenementCivilEch type : ignoredEventTypes) {
					if (!first) {
						b.append(", ");
					}
					b.append(type).append(" (").append(type.getCodeECH()).append(')');
					first = false;
				}
				LOGGER.info(b.toString());
			}
		}

		if (rethrower != null && rethrowDelayMinutes < 0) {
			throw new IllegalArgumentException("La propriété 'rethrowDelayMinutes' devrait être positive ou nulle");
		}
	}
}
