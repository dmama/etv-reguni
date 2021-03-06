package ch.vd.unireg.registrefoncier;

import java.util.HashMap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.BatimentRFDAO;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.TraiterImportRFJob;
import ch.vd.unireg.registrefoncier.dataimport.TraiterMutationsRFJob;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;
import ch.vd.unireg.registrefoncier.key.BatimentRFKey;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.scheduler.JobAlreadyStartedException;

public class RegistreFoncierImportServiceImpl implements RegistreFoncierImportService, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistreFoncierImportServiceImpl.class);

	private PlatformTransactionManager transactionManager;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private DroitRFDAO droitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private CommuneRFDAO communeRFDAO;
	private ApplicationContext applicationContext;

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

	public void setDroitRFDAO(DroitRFDAO droitRFDAO) {
		this.droitRFDAO = droitRFDAO;
	}

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setBatimentRFDAO(BatimentRFDAO batimentRFDAO) {
		this.batimentRFDAO = batimentRFDAO;
	}

	public void setAyantDroitRFDAO(AyantDroitRFDAO ayantDroitRFDAO) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
	}

	public void setCommuneRFDAO(CommuneRFDAO communeRFDAO) {
		this.communeRFDAO = communeRFDAO;
	}

	@Override
	public int deleteAllMutations(long importId, @Nullable StatusManager statusManager) {

		final MutableInt deleted = new MutableInt(0);

		final MutableBoolean loop = new MutableBoolean(true);
		// on efface les mutations par lot de 1'000 pour éviter de saturer le rollback log de la DB
		while (loop.booleanValue()) {
			if (statusManager != null) {
				statusManager.setMessage("Effacement des mutations de l'import n°" + importId + "... (" + deleted.intValue() + " processées)");
				if (statusManager.isInterrupted()) {
					break;
				}
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

		if (importEvent.getEtat() != EtatEvenementRF.A_TRAITER && importEvent.getEtat() != EtatEvenementRF.EN_ERREUR) {
			throw new IllegalArgumentException("L'import à forcer n'est pas en erreur ou à traiter.");
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
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, true);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IMPORT_SERVITUDES_JOB, true);
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

	@Override
	public HibernateEntity findEntityForMutation(@NotNull TypeEntiteRF type, @NotNull String idRF, @Nullable String versionIdRF) {

		switch (type) {
		case AYANT_DROIT:
			return ayantDroitRFDAO.find(new AyantDroitRFKey(idRF), null);
		case DROIT:
			// dans le cas d'un droit, l'idRf correspond à l'immeuble servant
			return immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
		case SERVITUDE:
			if (versionIdRF == null) {
				throw new IllegalArgumentException("Le versionId est obligsatoire pour les droits de propriété et les servitudes");
			}
			return droitRFDAO.find(new DroitRFKey(idRF, versionIdRF));
		case IMMEUBLE:
			return immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
		case SURFACE_AU_SOL:
			// dans le cas d'une surface au sol, l'idRf correspond à l'immeuble auxquelles elle est rattachée
			return immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
		case BATIMENT:
			return batimentRFDAO.find(new BatimentRFKey(idRF), null);
		case COMMUNE:
			return communeRFDAO.findActive(new CommuneRFKey(Integer.parseInt(idRF)));
		default:
			throw new IllegalArgumentException("Type d'entité RF inconnu = [" + type + "]");
		}
	}

	@Override
	@Nullable
	public Long findMatchingImportServitudesToProcess(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {
			final EvenementRFImport imp = evenementRFImportDAO.get(importId);
			if (imp == null) {
				throw new IllegalArgumentException("L'import avec l'id = [" + importId + "] n'existe pas.");
			}
			if (imp.getType() != TypeImportRF.PRINCIPAL) {
				// l'import spécifié n'est pas un import principal, rien à faire
				return null;
			}
			final EvenementRFImport impServitudes = evenementRFImportDAO.find(TypeImportRF.SERVITUDES, imp.getDateEvenement());
			if (impServitudes != null && (impServitudes.getEtat() == EtatEvenementRF.A_TRAITER || impServitudes.getEtat() == EtatEvenementRF.EN_ERREUR)) {
				// il y a bien un import des servitudes à traiter à la même date
				return impServitudes.getId();
			}
			else {
				return null;
			}
		});
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// au démarrage de l'application, on corrige l'état des imports interrompus si nécessaire
		if (event.getApplicationContext() == this.applicationContext) {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.execute(status -> {
				final int count = evenementRFImportDAO.fixAbnormalJVMTermination();
				if (count > 0) {
					LOGGER.warn("Corrigé l'état de " + count + " job(s) d'importation RF suite à l'arrêt anormal de la JVM.");
				}
				return null;
			});
		}
	}
}
