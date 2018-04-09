package ch.vd.unireg.evenement.organisation.engine;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationService;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementOrganisationProcessorFacade;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementOrganisationProcessorInternal;
import ch.vd.unireg.evenement.organisation.engine.translator.EvenementOrganisationTranslator;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;

public abstract class AbstractEvenementOrganisationProcessorTest extends BusinessTest {
	
	private EvenementOrganisationNotificationQueueImpl queue;
	protected EvenementOrganisationProcessorFacade processor;
	protected EvenementOrganisationDAO evtOrganisationDAO;
	protected EvenementOrganisationService evtOrganisationService;
	protected RegimeFiscalService regimeFiscalService;
	protected DataEventService dataEventService;
	protected ModificationInterceptor mainInterceptor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtOrganisationDAO  = getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO");
		evtOrganisationService  = getBean(EvenementOrganisationService.class, "evtOrganisationService");
		regimeFiscalService  = getBean(RegimeFiscalService.class, "regimeFiscalService");
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
		processor.start();
	}

	protected EvenementOrganisationProcessorFacade createProcessor(EvenementOrganisationTranslator translator) throws Exception {
		final EvenementOrganisationProcessorFacade facade = new EvenementOrganisationProcessorFacade();
		facade.setNotificationQueue(queue);

		final EvenementOrganisationProcessorInternal internal = new EvenementOrganisationProcessorInternal();
		internal.setEvtOrganisationDAO(evtOrganisationDAO);
		internal.setTransactionManager(transactionManager);
		internal.setTranslator(translator);
		internal.setTiersService(tiersService);
		internal.setIndexer(globalTiersIndexer);
		internal.setDataEventService(dataEventService);
		internal.afterPropertiesSet();

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

		// après l'exécution d'un ou plusieurs événements organisation en test, il est préférable d'attendre que
		// toutes les indexations on-the-fly soient terminées (car le traitement a lieu sur un thread séparé
		// dans lequel ce type d'indexation est activé par défaut)
		globalTiersIndexer.sync();
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
