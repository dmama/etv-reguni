package ch.vd.unireg.evenement.civil.ech;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
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
import org.xml.sax.SAXException;

import ch.vd.evd0001.v5.EventIdentification;
import ch.vd.evd0001.v5.EventNotification;
import ch.vd.evd0001.v5.ObjectFactory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

/**
 * Listener des événements civils au format e-CH envoyés par RCPers au travers de l'ESB
 */
public class EvenementCivilEchEsbHandler implements EsbMessageHandler, InitializingBean, SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchEsbHandler.class);

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private EvenementCivilEchReceptionHandler receptionHandler;
	private EvenementCivilEchRecuperateur recuperateur;
	private int delaiRecuperationMinutes = 0;
	private RecuperationThread recuperationThread;
	private Set<TypeEvenementCivilEch> ignoredEventTypes;
	private Set<TypeEvenementCivilEch> eventTypesWithNullEventDateReplacement;
	private EvenementCivilEchProcessingMode processingMode;
	private boolean running;

	/**
	 * Renderer d'un événement civil à la réception (on ne renvoie que les données fournies à la réception de l'événement)
	 */
	private static final StringRenderer<EvenementCivilEch> RECEPTION_EVT_CIVIL_RENDERER =
			evt -> String.format("id=%d, refId=%d, type=%s, action=%s, date=%s", evt.getId(), evt.getRefMessageId(), evt.getType(), evt.getAction(), evt.getDateEvenement());

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReceptionHandler(EvenementCivilEchReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIgnoredEventTypes(Set<TypeEvenementCivilEch> ignoredEventTypes) {
		this.ignoredEventTypes = ignoredEventTypes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEventTypesWithNullEventDateReplacement(Set<TypeEvenementCivilEch> eventTypesWithNullEventDateReplacement) {
		this.eventTypesWithNullEventDateReplacement = eventTypesWithNullEventDateReplacement;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRecuperateur(EvenementCivilEchRecuperateur recuperateur) {
		this.recuperateur = recuperateur;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDelaiRecuperationMinutes(int delaiRecuperationMinutes) {
		this.delaiRecuperationMinutes = delaiRecuperationMinutes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessingMode(EvenementCivilEchProcessingMode processingMode) {
		this.processingMode = processingMode;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws EsbBusinessException {

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
		catch (EvenementCivilEchEsbException e) {
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
				LOGGER.info(String.format("Réception du message JMS événement civil e-CH {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	/**
	 * Déroulement des opérations :
	 * <ol>
	 *     <li>Décodage de l'XML reçu</li>
	 *     <li>Les événements de types ignorés provoquent un log et c'est tout...</li>
	 *     <li>Sauvegarde de l'événement correspondant (dans une transaction séparée) dans l'état {@link EtatEvenementCivil#A_TRAITER A_TRAITER}</li>
	 *     <li>Récupération de l'individu qui se cache derrière l'événement (appel au service "getEvent" de RCPers)</li>
	 *     <li>Sauvegarde du numéro d'individu dans l'événement</li>
	 *     <li>Notification du moteur de traitement de l'arrivée d'un nouvel événement pour l'individu</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre civil
	 * @throws EvenementCivilEchEsbException en cas de problème <i>métier</i>
	 */
	private void onEvenementCivil(Source xml) throws EvenementCivilEchEsbException {
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

	private void fillCurrentDateOnSpecificIncomingEvent(EvenementCivilEch ech) throws EvenementCivilEchEsbException {
		// on ne s'intéresse qu'à des événements civils qui n'auraient pas de date en entrée
		// (ou une date tellement hors des clous que cela ne fait pas de différence)
		if (ech.getDateEvenement() == null) {
			if (eventTypesWithNullEventDateReplacement.contains(ech.getType())) {
				ech.setDateEvenement(RegDate.get());
				LOGGER.info(String.format("La date de l'événement %d de type %s a été modifiée pour correspondre à la date de réception.", ech.getId(), ech.getType()));
			}
		}
	}

	private static void checkValidIncomingEventData(EvenementCivilEch ech) throws EvenementCivilEchEsbException {
		final List<String> attributs = new ArrayList<>();
		if (ech.getAction() == null) {
			attributs.add("action");
		}
		if (ech.getDateEvenement() == null) {
			attributs.add("date");
		}
		if (ech.getId() == null) {
			attributs.add("identifiant");
		}
		if (ech.getType() == null) {
			attributs.add("type");
		}

		final int size = attributs.size();
		if (size > 0) {
			final String details = RECEPTION_EVT_CIVIL_RENDERER.toString(ech);
			final String msg;
			if (size == 1) {
				msg = String.format("L'attribut '%s' est obligatoire pour un événement civil à l'entrée dans Unireg (%s).", attributs.get(0), details);
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
				b.append(" sont obligatoires pour un événement civil à l'entrée dans Unireg (");
				b.append(details);
				b.append(").");
				msg = b.toString();
			}
			throw new EvenementCivilEchEsbException(EsbBusinessCode.EVT_CIVIL, msg);
		}
	}

	private EvenementCivilEch decodeEvenementCivil(Source xml) throws EvenementCivilEchEsbException {

		try {
			// 1. décodage de l'événement reçu
			final EventNotification message = parse(xml);
			final EventIdentification evt = message.getIdentification();

			final EvenementCivilEch ech;
			try {
				ech = new EvenementCivilEch(evt);
			}
			catch (IllegalArgumentException e) {
				throw new EvenementCivilEchEsbException(EsbBusinessCode.EVT_CIVIL, e);
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
			throw new EvenementCivilEchEsbException(EsbBusinessCode.XML_INVALIDE, src != null ? src : e);
		}
		catch (SAXException | IOException e) {
			throw new EvenementCivilEchEsbException(EsbBusinessCode.XML_INVALIDE, e);
		}
	}

	protected void onIgnoredEvent(EvenementCivilEch evt) {
		Audit.info(evt.getId(), String.format("Evénement civil ignoré (id=%d, type=%s/%s)", evt.getId(), evt.getType(), evt.getAction()));
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
		final Unmarshaller u = jaxbContext.createUnmarshaller();
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
			final Source[] source = XmlUtils.toSourcesArray("eCH-0006-2-0.xsd",
			                                                "eCH-0007-4-0.xsd",
			                                                "eCH-0008-2-0.xsd",
			                                                "eCH-0010-4-0.xsd",
			                                                "eCH-0044-2-0.xsd",
			                                                "eCH-0011-5-0.xsd",
			                                                "eCH-0021-4-0.xsd",
			                                                "eCH-0058-2-0.xsd",
			                                                "eCH-0090-1-0.xsd",
			                                                "eVD-0009-1-0.xsd",
			                                                "eVD-0004-3-0.xsd",
			                                                "eVD-0001-5-0.xsd");
			schemaCache = sf.newSchema(source);
		}
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
	private final class RecuperationThread extends Thread {

		private volatile boolean stopping = false;

		private RecuperationThread() {
			super("EvtCivilEchRecup");
		}

		@Override
		public void run() {
			AuthenticationHelper.pushPrincipal("Récupération-démarrage");
			try {
				waitDelay();
				if (!stopping) {
					recuperateur.recupererEvenementsCivil();
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Erreur pendant l'attente préalable à la récupération des événements civils à traiter", e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}

		private void waitDelay() throws InterruptedException {
			if (delaiRecuperationMinutes > 0) {
				LOGGER.info(String.format("On attend %d minute%s avant de démarrer la récupération des événements civils à traiter",
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
			final StringRenderer<TypeEvenementCivilEch> renderer = type -> String.format("%s (%d)", type, type.getCodeECH());
			LOGGER.info(String.format("Liste des événements civils ignorés en mode %s : %s", processingMode, CollectionsUtils.toString(ignoredEventTypes, renderer, ", ", "Aucun")));
			LOGGER.info(String.format("Liste des événements civils dont une date nulle sera remplacée par la date de réception en mode %s : %s", processingMode, CollectionsUtils.toString(eventTypesWithNullEventDateReplacement, renderer, ", ", "Aucun")));
		}

		if (recuperateur != null && delaiRecuperationMinutes < 0) {
			throw new IllegalArgumentException("La propriété 'delaiRecuperationMinutes' devrait être positive ou nulle");
		}

		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}
}
