package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationService;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessorFacade;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessorInternal;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslator;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public abstract class AbstractEvenementOrganisationProcessorTest extends BusinessTest {
	
	private EvenementOrganisationNotificationQueueImpl queue;
	protected EvenementOrganisationProcessorFacade processor;
	protected EvenementOrganisationDAO evtOrganisationDAO;
	protected EvenementOrganisationService evtOrganisationService;
	protected DataEventService dataEventService;
	protected ModificationInterceptor mainInterceptor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtOrganisationDAO  = getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO");
		evtOrganisationService  = getBean(EvenementOrganisationService.class, "evtOrganisationService");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		mainInterceptor = getBean(ModificationInterceptor.class, "modificationInterceptor");

		final EvenementOrganisationTranslator translator = getBean(EvenementOrganisationTranslator.class, "evenementOrganisationTranslator");

		queue = new EvenementOrganisationNotificationQueueImpl(0);
		queue.setTransactionManager(transactionManager);
		queue.setEvtOrganisationService(evtOrganisationService);
		if (buildProcessorOnSetup()) {
			buildProcessor(translator);
			((EvenementOrganisationProcessorInternal)processor.getInternalProcessor()).afterPropertiesSet();
			processor.start();
		}
		queue.afterPropertiesSet();
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Override
	public void onTearDown() throws Exception {
		if (processor != null) {
			processor.stop();
			processor = null;
		}
		queue.destroy();
		super.onTearDown();
	}
	
	protected void rebuildProcessor(EvenementOrganisationTranslator translator) throws Exception {
		if (processor != null) {
			processor.stop();
		}
		buildProcessor(translator);
	}

	protected void buildProcessor(EvenementOrganisationTranslator translator) throws Exception {
		processor = createProcessor(translator);
		((EvenementOrganisationProcessorInternal) processor.getInternalProcessor()).afterPropertiesSet();
		processor.start();
	}

	protected EvenementOrganisationProcessorFacade createProcessor(EvenementOrganisationTranslator translator) {
		final EvenementOrganisationProcessorFacade facade = new EvenementOrganisationProcessorFacade();
		facade.setNotificationQueue(queue);

		final EvenementOrganisationProcessorInternal internal = new EvenementOrganisationProcessorInternal();
		internal.setEvtOrganisationDAO(evtOrganisationDAO);
		internal.setTransactionManager(transactionManager);
		internal.setTranslator(translator);
		internal.setTiersService(tiersService);
		internal.setIndexer(globalTiersIndexer);
		internal.setDataEventService(dataEventService);

		facade.setInternalProcessor(internal);
		return facade;
	}

	protected void traiterEvenements(final long noOrganisation) throws InterruptedException {
		// on se met en position pour voir quand le traitement aura été effectué
		final MutableBoolean jobDone = new MutableBoolean(false);
		final EvenementOrganisationProcessor.ListenerHandle handle = processor.registerListener(new EvenementOrganisationProcessor.Listener() {
			@Override
			public void onOrganisationTraite(long noOrganisationTraite) {
				if (noOrganisationTraite == noOrganisation) {
					setAndNotify();
				}
			}

			@Override
			public void onStop() {
				setAndNotify();
			}

			private void setAndNotify() {
				synchronized (jobDone) {
					jobDone.setValue(true);
					jobDone.notifyAll();
				}
			}
		});

		try {
			// notification d'arrivée d'événement sur l'organisation
			queue.post(noOrganisation, EvenementOrganisationProcessingMode.BULK);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (jobDone) {
				// on attend que le traitement se fasse
				while (!jobDone.booleanValue()) {
					jobDone.wait();
				}
			}
		}
		finally {
			handle.unregister();
		}
	}

	@NotNull
	protected static EvenementOrganisation createEvent(Long noEvenement, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setNoEvenement(noEvenement);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}

	/**
	 * Récupérer l'unique événement lié au numéro d'événement RCEnt. Une exception est retournée
	 * s'il devait y en avoir plusieurs.
	 *
	 * Note: on trouve plusieurs événement en base pour un seul événement RCEnt si l'événement portait sur plusieurs organisations.
	 * @param noEvenement Le numéro d'événement RCEnt
	 * @return L'unique événement en base
	 */
	public EvenementOrganisation getUniqueEvent(long noEvenement) {
		List<EvenementOrganisation> result = evtOrganisationDAO.getEvenementsForNoEvenement(noEvenement);
		if (result.size() > 1) {
			throw new IllegalStateException("Plusieurs événements organisations trouvés en base alors que par cet appel on s'attend à n'en trouver qu'un seul.");
		}
		return result.isEmpty() ? null : result.get(0);
	}

}
