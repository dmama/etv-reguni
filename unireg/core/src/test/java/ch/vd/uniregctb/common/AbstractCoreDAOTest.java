package ch.vd.uniregctb.common;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.CachedResultSetTableFactory;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.IResultSetTableFactory;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.csv.CsvProducer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.FlatDtdProducer;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.dataset.xml.XmlDataSetWriter;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.junit.Assert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;
import org.xml.sax.InputSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Test case abstrait permettant de tester les DAO Spring.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		CoreTestingConstants.UNIREG_CORE_DAO,
		CoreTestingConstants.UNIREG_CORE_SF,
		CoreTestingConstants.UNIREG_CORE_UT_DATASOURCE,
		CoreTestingConstants.UNIREG_CORE_UT_PROPERTIES
})
public abstract class AbstractCoreDAOTest extends AbstractSpringTest {

	static {
		try {
			Log4jConfigurer.initLogging(getLog4jConfigFilename().toString());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fichier de configuration par défaut de log4j.
	 */
	private static final String DEFAULT_LOG4J_CONFIGURATION_FILENAME = "classpath:ut/log4j.xml";

	protected DataSource dataSource;
	protected SimpleJdbcTemplate simpleJdbcTemplate;
	protected LocalSessionFactoryBean localSessionFactoryBean;
	protected HibernateTemplate hibernateTemplate;
	protected Dialect dialect;
	protected TiersDAO tiersDAO;

	public static enum ProducerType {
		Flat,
		Cvs,
		Xml,
		Dtd
	}
	
	private ProducerType producerType = ProducerType.Xml;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		localSessionFactoryBean = getBean(LocalSessionFactoryBean.class, "&sessionFactory");
		setDataSource(getBean(DataSource.class, "dataSource"));
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		dialect = getBean(Dialect.class, "hibernateDialect");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		truncateDatabase();
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	/**
	 * @return the producerType
	 */
	public ProducerType getProducerType() {
		return producerType;
	}

	/**
	 * @param producerType
	 *            the producerType to set
	 */
	public void setProducerType(ProducerType producerType) {
		this.producerType = producerType;
	}

	/**
	 * virtual method to truncate the database
	 */
	protected void truncateDatabase() throws Exception {
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				deleteFromTables(getTableNames(false));
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public String[] getTableNames(boolean reverse) {
		ArrayList<String> t = new ArrayList<String>();
		Iterator<Table> tables = localSessionFactoryBean.getConfiguration().getTableMappings();
		while (tables.hasNext()) {
			Table table = tables.next();
			if (table.isPhysicalTable()) {
				addTableName(t, table);
			}
		}
		if (reverse) {
			Collections.reverse(t);
		}
		return t.toArray(new String[t.size()]);
	}

	/**
	 * Charge le fichier DbUnit après avoir vidé la base de données
	 */
	protected void loadDatabase(final String filename) throws Exception {

		File file = null;

		// Essaie d'abord tel-quel
		try {
			file = ResourceUtils.getFile(filename);
		}
		catch (Exception ignored) {
			// La variable file est nulle, ca nous suffit
		}

		// Ensuite avec classpath: devant
		if (file == null || !file.exists()) {
			try {
				String name = "classpath:"+filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		// Ensuite avec classpath: et le chemin du package devant
		if (file == null || !file.exists()) {
			try {
				String packageName = getClass().getPackage().getName();
				packageName = packageName.replace('.', '/');

				String name = "classpath:"+packageName+"/"+filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}
		loadDataSet(file);
	}

	/**
	 * Charge un fichier Dbunit dans la database préalablement vidée, le fichier doit être du format {@link #getProducerType()}.
	 *
	 * @param file
	 *            le fichier à utiliser
	 */
	private void loadDataSet(final File file) throws Exception {

		doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

				//Assert.assertTrue(file != null && file.exists());

				// initialize your database connection here
				Connection sql = DataSourceUtils.getConnection(dataSource);
				// initialize your dataset here
				try {
					IDatabaseConnection connection = createNewConnection(sql);
					IDataSet dataSet = getSrcDataSet(file, getProducerType(), false);
					DatabaseOperation.INSERT.execute(connection, dataSet);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					DataSourceUtils.releaseConnection(sql, dataSource);
				}
				return null;
			}
		});

	}

	/**
	 * Permet d'extraire les données de la db pour un fichier DBUnit de type {@link #getProducerType()}
	 * @param filename le nom du fichier
	 * @throws Exception exception
	 */
	public void dumpDatabase(String filename) throws Exception {
		dumpDatabase(new FileWriter(filename));
	}

	/**
	 * Permet d'extraire les données de la db pour un fichier DBUnit de type {@link #getProducerType()}
	 * @param writer writer
	 * @throws Exception exception
	 */
	public void dumpDatabase(Writer writer) throws Exception {
		// full database export
		Connection dsCon = DataSourceUtils.getConnection( dataSource);
		try {
			DatabaseConnection connection = createNewConnection(dsCon);
			QueryDataSet dataSet = new QueryDataSet(connection);
			for (String tablename : getTableNames(false)) {
				dataSet.addTable(tablename);
			}
			XmlDataSetWriter dataSetWriter = new XmlDataSetWriter(writer, "UTF-8");
			dataSetWriter.write(dataSet);

		}
		finally {
			DataSourceUtils.releaseConnection(dsCon, dataSource);
		}
	}

	protected static URL getLog4jConfigFilename() throws Exception {
		return ResourceUtils.getURL(DEFAULT_LOG4J_CONFIGURATION_FILENAME);
	}

	protected static DatabaseConnection createNewConnection(Connection connection) {
		final DatabaseConnection dbConnection = new DatabaseConnection(connection);
		final DatabaseConfig config = dbConnection.getConfig();
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());
		return dbConnection;
	}

	/**
	 * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
	 *
	 * @param names
	 *            the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 */
	protected int deleteFromTables(String... names) {
		return SimpleJdbcTestUtils.deleteFromTables(this.simpleJdbcTemplate, names);
	}

	/**
	 *
	 * @param tables
	 * @param table
	 */
	@SuppressWarnings("unchecked")
	private void addTableName(ArrayList<String> tables, Table table) {
		if (tables.contains(table.getName())) {
			return;
		}
		Iterator<Table> ts = localSessionFactoryBean.getConfiguration().getTableMappings();
		while ( ts.hasNext()) {
			Table t = ts.next();
			if ( t.equals(table)){
				continue;
			}
			Iterator<ForeignKey> relationships = t.getForeignKeyIterator();
			while (relationships.hasNext()) {
				ForeignKey fk = relationships.next();
				if (fk.getReferencedTable().equals(table)) {
					addTableName(tables, fk.getTable());
				}
			}
		}
		tables.add(table.getName());

	}



	/**
	 *
	 * @param connection
	 * @param tables
	 * @param forwardonly
	 * @return
	 * @throws DatabaseUnitException
	 */
	protected IDataSet getDatabaseDataSet(IDatabaseConnection connection, String[] tables, boolean forwardonly)
			throws DatabaseUnitException {
		if (logger.isDebugEnabled()) {
			logger.debug("getDatabaseDataSet(connection=" + connection + ", tables=" + Arrays.toString(tables) + ", forwardonly=" + forwardonly
					+ ") - start");
		}

		try {
			// Setup the ResultSet table factory
			IResultSetTableFactory factory;
			if (forwardonly) {
				factory = new ForwardOnlyResultSetTableFactory();
			}
			else {
				factory = new CachedResultSetTableFactory();
			}
			DatabaseConfig config = connection.getConfig();
			config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, factory);

			// Retrieve the complete database if no tables or queries specified.
			if (tables == null || tables.length == 0) {
				return connection.createDataSet();
			}

			List<QueryDataSet> queryDataSets = new ArrayList<QueryDataSet>();

			QueryDataSet queryDataSet = new QueryDataSet(connection);

			for (String item : tables) {
				queryDataSet.addTable(item);
			}

			if (queryDataSet.getTableNames().length > 0)
				queryDataSets.add(queryDataSet);

			IDataSet[] dataSetsArray = new IDataSet[queryDataSets.size()];
			return new CompositeDataSet(queryDataSets.toArray(dataSetsArray));
		}
		catch (SQLException e) {
			logger.error("getDatabaseDataSet()", e);

			throw new DatabaseUnitException(e);
		}
	}

	/**
	 *
	 * @param src
	 * @param format
	 * @param forwardonly
	 * @return
	 * @throws DatabaseUnitException
	 */
	protected IDataSet getSrcDataSet(File src, ProducerType format, boolean forwardonly) throws DatabaseUnitException {
		logger.debug("getSrcDataSet(src=" + src + ", format=" + format + ", forwardonly=" + forwardonly + ") - start");

		try {
			IDataSetProducer producer;
			if (format == ProducerType.Xml) {
				producer = new XmlProducer(new InputSource(src.toURL().toString()));
			}
			else if (format == ProducerType.Cvs) {
				producer = new CsvProducer(src);
			}
			else if (format == ProducerType.Flat) {
				producer = new FlatXmlProducer(new InputSource(src.toURL().toString()));
			}
			else if (format == ProducerType.Dtd) {
				producer = new FlatDtdProducer(new InputSource(src.toURL().toString()));
			}
			else {
				throw new IllegalArgumentException("Type must be either 'flat'(default), 'xml', 'csv' or 'dtd' but was: " + format);
			}

			if (forwardonly) {
				return new StreamingDataSet(producer);
			}
			return new CachedDataSet(producer);
		}
		catch (IOException e) {
			logger.error("getSrcDataSet()", e);

			throw new DatabaseUnitException(e);
		}
	}

	protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, TypeAutoriteFiscale type, int noOfs,
			MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipal forPrincipal) {
		assertNotNull(forPrincipal);
		assertEquals(debut, forPrincipal.getDateDebut());
		assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
		assertNull(forPrincipal.getDateFin());
		assertNull(forPrincipal.getMotifFermeture());
		assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forPrincipal.getMotifRattachement());
		assertEquals(modeImposition, forPrincipal.getModeImposition());
	}

	protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture,
			TypeAutoriteFiscale type, int noOfs, MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipal forPrincipal) {
		assertNotNull(forPrincipal);
		assertEquals(debut, forPrincipal.getDateDebut());
		assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
		assertEquals(fin, forPrincipal.getDateFin());
		assertEquals(motifFermeture, forPrincipal.getMotifFermeture());
		assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forPrincipal.getMotifRattachement());
		assertEquals(modeImposition, forPrincipal.getModeImposition());
	}

	protected static void assertForSecondaire(RegDate debut, MotifFor motifOuverture, TypeAutoriteFiscale type, int noOfs,
			MotifRattachement motif, ForFiscalSecondaire forSecondaire) {
		assertNotNull(forSecondaire);
		assertEquals(debut, forSecondaire.getDateDebut());
		assertEquals(motifOuverture, forSecondaire.getMotifOuverture());
		assertNull(forSecondaire.getDateFin());
		assertNull(forSecondaire.getMotifFermeture());
		assertEquals(type, forSecondaire.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forSecondaire.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forSecondaire.getMotifRattachement());
	}

	protected static void assertForSecondaire(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture,
			TypeAutoriteFiscale type, int noOfs, MotifRattachement motif, ForFiscalSecondaire forSecondaire) {
		assertNotNull(forSecondaire);
		assertEquals(debut, forSecondaire.getDateDebut());
		assertEquals(motifOuverture, forSecondaire.getMotifOuverture());
		assertEquals(fin, forSecondaire.getDateFin());
		assertEquals(motifFermeture, forSecondaire.getMotifFermeture());
		assertEquals(type, forSecondaire.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forSecondaire.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forSecondaire.getMotifRattachement());
	}

	protected static void assertForDebiteur(RegDate debut, TypeAutoriteFiscale taf, int noOFS, ForDebiteurPrestationImposable forFiscal) {
		assertForDebiteur(debut, null, taf, noOFS, forFiscal);
	}

	protected static void assertForDebiteur(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, int noOFS, ForDebiteurPrestationImposable forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(noOFS, forFiscal.getNumeroOfsAutoriteFiscale().intValue());
	}

	/**
	 * Asserte qu'il y a une bien une (et une seule) déclaration d'impôt dans la collection passée en paramètre, et qu'elle possède bien les valeurs spécifiées.
	 *
	 * @param debut              la date de début de la déclaration
	 * @param fin                la date de fin de la déclaration
	 * @param etat               l'état courant de la déclaration
	 * @param typeContribuable   le type de contribuable de la déclaration
	 * @param typeDocument       le type de document de la déclaration
	 * @param idCollRetour       l'id (pas le numéro) de la collectivité administrative (CEDI/ACI) de l'adresse de retour de la déclaration
	 * @param dateRetourImprimee le délai de retour imprimé sur la déclaration
	 * @param declarations       la collection de déclarations à asserter.
	 */
	protected static void assertDI(RegDate debut, RegDate fin, TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                               TypeDocument typeDocument, Long idCollRetour, RegDate dateRetourImprimee, List<Declaration> declarations) {
		assertNotNull(declarations);
		assertEquals(declarations.size(), 1);
		assertDI(debut, fin, etat, typeContribuable, typeDocument, idCollRetour, dateRetourImprimee, declarations.get(0));
	}

	/**
	 * Asserte que la déclaration d'impôt passée en paramètre possède bien les valeurs spécifiées.
	 *
	 * @param debut              la date de début de la déclaration
	 * @param fin                la date de fin de la déclaration
	 * @param etat               l'état courant de la déclaration
	 * @param typeContribuable   le type de contribuable de la déclaration
	 * @param typeDocument       le type de document de la déclaration
	 * @param idCollRetour       l'id (pas le numéro) de la collectivité administrative (CEDI/ACI) de l'adresse de retour de la déclaration
	 * @param delaiRetourImprime le délai de retour imprimé sur la déclaration
	 * @param declaration        la déclaration à asserter.
	 */
	protected static void assertDI(RegDate debut, RegDate fin, TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                               TypeDocument typeDocument, Long idCollRetour, RegDate delaiRetourImprime, Declaration declaration) {
		assertNotNull(declaration);
		DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
		assertEquals(debut, di.getDateDebut());
		assertEquals(fin, di.getDateFin());
		final EtatDeclaration e = di.getDernierEtat();
		assertEquals(etat, (e == null ? null : e.getEtat()));
		assertEquals(typeContribuable, di.getTypeContribuable());
		assertEquals(typeDocument, di.getModeleDocument().getTypeDocument());
		assertEquals(idCollRetour, di.getRetourCollectiviteAdministrativeId());
		assertEquals(delaiRetourImprime, di.getDelaiRetourImprime());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, CollectiviteAdministrative collectivite, TacheEnvoiDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDateDebut());
		assertEquals(dateFin, tache.getDateFin());
		assertEquals(typeCtb, tache.getTypeContribuable());
		assertEquals(typeDoc, tache.getTypeDocument());
		assertEquals(adresseRetour, tache.getAdresseRetour());
		if(collectivite!=null){
			assertEquals(collectivite, tache.getCollectiviteAdministrativeAssignee());
		}

	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, TacheEnvoiDeclarationImpot tache) {
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, adresseRetour,null, tache);

	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
			TacheAnnulationDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDeclarationImpotOrdinaire().getDateDebut());
		assertEquals(dateFin, tache.getDeclarationImpotOrdinaire().getDateFin());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheNouveauDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheControleDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheTransmissionDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la collection passée en paramètre possède bien une seule tâche et que celle-ci possède bien les valeurs spécifiées.
	 */
	protected static void assertOneTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                     TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, List<TacheEnvoiDeclarationImpot> taches) {
		assertNotNull(taches);
		assertEquals(1, taches.size());
		final TacheEnvoiDeclarationImpot tache = taches.get(0);
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, adresseRetour, tache);
	}

	protected static void assertSituation(RegDate debut, RegDate fin, int nombreEnfants, TarifImpotSource tarif,
			SituationFamilleMenageCommun situation) {
		assertNotNull(situation);
		assertEquals(debut, situation.getDateDebut());
		assertEquals(fin, situation.getDateFin());
		assertEquals(nombreEnfants, situation.getNombreEnfants());
		assertEquals(tarif, situation.getTarifApplicable());
	}

	/**
	 * Ajoute une période fiscale dans la base de données (avec les délais usuels)
	 */
	protected PeriodeFiscale addPeriodeFiscale(int annee) {
		PeriodeFiscale periode = new PeriodeFiscale();
		periode.setAnnee(annee);
		periode.setAllPeriodeFiscaleParametres(date(annee + 1, 1, 31), date(annee + 1, 3, 31), date(annee + 1, 6, 30));
		return (PeriodeFiscale) hibernateTemplate.merge(periode);
	}

	/**
	 * Ajoute un nouveau type de document dans la base de données
	 */
	protected ModeleDocument addModeleDocument(TypeDocument type, PeriodeFiscale periode) {
		assertNotNull(type);
		assertNotNull(periode);
		ModeleDocument doc = new ModeleDocument();
		doc.setTypeDocument(type);
		doc.setModelesFeuilleDocument(new HashSet<ModeleFeuilleDocument>());
		doc = (ModeleDocument) hibernateTemplate.merge(doc);
		periode.addModeleDocument(doc);
		return doc;
	}

	/**
	 * Ajoute un nouveau modele de feuille dans la base de données
	 */
	protected ModeleFeuilleDocument addModeleFeuilleDocument(String intitule, String numero, ModeleDocument modeleDoc) {
		assertNotNull(intitule);
		assertNotNull(numero);
		ModeleFeuilleDocument feuille = new ModeleFeuilleDocument();
		feuille.setNumeroFormulaire(numero);
		feuille.setIntituleFeuille(intitule);
		feuille.setModeleDocument(modeleDoc);
		feuille = (ModeleFeuilleDocument) hibernateTemplate.merge(feuille);
		return feuille;
	}

	/**
	 * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
	 */
	protected DeclarationImpotOrdinaire addDeclarationImpot(Contribuable tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                        CollectiviteAdministrative retourCollectiviteAdministrative, TypeContribuable typeC, ModeleDocument modele) {

		DeclarationImpotOrdinaire d = new DeclarationImpotOrdinaire();
		d.setPeriode(periode);
		d.setDateDebut(debut);
		d.setDateFin(fin);
		d.setTypeContribuable(typeC);
		d.setModeleDocument(modele);
		d.setRetourCollectiviteAdministrativeId(retourCollectiviteAdministrative == null ? null : retourCollectiviteAdministrative.getId());

		int numero = 0;
		final int annee = periode.getAnnee();
		Set<Declaration> decls = tiers.getDeclarations();
		if (decls != null) {
			for (Declaration dd : decls) {
				if (dd.getPeriode().getAnnee() == annee) {
					++numero;
				}
			}
		}
		d.setNumero(numero + 1);

		d.setTiers(tiers);
		d = (DeclarationImpotOrdinaire) hibernateTemplate.merge(d);

		tiers.addDeclaration(d);
		return d;
	}

	/**
	 * Ajoute une tâche d'envoi de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheEnvoiDeclarationImpot addTacheEnvoiDI(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                                     Contribuable contribuable, Qualification qualification, CollectiviteAdministrative colAdm) {
		TacheEnvoiDeclarationImpot tache =
				new TacheEnvoiDeclarationImpot(etat, dateEcheance, contribuable, dateDebut, dateFin, typeContribuable, typeDocument, qualification, TypeAdresseRetour.CEDI, colAdm);
		tache = (TacheEnvoiDeclarationImpot) hibernateTemplate.merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheAnnulationDeclarationImpot addTacheAnnulDI(TypeEtatTache etat, RegDate dateEcheance, DeclarationImpotOrdinaire declaration, Contribuable contribuable,
	                                                          CollectiviteAdministrative colAdm) {
		TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(etat, dateEcheance, contribuable, declaration, colAdm);
		tache = (TacheAnnulationDeclarationImpot) hibernateTemplate.merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheControleDossier addTacheControleDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative colAdm) {
		TacheControleDossier tache = new TacheControleDossier(etat, dateEcheance, contribuable, colAdm);
		tache = (TacheControleDossier) hibernateTemplate.merge(tache);
		return tache;
	}

	protected TacheTransmissionDossier addTacheTransmission(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheTransmissionDossier transmission = new TacheTransmissionDossier(etat, date(2010, 1, 1), ctb, ca);
		transmission = (TacheTransmissionDossier) hibernateTemplate.merge(transmission);
		return transmission;
	}

	protected TacheNouveauDossier addTacheNouveau(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheNouveauDossier nouveau = new TacheNouveauDossier(etat, date(2010, 1, 1), ctb, ca);
		nouveau = (TacheNouveauDossier) hibernateTemplate.merge(nouveau);
		return nouveau;
	}

	protected TacheControleDossier addTacheControle(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheControleDossier controle = new TacheControleDossier(etat, date(2010, 1, 1), ctb, ca);
		controle = (TacheControleDossier) hibernateTemplate.merge(controle);
		return controle;
	}

	protected PersonnePhysique addHabitant(long noIndividu) {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(noIndividu);
		return (PersonnePhysique) hibernateTemplate.merge(hab);
	}

	protected PersonnePhysique addHabitant(long noTiers, long noIndividu) {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(noTiers);
		hab.setNumeroIndividu(noIndividu);
		return (PersonnePhysique) hibernateTemplate.merge(hab);
	}

	/**
	 * Crée et ajoute dans la base de données un non-habitant minimal.
	 */
	protected PersonnePhysique addNonHabitant(String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		return addNonHabitant(null, prenom, nom, dateNaissance, sexe);
	}

	protected PersonnePhysique addNonHabitant(Long noTiers, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNumero(noTiers);
		nh.setPrenom(prenom);
		nh.setNom(nom);
		nh.setDateNaissance(dateNaissance);
		nh.setSexe(sexe);
		return (PersonnePhysique) hibernateTemplate.merge(nh);
	}

	/**
	 * Crée et ajoute dans la base de donnée un rapport d'appartenance ménage entre un ménage commun et une personne physique
	 */
	protected AppartenanceMenage addAppartenanceMenage(MenageCommun menage, PersonnePhysique pp, RegDate dateDebut, RegDate dateFin, boolean annule) {
		AppartenanceMenage rapport = new AppartenanceMenage();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setObjet(menage);
		rapport.setSujet(pp);
		rapport.setAnnule(annule);
		rapport = (AppartenanceMenage) hibernateTemplate.merge(rapport);

		menage.addRapportObjet(rapport);
		pp.addRapportSujet(rapport);

		return rapport;
	}

	/**
	 * Crée et ajoute dans la base de données un menage-commun.
	 */
	protected EnsembleTiersCouple addEnsembleTiersCouple(PersonnePhysique principal, PersonnePhysique conjoint, RegDate dateMariage, RegDate dateFin) {
		return addEnsembleTiersCouple(null, principal, conjoint, dateMariage, dateFin);
	}

	/**
	 * Crée et ajoute dans la base de données un menage-commun.
	 */
	protected EnsembleTiersCouple addEnsembleTiersCouple(Long noTiers, PersonnePhysique principal, PersonnePhysique conjoint, RegDate dateMariage, RegDate dateFin) {

		MenageCommun menage = new MenageCommun();
		menage.setNumero(noTiers);
		menage = (MenageCommun) hibernateTemplate.merge(menage);
		principal = (PersonnePhysique) hibernateTemplate.merge(principal);
		if (conjoint != null) {
			conjoint = (PersonnePhysique) hibernateTemplate.merge(conjoint);
		}

		addAppartenanceMenage(menage, principal, dateMariage, dateFin, false);
		if (conjoint != null) {
			addAppartenanceMenage(menage, conjoint, dateMariage, dateFin, false);
		}

		final EnsembleTiersCouple ensemble = new EnsembleTiersCouple();
		ensemble.setMenage(menage);
		ensemble.setPrincipal(principal);
		ensemble.setConjoint(conjoint);

		return ensemble;
	}

	protected Tutelle addTutelle(PersonnePhysique pupille, Tiers tuteur, Tiers autoriteTutelaire, RegDate dateDebut, RegDate dateFin) {
		Tutelle rapport = new Tutelle();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setObjet(tuteur);
		rapport.setSujet(pupille);
		rapport.setAutoriteTutelaire(autoriteTutelaire);
		rapport = (Tutelle) hibernateTemplate.merge(rapport);
		tuteur.addRapportObjet(rapport);
		pupille.addRapportSujet(rapport);
		return rapport;
	}

	protected Curatelle addCuratelle(PersonnePhysique pupille, Tiers curateur, RegDate dateDebut, RegDate dateFin) {
		Curatelle rapport = new Curatelle();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setObjet(curateur);
		rapport.setSujet(pupille);
		rapport = (Curatelle) hibernateTemplate.merge(rapport);
		curateur.addRapportObjet(rapport);
		pupille.addRapportSujet(rapport);
		return rapport;
	}

	protected RepresentationConventionnelle addRepresentationConventionnelle(Tiers represente, Tiers representant, RegDate dateDebut, boolean extensionExecutionForcee) {
		RepresentationConventionnelle rapport = new RepresentationConventionnelle();
		rapport.setDateDebut(dateDebut);
		rapport.setObjet(representant);
		rapport.setSujet(represente);
		rapport.setExtensionExecutionForcee(extensionExecutionForcee);
		rapport = (RepresentationConventionnelle) hibernateTemplate.merge(rapport);
		representant.addRapportObjet(rapport);
		represente.addRapportSujet(rapport);
		return rapport;
	}

	protected ConseilLegal addConseilLegal(Tiers pupille, Tiers conseiller, RegDate dateDebut) {
		ConseilLegal rapport = new ConseilLegal();
		rapport.setDateDebut(dateDebut);
		rapport.setObjet(conseiller);
		rapport.setSujet(pupille);
		rapport = (ConseilLegal) hibernateTemplate.merge(rapport);
		conseiller.addRapportObjet(rapport);
		pupille.addRapportSujet(rapport);
		return rapport;
	}

	protected AutreCommunaute addAutreCommunaute(String nom) {
		final AutreCommunaute communaute = new AutreCommunaute();
		communaute.setNom(nom);
		return (AutreCommunaute) hibernateTemplate.merge(communaute);
	}

	protected DebiteurPrestationImposable addDebiteur() {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi);
		return dpi;
	}

	protected DebiteurPrestationImposable addDebiteur(String complementNom, Contribuable ctbLie, RegDate dateDebutContact) {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setComplementNom(complementNom);
		dpi = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi);

		ContactImpotSource rapport = new ContactImpotSource(dateDebutContact, null, ctbLie, dpi);
		rapport = (ContactImpotSource) hibernateTemplate.merge(rapport);

		dpi.addRapportObjet(rapport);
		ctbLie.addRapportSujet(rapport);

		return dpi;
	}

	protected Entreprise addEntreprise(Long numeroEntreprise) {
		Entreprise ent = new Entreprise();
		ent.setNumero(numeroEntreprise);
		ent.setNumeroEntreprise(numeroEntreprise);
		ent = (Entreprise) hibernateTemplate.merge(ent);
		return ent;
	}

	protected CollectiviteAdministrative addCollAdm(int numero) {
		CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(numero);
		ca = (CollectiviteAdministrative) hibernateTemplate.merge(ca);
		return ca;
	}

	protected void addEtatDeclaration(Declaration declaration, RegDate dateObtention, TypeEtatDeclaration type) {
		EtatDeclaration etat = new EtatDeclaration();
		etat.setDateObtention(dateObtention);
		etat.setEtat(type);
		declaration.addEtat(etat);
		hibernateTemplate.merge(declaration);
	}

	protected void addDelaiDeclaration(Declaration declaration, RegDate dateTraitement, RegDate delaiAccordeAu) {
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(dateTraitement);
		delai.setDateDemande(dateTraitement);
		delai.setDelaiAccordeAu(delaiAccordeAu);
		declaration.addDelai(delai);
		hibernateTemplate.merge(declaration);
	}

	/**
	 * Ajoute un droit d'accès (autorisation ou interdiction) entre un opérateur et un tiers.
	 */
	protected DroitAcces addDroitAcces(long noIndOperateur, PersonnePhysique pp, TypeDroitAcces type, Niveau niveau, RegDate debut,
			RegDate fin) {

		DroitAcces da = new DroitAcces();
		da.setDateDebut(debut);
		da.setDateFin(fin);
		da.setNoIndividuOperateur(noIndOperateur);
		da.setType(type);
		da.setNiveau(niveau);
		da.setTiers(pp);

		da = (DroitAcces) hibernateTemplate.merge(da);
		return da;
	}

	/**
	 * Ajoute un for principal sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS, TypeAutoriteFiscale type, MotifRattachement motif) {
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f.setModeImposition(ModeImposition.ORDINAIRE);
		f = tiersDAO.addAndSave(contribuable, f);
		return f;
	}


		/**
	 * Ajoute un for principal Source sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipalSource(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS) {
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.SOURCE);
		f = tiersDAO.addAndSave(contribuable, f);
		return f;
		}

	/**
	 * Ajoute un for fiscal secondaire ouvert.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, Integer noOFS,
			MotifRattachement motif) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f = tiersDAO.addAndSave(tiers, f);
		return f;
	}

	/**
	 * Ajoute un for fiscal secondaire fermé.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS, MotifRattachement motif) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f = tiersDAO.addAndSave(tiers, f);
		return f;
	}

	protected ForFiscalAutreImpot addForAutreImpot(Contribuable tiers, RegDate ouverture, RegDate fermeture, Integer noOFS,
			TypeAutoriteFiscale type, GenreImpot genre) {
		ForFiscalAutreImpot f = new ForFiscalAutreImpot();
		f.setDateDebut(ouverture);
		f.setDateFin(fermeture);
		f.setGenreImpot(genre);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f = tiersDAO.addAndSave(tiers, f);
		return f;
	}

	/**
	 * Raccourci pour créer une RegDate.
	 */
	protected static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
