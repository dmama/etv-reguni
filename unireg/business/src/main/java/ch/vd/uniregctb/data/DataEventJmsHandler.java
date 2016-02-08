package ch.vd.uniregctb.data;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseLoadEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseTruncateEvent;
import ch.vd.unireg.xml.event.data.v1.DroitAccesChangeEvent;
import ch.vd.unireg.xml.event.data.v1.IndividuChangeEvent;
import ch.vd.unireg.xml.event.data.v1.ObjectFactory;
import ch.vd.unireg.xml.event.data.v1.PmChangeEvent;
import ch.vd.unireg.xml.event.data.v1.RelationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Bean qui traite les messages JMS de modification de la database pour propager l'information au database service
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataEventJmsHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataEventJmsHandler.class);

	private DataEventService dataEventService;

	private Schema schemaCache;

	private JAXBContext jaxbContext;

	private Map<Class<? extends DataEvent>, Handler> handlers;

	private static <T extends DataEvent> void addToMap(Map<Class<? extends DataEvent>, Handler> map, Class<T> clazz, Handler<T> handler) {
		map.put(clazz, handler);
	}

	private Map<Class<? extends DataEvent>, Handler> buildHandlers() {
		final Map<Class<? extends DataEvent>, Handler> map = new HashMap<>();
		addToMap(map, DatabaseLoadEvent.class, new DatabaseLoadEventHandler());
		addToMap(map, DatabaseTruncateEvent.class, new DatabaseTruncateEventHandler());
		addToMap(map, DatabaseTruncateEvent.class, new DatabaseTruncateEventHandler());
		addToMap(map, DroitAccesChangeEvent.class, new DroitAccesChangeEventHandler());
		addToMap(map, IndividuChangeEvent.class, new IndividuChangeEventHandler());
		addToMap(map, TiersChangeEvent.class, new TiersChangeEventHandler());
		addToMap(map, PmChangeEvent.class, new PmChangeEventHandler());
		addToMap(map, RelationChangeEvent.class, new RelationChangeEventHandler());
		return map;
	}

	private interface Handler<T extends DataEvent> {
		void onEvent(T event);
	}

	private final class DatabaseLoadEventHandler implements Handler<DatabaseLoadEvent> {
		@Override
		public void onEvent(DatabaseLoadEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement de chargement de la database");
			}
			dataEventService.onLoadDatabase();
		}
	}

	private final class DatabaseTruncateEventHandler implements Handler<DatabaseTruncateEvent> {
		@Override
		public void onEvent(DatabaseTruncateEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement de truncate de la database");
			}
			dataEventService.onTruncateDatabase();
		}
	}

	private final class DroitAccesChangeEventHandler implements Handler<DroitAccesChangeEvent> {
		@Override
		public void onEvent(DroitAccesChangeEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur les droits d'accès du tiers n°" + event.getId());
			}
			dataEventService.onDroitAccessChange(event.getId());
		}
	}

	private final class IndividuChangeEventHandler implements Handler<IndividuChangeEvent> {
		@Override
		public void onEvent(IndividuChangeEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur l'individu n°" + event.getId());
			}
			dataEventService.onIndividuChange(event.getId());
		}
	}

	private final class TiersChangeEventHandler implements Handler<TiersChangeEvent> {
		@Override
		public void onEvent(TiersChangeEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur le tiers n°" + event.getId());
			}
			dataEventService.onTiersChange(event.getId());
		}
	}

	private final class PmChangeEventHandler implements Handler<PmChangeEvent> {
		@Override
		public void onEvent(PmChangeEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur la PM n°" + event.getId());
			}
			dataEventService.onPersonneMoraleChange(event.getId());
		}
	}

	private final class RelationChangeEventHandler implements Handler<RelationChangeEvent> {
		@Override
		public void onEvent(RelationChangeEvent event) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur la relation de type " + event.getRelationType() + " entre les tiers sujet " + event.getSujetId() + " et objet " + event.getObjetId());
			}
			final TypeRapportEntreTiers type;
			switch (event.getRelationType()) {
				case ANNULE_ET_REMPLACE:
					type = TypeRapportEntreTiers.ANNULE_ET_REMPLACE;
					break;
				case APPARTENANCE_MENAGE:
					type = TypeRapportEntreTiers.APPARTENANCE_MENAGE;
					break;
				case CONSEIL_LEGAL:
					type = TypeRapportEntreTiers.CONSEIL_LEGAL;
					break;
				case CONTACT_IMPOT_SOURCE:
					type = TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE;
					break;
				case CURATELLE:
					type = TypeRapportEntreTiers.CURATELLE;
					break;
				case PARENTE:
					type = TypeRapportEntreTiers.PARENTE;
					break;
				case PRESTATION_IMPOSABLE:
					type = TypeRapportEntreTiers.PRESTATION_IMPOSABLE;
					break;
				case REPRESENTATION:
					type = TypeRapportEntreTiers.REPRESENTATION;
					break;
				case TUTELLE:
					type = TypeRapportEntreTiers.TUTELLE;
					break;
				case ASSUJETTISSEMENT_PAR_SUBSTITUTION:
					type = TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION;
					break;
				case ACTIVITE_ECONOMIQUE:
					type = TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE;
					break;
				case MANDAT:
					type = TypeRapportEntreTiers.MANDAT;
					break;
				case FUSION_ENTREPRISES:
					type = TypeRapportEntreTiers.FUSION_ENTREPRISES;
					break;
				case ADMINISTRATION_ENTREPRISE:
					type = TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE;
					break;
				case SOCIETE_DIRECTION:
					type = TypeRapportEntreTiers.SOCIETE_DIRECTION;
					break;
				default:
					throw new IllegalArgumentException("Type de relation inconnu : " + event.getRelationType());
			}
			dataEventService.onRelationshipChange(type, event.getSujetId(), event.getObjetId());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.handlers = buildHandlers();
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(msg.getBodyAsSource());

		final DataEvent evenement = element == null ? null : (DataEvent) element.getValue();
		if (evenement != null) {
			// traitement du message
			AuthenticationHelper.pushPrincipal("JMS-DbEvent(" + msg.getMessageId() + ')');
			try {
				final Handler handler = handlers.get(evenement.getClass());
				if (handler == null) {
					LOGGER.error("Pas de handler enregistré pour le message " + msg.getMessageId() + " de classe " + evenement.getClass().getSimpleName());
				}
				else {
					//noinspection unchecked
					handler.onEvent(evenement);
				}
			}
			catch (Exception e) {
				LOGGER.error("Erreur lors de la réception du message n°" + msg.getMessageId(), e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
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
			final ClassPathResource resource = new ClassPathResource("event/data/dataEvent-1.xsd");
			Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}
}
