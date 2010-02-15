package ch.vd.uniregctb.migreg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeMigRegError;

public abstract class MigregErrorsManager {

	private static final Logger LOGGER = Logger.getLogger(MigregErrorsManager.class);

	protected final int ctbStart;
	protected final int ctbEnd;
	protected final HostMigratorHelper helper;

	protected List<Long> hostCtbsAtStart;
	protected Set<Long> tiersAtStart;

	/**
	 * Listes des erreurs indexées par numéros de contribuables.
	 * <p>
	 * Ces listes sont initialisées avant la migration à partir des données de la base. Pendant la migration, elles sont mises-à-jour en
	 * mémoire uniquement, et après la migration, elles sont commitées dans une transaction différente de celle utilisées pour la migration.
	 */
	private final Map<Long, MigrationError> errorsToInsert = new HashMap<Long, MigrationError>();
	private final Map<Long, MigrationError> errorsToRemove = new HashMap<Long, MigrationError>();

	public MigregErrorsManager(){
		ctbStart=0;
		ctbEnd=0;
		helper=null;
	}


	public MigregErrorsManager(HostMigratorHelper helper, int ctbStart, int ctbEnd) throws Exception {
		this.helper = helper;
		this.ctbStart = ctbStart;
		this.ctbEnd = ctbEnd;

		initializeLists();
	}

	private void initializeLists() throws Exception {

		final List<Long> tiersIds = getTiersInRange(ctbStart, ctbEnd);
		tiersAtStart = new HashSet<Long>(tiersIds);

		final List<MigrationError> errorList = getMigregErrorsInCtbRange(ctbStart, ctbEnd);
		final HashMap<Long, MigrationError> migregErrorsAtStart = new HashMap<Long, MigrationError>(errorList.size());
		for (MigrationError e : errorList) {
			migregErrorsAtStart.put(e.getNoContribuable(), e);
		}

		hostCtbsAtStart = getHostCtbsInRange(ctbStart, ctbEnd);


		// On considère que tous les CTBs a migreg sont en erreurs tant qu'ils ne sont pas OK
		//    - sauf ceux qui sont deja dans la base
		//    - sauf ceux qui sont dans MigregError
		for (Long numeroCtb : hostCtbsAtStart) {
			if (!tiersAtStart.contains(numeroCtb)) {

				boolean found = migregErrorsAtStart.containsKey(numeroCtb);
				if (!found) {
					MigrationError err = new MigrationError();
					err.setNoContribuable(numeroCtb);
					err.setMessage("Erreur par défaut, tiers non migré");
					err.setTypeErreur(TypeMigRegError.DEFAULT_ERROR);
					errorsToInsert.put(numeroCtb, err);
				}
			}
		}
		errorsToRemove.clear();
	}

	/**
	 * Cette méthode est appelée lorsque la migration a été rollée-back.
	 */
	public void onRollback() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Rollback de la tranche " + ctbStart + " - " + ctbEnd);
		}

		// rollback de la transaction -> toutes les erreurs à supprimer sont finalement converties en erreurs réelles.
		errorsToInsert.putAll(errorsToRemove);
		errorsToRemove.clear();
	}

	/**
	 * Cette méthode est appelée de toutes façons à la fin de la migration
	 */
	public void terminate() {

		// Trace
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Range: " + ctbStart + " - " + ctbEnd);
			for (Long numeroCtb : hostCtbsAtStart) {
				LOGGER.trace("Tiers dans le Host: " + numeroCtb);
			}

			for (MigrationError error : errorsToRemove.values()) {
				LOGGER.trace("MigReg error to remove: " + error.getNoContribuable());
			}

			for (MigrationError error : errorsToInsert.values()) {
				if (error.getId() == null) {
					LOGGER.trace("MigReg error to insert: " + error.getNoContribuable());
				}
				else {
					LOGGER.trace("MigReg error to update: " + error.getNoContribuable());
				}
			}
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Fin de la tranche %d - %d (%d ctbs migrés, %d ctbs en erreur)", ctbStart, ctbEnd, errorsToRemove
					.size(), errorsToInsert.size()));
		}

		TransactionTemplate tmpl = new TransactionTemplate(helper.transactionManager);
		tmpl.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				for (MigrationError error : errorsToRemove.values()) {
					final Long id = error.getNoContribuable();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Suppression d'une erreur pour le CTB " + id);
					}
					helper.migrationErrorDAO.removeForContribuable(id);
				}

				for (MigrationError error : errorsToInsert.values()) {
					if (LOGGER.isDebugEnabled()) {
						if (error.getId() == null) {
							LOGGER.debug("Insertion d'une erreur pour le CTB " + error.getNoContribuable());
						}
						else {
							LOGGER.debug("Mise-à-jour d'une erreur pour le CTB " + error.getNoContribuable());
						}
					}
					helper.migrationErrorDAO.save(error);
				}

				return null;
			}
		});
	}

	public boolean tiersExistedAtStart(long numeroCtb) {
		return tiersAtStart.contains(numeroCtb);
	}

	private List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		List<Long> list = helper.tiersDAO.getTiersInRange(ctbStart, ctbEnd);
		return list;
	}

	private List<MigrationError> getMigregErrorsInCtbRange(int debut, int fin) throws Exception {

		List<MigrationError> list = helper.migrationErrorDAO.getMigregErrorsInCtbRange(debut, fin);
		return list;
	}

	protected abstract List<Long> getHostCtbsInRange(int debut, int fin);



	public void setMigratedOk(long numCtb) {
		final Long numeroCtb = Long.valueOf(numCtb);

		MigrationError error = errorsToInsert.remove(numeroCtb);
		if (error == null) {
			// si l'erreur existait en base avant le lancement de la migration, elle n'est pas forcément chargée en mémoire.
			error = helper.migrationErrorDAO.getErrorForContribuable(numeroCtb);
			Assert.notNull(error);
		}

		errorsToRemove.put(numeroCtb, error);
	}

	public void setInError(long numCtb, String message, TypeMigRegError type) {
		setInError(numCtb, null, message, null, type);
	}

	public void setInError(long numCtb, String message, String nom, TypeMigRegError type) {
		setInError(numCtb, null, message, nom, type);
	}

	public void setInError(Long numCtb, Integer noIndividu, String message, String nom, TypeMigRegError type) {

		final Long numeroCtb = Long.valueOf(numCtb);
		Assert.notNull(numeroCtb);
		Assert.isFalse(tiersAtStart.contains(numeroCtb), "Tiers à mettre en erreur : " + numeroCtb + " est déjà en DB");

		// Récupère l'erreur si elle existe dans la liste des erreurs à supprimer
		MigrationError error = errorsToRemove.remove(numeroCtb);

		if (error == null) {
			// Met-à-jour l'erreur si elle existe déjà dans la liste des erreurs à insérer
			error = errorsToInsert.get(numeroCtb);
		}

		if (error == null) {
			// Charge l'erreur en mémoire si elle existe dans la DB
			error = helper.migrationErrorDAO.getErrorForContribuable(numeroCtb);
		}

		if (error == null) {
			// In Tiers qui avait marché mais qui est rollbacké
			error = new MigrationError();
			Assert.fail();
		}

		error.setNoContribuable(numeroCtb);
		error.setNoIndividu(noIndividu);
		error.setMessage(message);
		error.setNomContribuable(nom);
		error.setTypeErreur(type);
		errorsToInsert.put(numeroCtb, error);
	}


	public Map<Long, MigrationError> getErrorsToInsert() {
		return errorsToInsert;
	}
}
