package ch.vd.uniregctb.migreg;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.RequiredTransactionDefinition;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.dbutils.SqlFileExecutor;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.hibernate.interceptor.ModificationLogInterceptor;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.OfficeImpotHibernateInterceptor;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migreg.HostMigratorHelper.IndexationMode;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class HostMigrationManager implements DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(HostMigrationManager.class);

	/**
	 * le fichier de truncate des tables
	 */
	private static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	List<Long> listCtbInError = new ArrayList<Long>();

	private final StringBuilder employeurValide = null;

	private final HostMigratorHelper helper = new HostMigratorHelper();

	public HostMigratorHelper getHelper() {
		return helper;
	}

	private int nbCollAdminCreated;

	/**
	 * L'initialize est appelé une SEULE fois pour toutes les Threads
	 *
	 * @throws Exception
	 */
	private void initialize(MigregStatusManager mgr) throws Exception {
		mgr.setMessage("Initialization...");
		helper.initialize();
	}

	/**
	 * Le terminate est appelé une SEULE fois pour toutes les Threads
	 *
	 * @throws Exception
	 */
	private void terminate(MigregStatusManager mgr) throws Exception {
		if (mgr != null) {
			mgr.setMessage("Terminaison...");
		}
		helper.terminate();
	}

	// Spring
	public void destroy() throws Exception {
		terminate(null);
	}

	public int execute(MigRegLimitsList limitsList, MigregStatusManager mgr) throws Exception {

		final String limitsName = limitsList.getName();
		final String errorMode = (helper.errorsProcessing ? "avec reprise d'erreurs" : "sans reprise d'erreurs");
		final String forceIndexation = (helper.forceIndexationAtEnd ? "avec lancement de l'indexation à la fin du processus"
				: "sans lancement de l'indexation à la fin du processus");
		final String message = String.format("Démarrage de la migration %s (%s, %s).", limitsName, errorMode, forceIndexation);
		Audit.info(message);

		Assert.notNull(mgr);
		// Assert.isNull(RegDate.get(1901, 1, 2), "Le 1er janvier 1901 doit être une date nulle !"
		// + " Veuillez spécifier la propriété -DDateConstants.EARLY=19010102 au démarrage de la jvm.");

		initialize(mgr);

		mgr.reset();

		long begin = System.currentTimeMillis();

		// Chargement des CTB dans la table des erreurs s'il y a lieu.
		if (isErrorsProcessing()) {
			// listCtbInError = helper.migrationErrorDAO.getAllNoCtbForTypeErrorNeq(TypeMigRegError.DEFAULT_ERROR);
			listCtbInError = helper.migrationErrorDAO.getAllNoCtb();
			LOGGER.debug("Nombre d'erreurs à traiter : " + listCtbInError.size());
			Collections.sort(listCtbInError);
			helper.setNbrInsertBeforeCommit(1);
			limitsList.setWantTruncate(false);
			helper.forceTruncateDatabase = false;
		}

		// Clear la database
		/*
		 * final boolean truncate = (limitsList.isWantTruncate() || helper.forceTruncateDatabase); LOGGER.info("Truncate DB ? " + truncate);
		 * if (!mgr.interrupted() && truncate) { mgr.setMessage("Truncating database"); clearDatabase(); }
		 */

		// Obtenir la liste des numéros de CTB non utilisé entre 50000000 et 60000000 sur le host pour
		// l'attribution des numéros de tiers aux nonHabitants.
		getLstCtbFreeFromRange(50000000, 60000000);

		helper.setIndexationMode(IndexationMode.NONE); // chaque thread doit appeler cette méthode !

		if (isValidationDisabled()) {
			helper.disableValidation(); // chaque thread doit appeler cette méthode !
		}

		// createCollectivitesAdministratives();

		// Chargement de la table des périodes fiscales et des modèles de document en partant de l'année fiscale 2003.
		// savePeriodeFiscaleFrom(2003);
		// saveModeleDocumentFrom(2003);
		// saveCEDI();

		LOGGER.debug("nbrInsertBeforeCommit=" + helper.nbrInsertBeforeCommit + " " + limitsList);

		int nb = 0;
		for (MigRegLimits limits : limitsList) {
			if (!mgr.interrupted()) {
				nb += execute(limits, limitsList.getIndexationMode(), helper.nbrThread, mgr);
			}
		}

		// Mariés seuls
		LOGGER.debug("Faire le traitement des mariés seuls ? " + limitsList.isWantMariesSeuls());
		if (limitsList.isWantMariesSeuls()) {
			MarieSeulCreator ms = new MarieSeulCreator(helper, mgr);
			ms.migrate();
		}

		// Tutelles
		if (limitsList.isWantTutelles()) {
			TutellesLoader tutellesLoader = new TutellesLoader(helper, mgr);
			tutellesLoader.migrate();
		}

		if (forceIndexationAtEnd() || limitsList.getIndexationMode().equals(IndexationMode.AT_END)) {
			mgr.setMessage("Indexation de la base de données...");
			indexAll(mgr);
		}

		long end = System.currentTimeMillis();

		terminate(mgr);

		final int tiersMigres = mgr.getGlobalNbObjectsMigrated();
		final int tiersCount = helper.tiersDAO.getCount(Tiers.class);
		final int tiersErreur = helper.migrationErrorDAO.getCount(MigrationError.class);

		final String msg = String.format(
				"Migration %s (%s, %s) terminée. Temps: %d secondes. %d ctbs nouvellement migrés. %d ctbs en erreur. %d ctbs en base.",
				limitsName, errorMode, forceIndexation, (end - begin) / 1000, tiersMigres, tiersErreur, tiersCount);
		Audit.success(msg);
		mgr.setMessage(msg);

		LOGGER.info("Il y a " + tiersCount + " tiers dans la DB");
		LOGGER.info("Il y a " + helper.globalTiersIndexer.getApproxDocCount() + " documents dans l'index");

		return nb;
	}

	private int execute(MigRegLimits limits, IndexationMode mode, int nbThreads, MigregStatusManager mgr) throws Exception {

		Assert.notNull(mgr);
		Assert.isTrue(nbThreads > 0);

		mgr.setMessage("En cours...");
		//CHargement du ficheir de rapprochement pour les employeurs et les pm
		helper.loadRapprochementpmEmployeur();
		// Cas particulier
		if (nbThreads == 1) {
			internalExecute(limits, mode, mgr);
		}
		else {

			// CTBs
			int ctbRange = 0;
			int srcRange = 0;
			int droitRange = 0;
			// int srcCurrent = 0;
			int empRange = 0;
			// int empCurrent = 0;
			{
				// int MAX_BY_THREADS = 100;
				int MAX_BY_THREADS = 10000;
				if (helper.errorsProcessing) {
					MAX_BY_THREADS = 1000;
				}
				// int MAX_BY_THREADS = 20000000;
				LOGGER.debug("max_by_thread : " + MAX_BY_THREADS);
				// int ctbCurrent = 0;
				if (limits.ctbFirst != null) {
					// ctbCurrent = limits.ctbFirst;
					ctbRange = (int) ((limits.ctbEnd - (double) limits.ctbFirst) / nbThreads) + 1;
					if (ctbRange > MAX_BY_THREADS) {
						ctbRange = MAX_BY_THREADS;
					}
					else if (ctbRange < 2 * nbThreads) {
						ctbRange = 2 * nbThreads;
					}
				}
				if (limits.srcFirst != null) {
					// srcCurrent = limits.srcFirst;
					srcRange = (int) ((limits.srcEnd - (double) limits.srcFirst) / nbThreads) + 1;
					if (srcRange > MAX_BY_THREADS) {
						srcRange = MAX_BY_THREADS;
					}
					else if (srcRange < 2 * nbThreads) {
						srcRange = 2 * nbThreads;
					}
				}


				if (limits.srcFirstBadAvs != null) {
					// srcCurrent = limits.srcFirst;
					srcRange = (int) ((limits.srcEndBadAvs - (double) limits.srcFirstBadAvs) / nbThreads) + 1;
					if (srcRange > MAX_BY_THREADS) {
						srcRange = MAX_BY_THREADS;
					}
					else if (srcRange < 2 * nbThreads) {
						srcRange = 2 * nbThreads;
					}
				}

				if (limits.empFirst != null) {
					// empCurrent = limits.empFirst;
					empRange = (int) ((limits.empEnd - (double) limits.empFirst) / nbThreads) + 1;
					// TODO(GDY) : range truncate
					if (empRange > MAX_BY_THREADS) {
						empRange = MAX_BY_THREADS;
					}
					else if (empRange < 2 * nbThreads) {
						empRange = 2 * nbThreads;
					}
				}
				if (limits.indiDroitAccesFirst != null) {
					// empCurrent = limits.empFirst;
					droitRange = (int) ((limits.indiDroitAccesEnd - (double) limits.indiDroitAccesFirst) / nbThreads) + 1;
					// TODO(GDY) : range truncate
					if (droitRange > MAX_BY_THREADS) {
						droitRange = MAX_BY_THREADS;
					}
					else if (droitRange < 2 * nbThreads) {
						droitRange = 2 * nbThreads;
					}
				}
			}

			// On crée une liste avec tous les range
			ArrayList<MigRegLimits> list = new ArrayList<MigRegLimits>();
			{
				if (limits.ctbFirst != null && limits.ctbEnd != null) {
					int ctbCurrent = limits.ctbFirst;
					while (ctbCurrent <= limits.ctbEnd) {
						MigRegLimits subLimits = new MigRegLimits();
						list.add(subLimits);

						subLimits.copyFrom(limits);
						if (subLimits.needPopulation(MigRegLimits.MIGRE_ORDINAIRE)) {
							subLimits.ctbFirst = ctbCurrent;
							subLimits.ctbEnd = ctbCurrent + ctbRange - 1;
							ctbCurrent += ctbRange;
							if (subLimits.ctbEnd > limits.ctbEnd) {
								subLimits.ctbEnd = limits.ctbEnd;
							}
							if (subLimits.ctbFirst >= subLimits.ctbEnd) {
								subLimits.setOrdinaire(null, null);
							}
						}
						LOGGER.info("Limite #" + list.size() + " : " + subLimits);
					}
				}
			}

			{
				//CHargement du ficheir de rapprochement pour les employeurs et les pm
				//helper.loadRapprochementpmEmployeur();

				if (limits.empFirst != null && limits.empEnd != null) {
					int debiteurCurrent = limits.empFirst;
					while (debiteurCurrent <= limits.empEnd) {
						MigRegLimits subLimits = new MigRegLimits();
						list.add(subLimits);

						subLimits.copyFrom(limits);
						if (subLimits.needPopulation(MigRegLimits.MIGRE_DEBITEURS)) {
							subLimits.empFirst = debiteurCurrent;
							subLimits.empEnd = debiteurCurrent + empRange - 1;
							debiteurCurrent += empRange;
							if (subLimits.empEnd > limits.empEnd) {
								subLimits.empEnd = limits.empEnd;
							}
							if (subLimits.empFirst >= subLimits.empEnd) {
								subLimits.setDebiteurs(null, null);
							}
						}
						LOGGER.info("Limite #" + list.size() + " : " + subLimits);
					}
				}
			}

			{
				if (limits.srcFirst != null && limits.srcEnd != null) {
					int sourcierCurrent = limits.srcFirst;
					while (sourcierCurrent <= limits.srcEnd) {
						MigRegLimits subLimits = new MigRegLimits();
						list.add(subLimits);

						subLimits.copyFrom(limits);
						if (subLimits.needPopulation(MigRegLimits.MIGRE_SOURCIERS)) {
							subLimits.srcFirst = sourcierCurrent;
							subLimits.srcEnd = sourcierCurrent + srcRange - 1;
							sourcierCurrent += srcRange;
							if (subLimits.srcEnd > limits.srcEnd) {
								subLimits.srcEnd = limits.srcEnd;
							}
							if (subLimits.srcFirst >= subLimits.srcEnd) {
								subLimits.setSourciers(null, null);
							}
						}
						LOGGER.info("Limite #" + list.size() + " : " + subLimits);
					}
				}
			}
			{
				if (limits.srcFirstBadAvs != null && limits.srcEndBadAvs != null) {
					int sourcierCurrent = limits.srcFirstBadAvs;
					while (sourcierCurrent <= limits.srcEndBadAvs) {
						MigRegLimits subLimits = new MigRegLimits();
						list.add(subLimits);

						subLimits.copyFrom(limits);
						if (subLimits.needPopulation(MigRegLimits.SOURCIERS_BADAVS)) {
							subLimits.srcFirstBadAvs = sourcierCurrent;
							subLimits.srcEndBadAvs = sourcierCurrent + srcRange - 1;
							sourcierCurrent += srcRange;
							if (subLimits.srcEndBadAvs > limits.srcEndBadAvs) {
								subLimits.srcEndBadAvs = limits.srcEndBadAvs;
							}
							if (subLimits.srcFirstBadAvs >= subLimits.srcEndBadAvs) {
								subLimits.setSourciers(null, null);
							}
						}
						LOGGER.info("Limite #" + list.size() + " : " + subLimits);
					}
				}
			}
			{
				if (limits.indiDroitAccesFirst != null && limits.indiDroitAccesEnd != null) {
					int indiCurrent = limits.indiDroitAccesFirst;
					while (indiCurrent <= limits.indiDroitAccesEnd) {
						MigRegLimits subLimits = new MigRegLimits();
						list.add(subLimits);

						subLimits.copyFrom(limits);
						if (subLimits.needPopulation(MigRegLimits.MIGRE_DROITS_ACCES)) {
							subLimits.indiDroitAccesFirst = indiCurrent;
							subLimits.indiDroitAccesEnd = indiCurrent + droitRange - 1;
							indiCurrent += droitRange;
							if (subLimits.indiDroitAccesEnd > limits.indiDroitAccesEnd) {
								subLimits.indiDroitAccesEnd = limits.indiDroitAccesEnd;
							}
							if (subLimits.indiDroitAccesFirst >= subLimits.indiDroitAccesEnd) {
								subLimits.setDroitAcces(null, null);
							}
						}
						LOGGER.info("Limite #" + list.size() + " : " + subLimits);
					}
				}
			}

			LOGGER.info("Apres decoupage, Migration en " + list.size() + " parties");

			{
				int index = 0;
				ArrayList<HostMigratorThread> threads = new ArrayList<HostMigratorThread>();
				while (index < list.size()) {

					if (threads.size() < nbThreads) {
						MigRegLimits subLimits = list.get(index);
						index++;
						HostMigratorThread t = new HostMigratorThread(this, subLimits, mode, mgr);
						t.setName("MigR-" + index);
						LOGGER.info("Starting Thread #" + index + " : " + t.getName());
						threads.add(t);
						t.start();
					}
					else {
						Thread.sleep(200);
					}

					for (int i = 0; i < threads.size(); i++) {
						HostMigratorThread t = threads.get(i);
						if (t.getState() == Thread.State.TERMINATED) {
							t.join(); // Ne doit pas bloquer
							threads.remove(i);
							LOGGER.info("Ending Thread #" + i + " : " + t.getName());
						}
					}
				}

				// Join les threads terminées
				for (HostMigratorThread t : threads) {
					t.join();
				}
			}
		}
		LOGGER.info("Fin de la limite : " + limits);

		return mgr.getGlobalNbObjectsMigrated();
	}

	protected int internalExecute(MigRegLimits limits, IndexationMode mode, MigregStatusManager mgr) throws Exception {

		helper.setIndexationMode(mode); // chaque thread doit appeler cette méthode !

		if (isValidationDisabled()) {
			helper.disableValidation(); // chaque thread doit appeler cette méthode !
		}

		int nbCtbMigrated = 0;
		int nbDroit = 0;

		if (!mgr.interrupted() && limits.needPopulation(MigRegLimits.MIGRE_ORDINAIRE)) {
			LOGGER.info("Limites de ce Thread : " + limits.ctbFirst + " " + limits.ctbEnd);
			if (listCtbInError.isEmpty() || isRangeToExecute(limits)) {
				HostContribuableMigrator ctb = new HostContribuableMigrator(helper, limits, mgr, listCtbInError);
				nbCtbMigrated += ctb.migrate();
			}
		}

		// Sourciers
		if (!mgr.interrupted() && limits.needPopulation(MigRegLimits.MIGRE_SOURCIERS)) {
			LOGGER.info("Limites de ce Thread : " + limits.srcFirst + " " + limits.srcEnd);

			HostSourcierMigrator sourcier = new HostSourcierMigrator(helper, limits, mgr, employeurValide);
			nbCtbMigrated += sourcier.migrate();
		}

		// Sourciers BADAVS
		if (!mgr.interrupted() && limits.needPopulation(MigRegLimits.SOURCIERS_BADAVS)) {
			LOGGER.info("Limites de ce Thread : " + limits.srcFirstBadAvs + " " + limits.srcEndBadAvs);

			HostSourcierAvs sourcierBadAvs = new HostSourcierAvs(helper, limits, mgr);
			nbCtbMigrated += sourcierBadAvs.migrate();
		}



		// Employeurs
		if (!mgr.interrupted() && limits.needPopulation(MigRegLimits.MIGRE_DEBITEURS)) {

			LOGGER.info("Limites de ce Thread : " + limits.empFirst + " " + limits.empEnd);

			HostEmployeurMigrator emp = new HostEmployeurMigrator(helper, limits, mgr);
			nbCtbMigrated += emp.migrate();
		}
		// Droit accès
		if (!mgr.interrupted() && limits.needPopulation(MigRegLimits.MIGRE_DROITS_ACCES)) {

			LOGGER.info("Limites de ce Thread : " + limits.indiDroitAccesFirst + " " + limits.indiDroitAccesEnd);

			HostDroitAccesMigrator droitAccesMigrator = new HostDroitAccesMigrator(helper, limits, mgr);
			nbDroit += droitAccesMigrator.migrate();
			LOGGER.info("Nombre de droits migrés par ce Thread: " + nbCtbMigrated);
		}

		helper.globalTiersIndexer.flush();

		LOGGER.info("CTB migrés par ce Thread: " + nbCtbMigrated);

		return nbCtbMigrated;
	}

	private boolean isRangeToExecute(MigRegLimits limits) {
		for (Long noCtb : listCtbInError) {
			if (noCtb.compareTo(new Long(limits.ctbFirst)) >= 0 && (noCtb.compareTo(new Long(limits.ctbEnd))) <= 0) {
				return true;
			}
		}
		return false;
	}

	private void indexAll(StatusManager mgr) {

		TransactionStatus tx = helper.transactionManager.getTransaction(new RequiredTransactionDefinition());
		try {
			if (mgr != null) {
				mgr.setMessage("Indexation des tiers...");
			}
			LOGGER.info("Indexation des tiers migrés AT_END...");
			helper.globalTiersIndexer.indexAllDatabaseAsync(mgr, 8, Mode.FULL, true);
		}
		catch (Exception e) {
			// Dans le cas de l'indexation, on accepte de pas indexer. Le Tiers est mis a DIRTY
			LOGGER.error(e, e);
		}
		finally {
			helper.transactionManager.rollback(tx);
		}
		LOGGER.info("Indexation des tiers terminée ...");
	}

	private void savePeriodeFiscaleFrom(final int startYear) {
		final int currentYear = DateHelper.getCurrentYear();
		Assert.isTrue(startYear < currentYear, "Chargement Périodes fiscales : Date de départ postérieure à la date courante!");

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		template.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				int start = (startYear == 0 ? 2003 : startYear);
				for (int year = start; year <= currentYear + 1; year++) {

					if (helper.periodeFiscaleDAO.getPeriodeFiscaleByYear(year) == null) {
						PeriodeFiscale pf = new PeriodeFiscale();
						pf.setAnnee(year);
						pf.setDefaultPeriodeFiscaleParametres();
						helper.periodeFiscaleDAO.save(pf);
					}
				}
				return null;
			}
		});
	}

	/**
	 * Défini tous les modèles de documents à partir de l'année spécifiée.
	 */
	private void saveModeleDocumentFrom(final int startYear) {

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		template.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				for (int year = startYear; year <= DateHelper.getCurrentYear() + 1; year++) {
					final PeriodeFiscale periode = helper.periodeFiscaleDAO.getPeriodeFiscaleByYear(year);
					Assert.notNull(periode);
					for (TypeDocument type : TypeDocument.values()) {
						if (helper.modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, type) == null) {
							ModeleDocument doc = new ModeleDocument();
							doc.setTypeDocument(type);
							doc = helper.modeleDocumentDAO.save(doc);
							if (type.equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH)) {
								ModeleFeuilleDocument feuille210 = new ModeleFeuilleDocument();
								feuille210.setIntituleFeuille("Déclaration complète");
								feuille210.setNumeroFormulaire("210");
								doc.addModeleFeuilleDocument(feuille210);
								ModeleFeuilleDocument feuille220 = new ModeleFeuilleDocument();
								feuille220.setIntituleFeuille("Annexe 1");
								feuille220.setNumeroFormulaire("220");
								doc.addModeleFeuilleDocument(feuille220);
								ModeleFeuilleDocument feuille230 = new ModeleFeuilleDocument();
								feuille230.setIntituleFeuille("Annexe 2 et 3");
								feuille230.setNumeroFormulaire("230");
								doc.addModeleFeuilleDocument(feuille230);
								ModeleFeuilleDocument feuille240 = new ModeleFeuilleDocument();
								feuille240.setIntituleFeuille("Annexe 4 et 5");
								feuille240.setNumeroFormulaire("240");
								doc.addModeleFeuilleDocument(feuille240);
								if (year >= 2009) {
									ModeleFeuilleDocument feuille310 = new ModeleFeuilleDocument();
									feuille310.setIntituleFeuille("Annexe 1-1");
									feuille310.setNumeroFormulaire("310");
									doc.addModeleFeuilleDocument(feuille310);
								}
							}
							else if (type.equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
								ModeleFeuilleDocument feuille250 = new ModeleFeuilleDocument();
								feuille250.setIntituleFeuille("Déclaration Vaud Tax");
								feuille250.setNumeroFormulaire("250");
								doc.addModeleFeuilleDocument(feuille250);
							}
							else if (type.equals(TypeDocument.DECLARATION_IMPOT_DEPENSE)) {
								ModeleFeuilleDocument feuille200 = new ModeleFeuilleDocument();
								feuille200.setIntituleFeuille("Déclaration Dépense");
								feuille200.setNumeroFormulaire("270");
								doc.addModeleFeuilleDocument(feuille200);
							}
							else if (type.equals(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE)) {
								ModeleFeuilleDocument feuille270 = new ModeleFeuilleDocument();
								feuille270.setIntituleFeuille("Déclaration Hors canton immeuble");
								feuille270.setNumeroFormulaire("200");
								doc.addModeleFeuilleDocument(feuille270);
							}
							periode.addModeleDocument(doc);
						}
					}
				}
				return null;
			}
		});
	}

	private void saveCEDI() {
		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				CollectiviteAdministrative cedi = helper.tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
				Assert.notNull(cedi);
				return null;
			}
		});
	}

	private void createCollectivitesAdministratives() {

		nbCollAdminCreated = 0;

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		template.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				for (ListOrderedMap colAdm : readAllCollectivite()) {
					if (helper.tiersService.getCollectiviteAdministrative((Integer) colAdm.getValue(0)) == null) {
						CollectiviteAdministrative collectivite = new CollectiviteAdministrative();
						collectivite.setNumeroCollectiviteAdministrative((Integer) colAdm.getValue(0));
						helper.tiersDAO.save(collectivite);
						++nbCollAdminCreated;
					}
				}
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private List<ListOrderedMap> readAllCollectivite() {

		String query = "SELECT DISTINCT NO_COL_ADM FROM " + helper.getTableDb2("COLLECTIVITE_ADM");

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		List<ListOrderedMap> listColAdm = helper.db2Template.queryForList(query);

		return listColAdm;
	}

	private void getLstCtbFreeFromRange(int ctbStart, int ctbEnd) throws Exception {
		if (ctbEnd == 0) {
			ctbEnd = 99999999;
		}
		String query = "SELECT " + "NO_CANTONAL " + "FROM " + helper.getTableDb2("IROLE_NO_CTB") + " " + "WHERE NO_CANTONAL BETWEEN "
				+ ctbStart + " AND " + ctbEnd;

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		Statement stat = helper.db2Connection.createStatement();
		ResultSet rs = stat.executeQuery(query);

		// List<Integer> lstNoCtbUsed = helper.db2Template.queryForList(query);
		int noCtbFree = ctbStart;
		try {
			while (rs.next()) {
				int noCtbUsed = rs.getInt("NO_CANTONAL");
				if (helper.listContribuableFree.size() > 10000) {
					break;
				}
				if (noCtbUsed == noCtbFree) {
					continue;
				}
				while (true) {
					noCtbFree++;
					if (noCtbUsed == noCtbFree || noCtbFree > ctbEnd || helper.listContribuableFree.size() > 10000) {
						break;
					}
					helper.listContribuableFree.add(noCtbFree);
				}
			}
		}
		finally {
			rs.close();
			stat.close();
		}

	}



	@SuppressWarnings("unused")
	private StringBuilder readEmployeurValide() throws Exception {
		String query = "   SELECT e.NO_EMPLOYEUR" + "   FROM " + helper.getTableIs("employeur") + " e" + "   WHERE e.ACTIF='O'      "
				+ "  OR e.DESIGN_ABREGEE LIKE '%@%'" + "  UNION" + "   SELECT emp.NO_EMPLOYEUR" + "   FROM  "
				+ helper.getTableIs("employeur") + " emp, " + helper.getTableIs("facture_employeur") + " fact" + "   WHERE emp.ACTIF='N'"
				+ "   AND emp.NO_EMPLOYEUR = fact.FK_CAE_EMPNO " + "   AND fact.FK_TFINOTECH = 1" + "   AND fact.CO_ETAT = 'B'"
				+ "   AND YEAR(fact.DAD_PER_EFFECTIVE)= 2009";

		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		StringBuilder listEmployeur = new StringBuilder();
		try {
			ResultSet rs = stmt.executeQuery(query);
			try {
				while (rs.next()) {
					listEmployeur.append(rs.getLong("NO_EMPLOYEUR"));
					listEmployeur.append(",");
				}
			}
			finally {
				rs.close();
			}
		}
		catch (Exception e) {
			LOGGER.info(e.getMessage());
		}
		finally {
			stmt.close();
		}
		if (listEmployeur.length() == 0) {
			return null;
		}
		return listEmployeur.deleteCharAt(listEmployeur.lastIndexOf(","));
	}

	private void clearDatabase() throws Exception {
		LOGGER.debug("Truncating database");
		SqlFileExecutor.execute(helper.transactionManager, helper.dataSource, CORE_TRUNCATE_SQL);

		helper.globalTiersIndexer.overwriteIndex();
	}

	public void setTiersService(TiersService tiersService) {
		helper.tiersService = tiersService;
	}

	public void setIsDataSource(DataSource isDataSource) {
		helper.isDataSource = isDataSource;
	}

	public void setEmpDataSource(DataSource empDataSource) {
		helper.empDataSource = empDataSource;
	}

	public void setIsSchema(String isSchema) {
		helper.isSchema = isSchema;
	}

	public void setIsTablePrefix(String tablePrefix) {
		helper.isTablePrefix = tablePrefix;
	}

	public void setIsScopeDataSource(DataSource isScopeDataSource) {
		helper.isScopeDataSource = isScopeDataSource;
	}

	public void setIsScopeSchema(String isScopeSchema) {
		helper.isScopeSchema = isScopeSchema;
	}

	public void setIsScopeTablePrefix(String isScopeTablePrefix) {
		helper.isScopeTablePrefix = isScopeTablePrefix;
	}

	/**
	 *
	 * @param db2DataSource
	 */
	public void setDb2DataSource(DataSource db2DataSource) {
		helper.db2DataSource = db2DataSource;
	}

	/**
	 *
	 * @param tiersDAO
	 */
	public void setTiersDAO(TiersDAO tiersDAO) {
		helper.tiersDAO = tiersDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		helper.droitAccesDAO = droitAccesDAO;
	}

	/**
	 *
	 * @param db2Schema
	 */
	public void setDb2Schema(String db2Schema) {
		helper.db2Schema = db2Schema;
	}

	public void setMigrationErrorDAO(MigrationErrorDAO migrationErrorDAO) {
		helper.migrationErrorDAO = migrationErrorDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		helper.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		helper.hibernateTemplate = hibernateTemplate;
	}

	public int getNbrInsertBeforeCommit() {
		return helper.nbrInsertBeforeCommit;
	}

	public void setNbrInsertBeforeCommit(int nbrInsertBeforeCommit) {
		helper.nbrInsertBeforeCommit = nbrInsertBeforeCommit;
	}

	public void setModeEssai(boolean modeEssai) {
		helper.modeEssai = modeEssai;
	}

	public void setFichierRapprochement(String fichierRapprochement) {
		helper.fichierRapprochement = fichierRapprochement;
	}

	public boolean isErrorsProcessing() {
		return helper.errorsProcessing;
	}

	public void setErrorsProcessing(boolean errorsProcessing) {
		helper.errorsProcessing = errorsProcessing;
	}

	public boolean isValidationDisabled() {
		return helper.validationDisabled;
	}

	public void setValidationDisabled(boolean validationDisabled) {
		helper.validationDisabled = validationDisabled;
	}

	public boolean forceIndexationAtEnd() {
		return helper.forceIndexationAtEnd;
	}

	public void setForceIndexationAtEnd(boolean indexation) {
		helper.forceIndexationAtEnd = indexation;
	}

	public int getNbrThread() {
		return helper.nbrThread;
	}

	public void setNbrThread(int nbrThread) {
		helper.nbrThread = nbrThread;
	}

	public synchronized void setDateLimiteRepriseDataSourcier(String dateLimiteRepriseDataSourcier) {
		helper.dateLimiteRepriseDataSourcier = RegDate.get(DateHelper.displayStringToDate(dateLimiteRepriseDataSourcier));
	}

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		helper.globalTiersIndexer = globalTiersIndexer;
	}

	public void setOidInterceptor(OfficeImpotHibernateInterceptor oidInterceptor) {
		helper.oidInterceptor = oidInterceptor;
	}

	public void setDataSource(DataSource ds) {
		helper.dataSource = ds;
	}

	public void setIsDbType(String isDbType) {
		helper.isDbType = isDbType;
	}

	public void setIsScopeDbType(String isScopeDbType) {
		helper.isScopeDbType = isScopeDbType;
	}

	public void setForceTruncateDatabase(Boolean truncate) {
		helper.forceTruncateDatabase = truncate;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		helper.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		helper.adresseTiersDAO = adresseTiersDAO;
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		helper.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		helper.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setModeleFeuilleDocumentDAO(ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO) {
		helper.modeleFeuilleDocumentDAO = modeleFeuilleDocumentDAO;
	}

	public void setSessionFactory(SessionFactory sf) {
		helper.sessionFactory = sf;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		helper.serviceCivilService = serviceCivilService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		helper.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setAdresseService(AdresseService adresseService) {
		helper.adresseService = adresseService;
	}

	public void setModificationLogInterceptor(ModificationLogInterceptor modificationLogInterceptor) {
		helper.modificationLogInterceptor = modificationLogInterceptor;
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		helper.setValidationInterceptor(validationInterceptor);
	}

	public int getNbCollAdminCreated() {
		return nbCollAdminCreated;
	}

}
