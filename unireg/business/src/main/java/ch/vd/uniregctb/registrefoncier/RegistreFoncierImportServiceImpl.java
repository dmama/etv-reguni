package ch.vd.uniregctb.registrefoncier;

import java.util.HashMap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.TraiterImportRFJob;
import ch.vd.uniregctb.registrefoncier.dataimport.TraiterMutationsRFJob;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class RegistreFoncierImportServiceImpl implements RegistreFoncierImportService {

	private PlatformTransactionManager transactionManager;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private BatchScheduler batchScheduler;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	public void setEvenementRFMutationDAO(EvenementRFMutationDAO evenementRFMutationDAO) {
		this.evenementRFMutationDAO = evenementRFMutationDAO;
	}

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	@Override
	public int deleteAllMutations(long importId, @Nullable StatusManager statusManager) {

		final MutableInt deleted = new MutableInt(0);

		final MutableBoolean loop = new MutableBoolean(true);
		// on efface les mutations par lot de 1'000 pour éviter de saturer le rollback log de la DB
		while (loop.booleanValue()) {
			if (statusManager != null) {
				statusManager.setMessage("Effacement des mutations de l'import n°" + importId + "... (" + deleted.intValue() + " processées)");
			}
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);
			template.execute(status -> {
				final int count = evenementRFMutationDAO.deleteMutationsFor(importId, 1000);
				loop.setValue(count > 0);   // on boucle tant qu'il y a des mutations à supprimer
				deleted.add(count);
				return null;
			});
		}

		return deleted.intValue();
	}

	@Override
	public void startImport(long importId) throws JobAlreadyStartedException, SchedulerException {

		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, importId);
		params.put(TraiterImportRFJob.NB_THREADS, 8);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, true);
		batchScheduler.startJob(TraiterImportRFJob.NAME, params);
	}

	@Override
	public void forceImport(long importId) {

		final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
		if (importEvent == null) {
			throw new ObjectNotFoundException("L'import RF avec l'identifiant " + importId + " est inconnu");
		}

		if (importEvent.getEtat() != EtatEvenementRF.EN_ERREUR) {
			throw new IllegalArgumentException("L'import à forcer n'est pas en erreur.");
		}

		// on force le job et les mutations
		importEvent.setEtat(EtatEvenementRF.FORCE);
		evenementRFMutationDAO.forceMutations(importId);
	}

	@Override
	public void startMutations(long importId) throws JobAlreadyStartedException, SchedulerException {

		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 8);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, true);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, true);
		batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
	}

	@Override
	public void forceMutation(long mutId) {
		evenementRFMutationDAO.forceMutation(mutId);
	}

	@Override
	public void forceAllMutations(long importId) {
		// on force les mutations
		evenementRFMutationDAO.forceMutations(importId);
	}
}
