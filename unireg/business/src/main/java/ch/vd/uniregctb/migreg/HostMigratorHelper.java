package ch.vd.uniregctb.migreg;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.hibernate.interceptor.ModificationLogInterceptor;
import ch.vd.uniregctb.indexer.async.AsyncTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.OfficeImpotHibernateInterceptor;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class HostMigratorHelper {

	private static final Logger LOGGER = Logger.getLogger(HostMigratorHelper.class);
	protected static final Logger SQL_LOG = Logger.getLogger("ch.vd.uniregctb.migreg.sqllog");

	public enum IndexationMode {
		ON_THE_FLY,
		NONE,
		ASYNC,
		AT_END
	}

	public SessionFactory sessionFactory;

	/**
	 * Transactiuon Mananger
	 */
	public PlatformTransactionManager transactionManager;

	/**
	 * Hibernate template
	 */
	public HibernateTemplate hibernateTemplate;

	// Oracle
	public DataSource dataSource;

	/**
	 * La connection Oracle
	 */
	public Connection orclConnection;
	/**
	 * Le template Oracle
	 */
	public JdbcTemplate orclTemplate;


	// DB2
	/**
	 * La data source DB2
	 */
	public DataSource db2DataSource;
	/**
	 * Le Schema DB2 utilisé
	 */
	public String db2Schema;
	/**
	 * La connection DB2
	 */
	public Connection db2Connection;
	/**
	 * Le template DB2
	 */
	public JdbcTemplate db2Template;

	public List<Integer> listContribuableFree;

	//EMP ACI
	/**
	 * La data source EMP ACI
	 */
	public DataSource empDataSource;

	/**
	 * La connection  EMP ACI
	 */
	public Connection empConnection;
	/**
	 * Le template EMP ACI
	 */
	public JdbcTemplate empTemplate;


	// IS
	/**
	 * La data source IS
	 */
	public DataSource isDataSource;
	/**
	 * La data source IS
	 */
	public DataSource isScopeDataSource;
	/**
	 * La connection IS
	 */
	public Connection isConnection;
	/**
	 * Le template IS
	 */
	public JdbcTemplate isTemplate;
	/**
	 * La connection IS
	 */
	public Connection isScopeConnection;
	/**
	 * Le template IS
	 */
	public JdbcTemplate isScopeTemplate;
	/**
	 * Le schema IS ou se trouvent les données
	 */
	public String isSchema;
	/**
	 * Le prefix des tables IS
	 */
	public String isTablePrefix;
	/**
	 * La base de données attaquée pour IS
	 */
	/**
	 * Le schema IS ou se trouvent les données
	 */
	public String isScopeSchema;
	/**
	 * Le prefix des tables IS
	 */
	public String isScopeTablePrefix;
	/**
	 * La base de données attaquée pour IS
	 */
	public String isDbType;
	/**
	 * La base de données attaquée pour IS
	 */
	public String isScopeDbType;
	/**
	 * La date limite de reprise des données pour les sourciers
	 */
	public RegDate dateLimiteRepriseDataSourcier;

	/**Le mode essai de la migration
	 *
	 */

	public boolean modeEssai;

	/**
	 * Nom du fichier permettant de rapprocher les personnes morales des débiteurs
	 */

	public String fichierRapprochement;

	/**
	 * hashMap liant le debiteur à une personne morale
	 */
	public HashMap<Integer, Long> mapDebiteurPm;

	protected AsyncTiersIndexer asyncIndexer;


	public TiersDAO tiersDAO;
	public MigrationErrorDAO migrationErrorDAO;
	public PeriodeFiscaleDAO periodeFiscaleDAO;
	public AdresseTiersDAO adresseTiersDAO;
	public ModeleDocumentDAO modeleDocumentDAO;
	public ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO;
	public DroitAccesDAO droitAccesDAO;
	public RapportEntreTiersDAO rapportEntreTiersDAO;

	public ModificationLogInterceptor modificationLogInterceptor;

	public GlobalTiersIndexer globalTiersIndexer;
	public OfficeImpotHibernateInterceptor oidInterceptor;

	public TiersService tiersService;

	public ServiceCivilService serviceCivilService;

	public ServiceInfrastructureService serviceInfrastructureService;

	public AdresseService adresseService;

	/**
	 * Le nombre d'insert qui doit être fait avant de committer
	 * a à voir avec la performance de la migration
	 */
	public int nbrInsertBeforeCommit;

	/**
	 * Le nombre de thread à utiliser pour le traitement.
	 */
	public int nbrThread;

	/**
	 * Effectuer ou non le traitement des erreurs de la migration.
	 * Repasse le traitement des contribuables trouvés dans la table des erreurs
	 * et commit après chaque tiers inséré.
	 * Permet de récupérer les tiers non encore traités suite à un rollback de masse.
	 */
	public boolean errorsProcessing;

	/**
	 * Permet de désactiver la validation.
	 * <p>
	 * <b>A N'UTILISER QUE LORS DE LA DERNIERE PHASE DE MIGRATION, lorsque tous les algorithmes possibles ont été épuisés pour rendre
	 * valides les tiers</b>.
	 * <p>
	 * Un tiers migrés comme non-valide devra être corrigé après-coup dans l'interface, car il ne peut pas être processé par Unireg.
	 */
	public boolean validationDisabled;

	/**
	 * Effectuer ou non l'indexation de tout à la fin du processus.
	 * Ne devrait être true que pour le lancement du dernier processus de migration
	 */
	public boolean forceIndexationAtEnd;
	/**
	 * Si vrai, la base est vidée avant la migration dans tous les cas.
	 */
	public boolean forceTruncateDatabase;

	private ValidationInterceptor validationInterceptor;

	public void initialize() throws Exception {

		if (db2Connection == null) {
			db2Connection = db2DataSource.getConnection();
			db2Template = new JdbcTemplate(db2DataSource);
		}
		if (isConnection == null) {
			isConnection = isDataSource.getConnection();
			isTemplate = new JdbcTemplate(isDataSource);
		}
		if (isScopeConnection == null) {
			isScopeConnection = isScopeDataSource.getConnection();
			isScopeTemplate = new JdbcTemplate(isScopeDataSource);
		}
		if (orclConnection == null) {
			orclConnection = dataSource.getConnection();
			orclTemplate = new JdbcTemplate(dataSource);
		}
		if (empConnection == null) {
			empConnection = empDataSource.getConnection();
			empTemplate = new JdbcTemplate(empDataSource);
		}

		asyncIndexer = new AsyncTiersIndexer(globalTiersIndexer, transactionManager, sessionFactory, 4, nbrInsertBeforeCommit, Mode.FULL);
		asyncIndexer.initialize();

		//Permet de définir que les attributs de logs (date et user de création et modification)
		//seront sauvés avec les valeurs importées s'il y en a.
		modificationLogInterceptor.setCompleteOnly(true);

		listContribuableFree = new ArrayList<Integer>();
	}
	public void terminate() throws Exception {

		if (globalTiersIndexer != null) {
			globalTiersIndexer.setOnTheFlyIndexation(true);
		}

		if (oidInterceptor != null) {
			oidInterceptor.setEnabled(true);
		}

		if (db2Connection != null) {
			db2Connection.close();
			db2Connection = null;
		}
		if (isConnection != null) {
			isConnection.close();
			isConnection = null;
		}
		if (isScopeConnection != null) {
			isScopeConnection.close();
			isScopeConnection = null;
		}
		if (orclConnection != null) {
			orclConnection.close();
			orclConnection = null;
		}

		if (asyncIndexer != null) {
			asyncIndexer.setEnabled(true);
			asyncIndexer.terminate();
			asyncIndexer = null;
		}

		if (empConnection != null) {
			empConnection.close();
			empConnection = null;
		}

		if (asyncIndexer != null) {
			asyncIndexer.terminate();
		}
	}

	public String getTableIs(String name) {
		return isSchema+"."+isTablePrefix+name;
	}
	public String getTableIsScope(String name) {
		return isScopeSchema+"."+isScopeTablePrefix+name;
	}
	public String getTableDb2(String name){
		return db2Schema+"."+name;
	}
	public String getTableOrcl(String name){
		return name;
	}

	public String getSqlDateIsNull(boolean notEqual) {
		if (isDbType.equals("DB2")) {
			if (!notEqual) {
				return "='01.01.0001'";
			}
			return "<>'01.01.0001'";
		}
		if (!notEqual) {
			return "IS NULL";
		}
		return "IS NOT NULL";
	}

	public synchronized void setNbrInsertBeforeCommit(int nbrInsertBeforeCommit) {
		this.nbrInsertBeforeCommit = nbrInsertBeforeCommit;
	}

	public synchronized void setErrorsprocessing(boolean errorsProcessing) {
		this.errorsProcessing = errorsProcessing;
	}

	public synchronized void setForceIndexationAtEnd(boolean indexation) {
		this.forceIndexationAtEnd = indexation;
	}

	public synchronized void setDateLimiteRepriseDataSourcier(String dateLimiteRepriseDataSourcier) {
		this.dateLimiteRepriseDataSourcier = RegDate.get(DateHelper.displayStringToDate(dateLimiteRepriseDataSourcier));
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	public RegDate transformDateDb2(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		if (cal.get(Calendar.YEAR) == 1 && cal.get(Calendar.MONTH) == 0 && cal.get(Calendar.DAY_OF_MONTH) == 1) {
			return null;
		}
		return RegDate.get(date);
	}

	public String formatDiffTime(long end, long begin) {
		long diff = end-begin;
		float value = diff / 1000000.0f;

		String str = String.format("%.2f", value);
		return str;
	}

	/**
	 * Note: chaque thread doit appeler cette méthode !
	 */
	void setIndexationMode(IndexationMode mode) {

		switch (mode) {
		case NONE:
			globalTiersIndexer.setOnTheFlyIndexation(false);
			oidInterceptor.setEnabled(false);
			asyncIndexer.setEnabled(false);
			LOGGER.info("AUCUNE indexation pendant la migration");
			break;
		case ON_THE_FLY:
			globalTiersIndexer.setOnTheFlyIndexation(true);
			oidInterceptor.setEnabled(true);
			asyncIndexer.setEnabled(false);
			LOGGER.info("L'indexation pendant la migration se fera ON_THE_FLY");
			break;
		case ASYNC:
			globalTiersIndexer.setOnTheFlyIndexation(false);
			oidInterceptor.setEnabled(false);
			asyncIndexer.setEnabled(true);
			LOGGER.info("L'indexation pendant la migration se fera ASYNC");
			break;
		case AT_END:
			globalTiersIndexer.setOnTheFlyIndexation(false);
			oidInterceptor.setEnabled(false);
			asyncIndexer.setEnabled(false);
			LOGGER.info("L'indexation pendant la migration se fera AT_END");
			break;
		}
	}


	public void loadRapprochementpmEmployeur() throws Exception {
		mapDebiteurPm = new HashMap<Integer, Long>();
		File file = new File(fichierRapprochement);
		if (!file.exists()) {
			throw new FileNotFoundException("Le fichier '" + fichierRapprochement + "' n'existe pas.");
		}
		if (!file.canRead()) {
			throw new FileNotFoundException("Le fichier '" + fichierRapprochement + "' n'est pas lisible.");
		}

		// on parse le fichier
		Scanner s = new Scanner(file);
		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();

				if (!line.trim().equals("")) {
					String[] tokens = line.split(";");

					// on a un numero de PM
					if (!"".equals(tokens[0])) {
						Integer numeroDebiteur = Integer.valueOf(tokens[1]);
						Long numeroPM = Long.valueOf(tokens[0]);
						mapDebiteurPm.put(numeroDebiteur, numeroPM);
					}
				}

			}
		}
		finally {
			s.close();
		}
	}

	/**
	 * Note: chaque thread doit appeler cette méthode !
	 * <p>
	 * <b>A N'UTILISER QUE LORS DE LA DERNIERE PHASE DE MIGRATION, lorsque tous les algorithmes possibles ont été épuisés pour rendre
	 * valides les tiers</b>.
	 * <p>
	 * Un tiers migrés comme non-valide devra être corrigé après-coup dans l'interface, car il ne peut pas être processé par Unireg.
	 */
	public void disableValidation() {
		validationInterceptor.setEnabled(false);
	}
}
