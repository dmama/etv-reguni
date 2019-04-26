package ch.vd.unireg.evenement.entreprise.engine;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseProcessingMode;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessor;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessorFacade;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessorInternal;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslator;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public abstract class AbstractEvenementEntrepriseCivileProcessorTest extends BusinessTest {
	
	private EvenementEntrepriseNotificationQueueImpl queue;
	protected EvenementEntrepriseProcessorFacade processor;
	protected EvenementEntrepriseDAO evtEntrepriseDAO;
	protected EvenementEntrepriseService evtEntrepriseService;
	protected RegimeFiscalService regimeFiscalService;
	protected DataEventService dataEventService;
	protected ModificationInterceptor mainInterceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evtEntrepriseDAO = getBean(EvenementEntrepriseDAO.class, "evenementEntrepriseDAO");
		evtEntrepriseService = getBean(EvenementEntrepriseService.class, "evtEntrepriseService");
		regimeFiscalService  = getBean(RegimeFiscalService.class, "regimeFiscalService");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		mainInterceptor = getBean(ModificationInterceptor.class, "modificationInterceptor");

		final EvenementEntrepriseTranslator translator = getBean(EvenementEntrepriseTranslator.class, "evenementEntrepriseTranslator");

		queue = new EvenementEntrepriseNotificationQueueImpl(0);
		queue.setTransactionManager(transactionManager);
		queue.setEvtEntrepriseService(evtEntrepriseService);
		if (buildProcessorOnSetup()) {
			buildProcessor(translator);
			((EvenementEntrepriseProcessorInternal)processor.getInternalProcessor()).afterPropertiesSet();
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
	
	protected void rebuildProcessor(EvenementEntrepriseTranslator translator) throws Exception {
		if (processor != null) {
			processor.stop();
		}
		buildProcessor(translator);
	}

	protected void buildProcessor(EvenementEntrepriseTranslator translator) throws Exception {
		processor = createProcessor(translator);
		processor.start();
	}

	protected EvenementEntrepriseProcessorFacade createProcessor(EvenementEntrepriseTranslator translator) throws Exception {
		final EvenementEntrepriseProcessorFacade facade = new EvenementEntrepriseProcessorFacade();
		facade.setAudit(audit);
		facade.setNotificationQueue(queue);

		final EvenementEntrepriseProcessorInternal internal = new EvenementEntrepriseProcessorInternal();
		internal.setEvtEntrepriseDAO(evtEntrepriseDAO);
		internal.setTransactionManager(transactionManager);
		internal.setTranslator(translator);
		internal.setTiersService(tiersService);
		internal.setIndexer(globalTiersIndexer);
		internal.setDataEventService(dataEventService);
		internal.setAudit(audit);
		internal.afterPropertiesSet();

		facade.setInternalProcessor(internal);
		return facade;
	}

	protected void traiterEvenements(final long noEntreprise) throws InterruptedException {
		// on se met en position pour voir quand le traitement aura été effectué
		final MutableBoolean jobDone = new MutableBoolean(false);
		final EvenementEntrepriseProcessor.ListenerHandle handle = processor.registerListener(new EvenementEntrepriseProcessor.Listener() {
			@Override
			public void onEntrepriseTraitee(long noEntrepriseCivile) {
				if (noEntrepriseCivile == noEntreprise) {
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
			// notification d'arrivée d'événement sur l'entreprise
			queue.post(noEntreprise, EvenementEntrepriseProcessingMode.BULK);

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

		// après l'exécution d'un ou plusieurs événements entreprise en test, il est préférable d'attendre que
		// toutes les indexations on-the-fly soient terminées (car le traitement a lieu sur un thread séparé
		// dans lequel ce type d'indexation est activé par défaut)
		globalTiersIndexer.sync();
	}

	@NotNull
	protected static EvenementEntreprise createEvent(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		final EvenementEntreprise event = new EvenementEntreprise();
		event.setNoEvenement(noEvenement);
		event.setNoEntrepriseCivile(noEntrepriseCivile);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}

	/**
	 * Récupérer l'unique événement lié au numéro d'événement RCEnt. Une exception est retournée
	 * s'il devait y en avoir plusieurs.
	 *
	 * Note: on trouve plusieurs événement en base pour un seul événement RCEnt si l'événement portait sur plusieurs entreprises.
	 * @param noEvenement Le numéro d'événement RCEnt
	 * @return L'unique événement en base
	 */
	public EvenementEntreprise getUniqueEvent(long noEvenement) {
		List<EvenementEntreprise> result = evtEntrepriseDAO.getEvenementsForNoEvenement(noEvenement);
		if (result.size() > 1) {
			throw new IllegalStateException("Plusieurs événements entreprises trouvés en base alors que par cet appel on s'attend à n'en trouver qu'un seul.");
		}
		return result.isEmpty() ? null : result.get(0);
	}
}
