package ch.vd.uniregctb.migreg;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeMigRegError;

public abstract class HostMigrator {

	private static final Logger LOGGER = Logger.getLogger(HostMigrator.class);

	protected final MigregStatusManager status;
	protected final HostMigratorHelper helper;
	protected MigRegLimits limits = null;
	private TransactionStatus txStatus;
	protected MigregErrorsManager errorsManager;

	protected  class NombreMigrees {
		public Integer valeur;

		public NombreMigrees() {
			valeur = 0;
		}
	}

	private int nbCommits;
	private final ArrayList<Tiers> tierss = new ArrayList<Tiers>();
	protected final NombreMigrees nbCtbMigrated = new NombreMigrees();

	public ArrayList<MigrationError> errors = new ArrayList<MigrationError>();


	public HostMigrator(HostMigratorHelper h, MigregStatusManager mgr) {
		this.helper = h;
		this.status = mgr;

	}
	public HostMigrator(HostMigratorHelper h, MigRegLimits limits, MigregStatusManager mgr) {
		this.helper = h;
		this.limits = limits;
		this.status = mgr;
	}

	public abstract int migrate() throws Exception;

	public void initialize() throws Exception {
	}
	public void terminate() throws Exception {
	}

	// A surcharger
	protected void doBeforeCommit() throws Exception {
	}

	protected void doAfterCommit() throws Exception {

		if (helper.asyncIndexer.isEnabled()) {
			ArrayList<Tiers> list = getTiersList();
			setRunningMessage("ASYNC Indexation de "+list.size()+" tiers...");
			for (Tiers tiers : list) {
				long id = tiers.getNumero();
				helper.asyncIndexer.queueTiersForIndexation(id);
			}
		}
	}

	protected Tiers saveTiers(Tiers tiers) {
		Assert.notNull(tiers);

		// DEBUG
		long numero = -1L;
		if (tiers.getNumero() != null) {
			numero = tiers.getNumero();
		}
		// DEBUG

//		if (tiers instanceof Habitant) {
//			Habitant hab = (Habitant) tiers;
//			for (Long noIndToBanned : lstIndToBanned) {
//				if (noIndToBanned.equals(hab.getNumeroIndividu())) {
//					String message = "Le tiers "+tiers.getNumero()+" ne peut pas être migré. Individu "+hab.getNumeroIndividu()+" est déjà présent.";
//					errorsManager.setInError(hab.getNumero(), message, null, null);
//					return null;
//				}
//			}
//		}
		int nb = status.addGlobalObjectsMigrated(1);
		if (nb % 100 == 0) {
			LOGGER.info("Nombre de tiers sauvés: "+nb);
		}

		tiers = helper.tiersDAO.save(tiers);
		//TODO(BNM)Pour les nonabitants sans numero de ocntribuables comment on fait ?
		errorsManager.setMigratedOk(tiers.getNumero());
		getTiersList().add(tiers);

		if (LOGGER.isDebugEnabled()) {
//			LOGGER.debug("Tiers saved: "+numero);
			setRunningMessage("Migration de "+tiers.getNatureTiers()+"("+numero+") total: "+status.getGlobalNbObjectsMigrated());
		}
		return tiers;
	}

	protected ArrayList<Tiers> getTiersList() {
		return tierss;
	}

	protected boolean isInterrupted() {
		return status.interrupted();
	}

	protected void setRunningMessage(String msg) {
		LOGGER.debug("Status: "+msg);
		status.setMessage(msg);
	}

	public void doCommit() throws Exception {

		int nb = getTiersList().size();
		if (nb >= helper.nbrInsertBeforeCommit) {

			commitAndOpenTransaction();
			getTiersList().clear();
		}
	}

	public void endTransaction() throws Exception {

		if (txStatus == null) {
			throw new RuntimeException("Il n'y a pas de transaction en cours.");
		}
		if (txStatus.isCompleted()) {
			throw new RuntimeException("La transaction est déjà comittée/rollée-back !");
		}

		if (!txStatus.isRollbackOnly()) {
			try {
				doBeforeCommit();
			}
			catch (Exception e) {
				LOGGER.error(e, e);
				txStatus.setRollbackOnly();
			}
		}

        try {
			boolean wasRollback = commitTransaction();
			if (!wasRollback) {
				doAfterCommit();
			}
		}
		catch (ValidationException valE) {
			if (getTiersList().size() == 1) {
				MigrationError error = new MigrationError();
				error.setNoContribuable(getTiersList().get(0).getNumero());
				error.setMessage(valE.getErrors().get(0));
				errors.add(error);
			}
			updateMigregErrorsForTiersList();
		}
		catch (Exception e) {
			LOGGER.error(e,e);
			// Si on a une exception, on fait rien. Les MigregErrors ont été créées.
		}

//		if (!wasRollback) {
//			doAfterCommit();
//		}
//		else {
////			updateMigregErrorsForTiersList("Batch rollback");
//			updateMigregErrorsForTiersList();
//		}

		getTiersList().clear();
	}

	public void beginTransaction() {

		if (txStatus != null) {
			throw new RuntimeException("Il y a déjà une transaction en cours.");
		}

		txStatus = helper.transactionManager.getTransaction(new DefaultTransactionDefinition());

		Assert.notNull(txStatus);
		Assert.isTrue(txStatus.isNewTransaction());
	}

	private boolean commitTransaction() throws Exception {

		Assert.notNull(txStatus);
		Assert.isFalse(txStatus.isCompleted());

		setRunningMessage("Commit des données...");

		boolean wasRollback = txStatus.isRollbackOnly();
		try {
			if (txStatus.isRollbackOnly()) {
				helper.transactionManager.rollback(txStatus);
			}
			else {
				helper.transactionManager.commit(txStatus);
			}
		}
		finally {
			txStatus = null;
		}

		return wasRollback;
	}

	private void cleanTransaction() {
		txStatus = null;
	}

	public void commitAndOpenTransaction() {

		LOGGER.info("Début du commit no "+(getNbCommits()+1)+"...");

		try {
			endTransaction();
		}
		catch (Exception e) {
			LOGGER.error(e,e);
			Audit.error("Problème au commit : "+e.getMessage());
			cleanTransaction();
		}

		LOGGER.info("Commit no "+(getNbCommits()+1)+" terminé");
		addOneCommit();

		beginTransaction();
	}

	public int addOneCommit() {
		nbCommits++;
		return nbCommits;
	}
	public int getNbCommits() {
		return nbCommits;
	}

	public void addTiers(Tiers tiers) {
		Assert.isTrue(tiers.getNumero() > 0);
		ArrayList<Tiers> list = tierss;
		list.add(tiers);
	}


	protected String getRootMessage(Exception e) {

		String msg = "["+e.getMessage()+"]";

		Throwable inner = e;
		while (inner.getCause() != null) {
			msg += " ["+inner.getCause()+"]";
			inner = e.getCause();
		}
		return msg;
	}

	/**
	 * Change le message des MigregErrors pour tous les tiers dans la liste courante
	 * @param message
	 */
	protected void updateMigregErrorsForTiersList(final String message) {
		TransactionTemplate tmpl = new TransactionTemplate(helper.transactionManager);
		tmpl.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				for (Tiers t : getTiersList()) {
					errorsManager.setInError(t.getNumero(), message, null, TypeMigRegError.ROLLBACK_ERROR);
				}
				return null;
			}
		});
	}

	/**
	 * Change le message des MigregErrors pour tous les tiers dans la liste courante
	 * @param message
	 */
	protected void updateMigregErrorsForTiersList() {
		TransactionTemplate tmpl = new TransactionTemplate(helper.transactionManager);
		tmpl.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				for (Tiers t : getTiersList()) {
					String message = "Batch rollback";
					TypeMigRegError typeErreur = TypeMigRegError.ROLLBACK_ERROR;
					for (MigrationError error : errors) {
						if (t.getNumero().equals(error.getNoContribuable())) {
							if (TypeMigRegError.A_TRANSFORMER_EN_PP.equals(error.getTypeErreur())) {
								message = error.getMessage();
								typeErreur = TypeMigRegError.A_TRANSFORMER_EN_PP;
								break;
							}
							message = error.getMessage();
							typeErreur = TypeMigRegError.ERROR_APPLICATIVE;
						}
					}
					errorsManager.setInError(t.getNumero(), message, null, typeErreur);
				}
				return null;
			}
		});
	}

}
