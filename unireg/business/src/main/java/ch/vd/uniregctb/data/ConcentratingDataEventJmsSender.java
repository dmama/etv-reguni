package ch.vd.uniregctb.data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.FiscalEventSendRequestEvent;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalSender;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ConcentratingDataEventJmsSender implements ModificationSubInterceptor, InitializingBean, DisposableBean, EvenementFiscalSender, DataEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConcentratingDataEventJmsSender.class);

	/**
	 * Les événements fiscaux collectés dans la transaction en cours
	 */
	private final ThreadLocal<List<EvenementFiscal>> evenementsFiscauxCollectes = ThreadLocal.withInitial(LinkedList::new);

	/**
	 * Les événements "data" collectés dans la transaction en cours
	 */
	private final DataEventByThreadCollector dataEventsCollectes = new DataEventByThreadCollector();

	private DataEventSender sender;
	private ModificationInterceptor parentInterceptor;
	private DataEventService parentService;
	private boolean evenementsFiscauxActives;

	public void setSender(DataEventSender sender) {
		this.sender = sender;
	}

	public void setParentInterceptor(ModificationInterceptor parentInterceptor) {
		this.parentInterceptor = parentInterceptor;
	}

	public void setParentService(DataEventService parentService) {
		this.parentService = parentService;
	}

	public void setEvenementsFiscauxActives(boolean evenementsFiscauxActives) {
		this.evenementsFiscauxActives = evenementsFiscauxActives;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parentService.register(this);
		parentInterceptor.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parentInterceptor.unregister(this);
		parentService.unregister(this);
	}

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
		// on ne fera quelque chose qu'à la fin de la transaction
		return false;
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici, on attend la fin de la transaction
	}

	@Override
	public void preTransactionCommit() {
		final List<DataEvent> collectedEvents = dataEventsCollectes.getCollectedEvents();
		final List<EvenementFiscal> evtsFiscaux = evenementsFiscauxCollectes.get();
		final List<DataEvent> events;
		if (!evtsFiscaux.isEmpty()) {
			final DataEvent evtFiscalDataEvent = new FiscalEventSendRequestEvent(evtsFiscaux.stream()
					                                                                     .map(EvenementFiscal::getId)
					                                                                     .collect(Collectors.toList()));
			events = Stream.concat(collectedEvents.stream(), Stream.of(evtFiscalDataEvent))
					.collect(Collectors.toList());
		}
		else {
			events = collectedEvents;
		}

		if (!events.isEmpty()) {
			try {
				sender.sendDataEvent(events);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer les 'dataEvents' : [" + CollectionsUtils.toString(events, StringRenderer.DEFAULT, ", ") + "]", e);
			}
		}
	}

	@Override
	public void postTransactionCommit() {
		cleanup();
	}

	@Override
	public void postTransactionRollback() {
		cleanup();
	}

	private void cleanup() {
		evenementsFiscauxCollectes.remove();
		dataEventsCollectes.cleanup();
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		if (!evenementsFiscauxActives) {
			LOGGER.info(String.format("Evénements fiscaux désactivés : l'événement fiscal %d n'est pas envoyé.", evenement.getId()));
			return;
		}

		evenementsFiscauxCollectes.get().add(evenement);
	}

	@Override
	public void onOrganisationChange(long id) {
		dataEventsCollectes.onOrganisationChange(id);
	}

	@Override
	public void onTiersChange(long id) {
		dataEventsCollectes.onTiersChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		dataEventsCollectes.onIndividuChange(id);
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		dataEventsCollectes.onDroitAccessChange(tiersId);
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		dataEventsCollectes.onRelationshipChange(type, sujetId, objetId);
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		dataEventsCollectes.onImmeubleChange(immeubleId);
	}

	@Override
	public void onBatimentChange(long batimentId) {
		dataEventsCollectes.onBatimentChange(batimentId);
	}

	@Override
	public void onTruncateDatabase() {
		dataEventsCollectes.onTruncateDatabase();
	}

	@Override
	public void onLoadDatabase() {
		dataEventsCollectes.onLoadDatabase();
	}
}
