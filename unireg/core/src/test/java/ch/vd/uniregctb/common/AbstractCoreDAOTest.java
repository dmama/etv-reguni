package ch.vd.uniregctb.common;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CachedDataSet;
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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;
import org.xml.sax.InputSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.hibernate.config.DescriptiveSessionFactoryBean;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationAvecNumeroSequence;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.EtatDeclarationSuspendue;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.uniregctb.evenement.ide.ReferenceAnnonceIDEDAO;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.rf.Proprietaire;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.ScissionEntreprise;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TransfertPatrimoine;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.ModeleFeuille;
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
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeLettreBienvenue;
import ch.vd.uniregctb.type.TypeRapprochementRF;
import ch.vd.uniregctb.type.TypeTiersEtiquette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

	protected DataSource dataSource;
	protected JdbcTemplate jdbcTemplate;
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	protected DescriptiveSessionFactoryBean sessionFactoryBean;
	protected ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	protected HibernateTemplate hibernateTemplate;
	protected Dialect dialect;
	protected TiersDAO tiersDAO;
	protected SessionFactory sessionFactory;

	public enum ProducerType {
		Flat,
		Cvs,
		Xml,
		Dtd
	}

	private ProducerType producerType = ProducerType.Xml;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		sessionFactoryBean = getBean(DescriptiveSessionFactoryBean.class, "&sessionFactory");
		setDataSource(getBean(DataSource.class, "dataSource"));
		dialect = getBean(Dialect.class, "hibernateDialect");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		referenceAnnonceIDEDAO = getBean(ReferenceAnnonceIDEDAO.class, "referenceAnnonceIDEDAO");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");

		truncateDatabase();
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
	 * @return the current Hibernate session
	 */
	protected final Session getCurrentSession() {
		return sessionFactoryBean.getObject().getCurrentSession();
	}

	protected final <T extends HibernateEntity> T merge(T entity) {
		return hibernateTemplate.merge(entity);
	}

	/**
	 * virtual method to truncate the database
	 */
	protected void truncateDatabase() throws Exception {
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				deleteFromTables(getTableNames(false));
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public String[] getTableNames(boolean reverse) {
		ArrayList<String> t = new ArrayList<>();
		Iterator<Table> tables = sessionFactoryBean.getConfiguration().getTableMappings();
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

		File file = getFile(filename);
		loadDataSet(file);
	}

	/**
	 * Retourne le fichier spécifié par son nom en cherchant : <ul> <li>dans les ressources</li> <li>dans le classpath</li> <li>dans le package</li> </ul>
	 *
	 * @param filename un nom de fichier
	 * @return un fichier; ou <b>null</b> si le fichier n'a pas été trouvé
	 */
	protected File getFile(String filename) {
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
				String name = "classpath:" + filename;
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

				String name = "classpath:" + packageName + '/' + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		if (!file.exists()) {
			return null;
		}

		return file;
	}

	/**
	 * Charge un fichier Dbunit dans la database préalablement vidée, le fichier doit être du format {@link #getProducerType()}.
	 *
	 * @param file
	 *            le fichier à utiliser
	 */
	private void loadDataSet(final File file) throws Exception {

		doInNewTransaction(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

				try (Connection sql = DataSourceUtils.getConnection(dataSource)) {
					IDatabaseConnection connection = createNewConnection(sql);
					IDataSet dataSet = getSrcDataSet(file, getProducerType(), false);
					DatabaseOperation.INSERT.execute(connection, dataSet);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
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
		return JdbcTestUtils.deleteFromTables(jdbcTemplate, names);
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
		Iterator<Table> ts = sessionFactoryBean.getConfiguration().getTableMappings();
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
				producer = new XmlProducer(new InputSource(src.toURI().toURL().toString()));
			}
			else if (format == ProducerType.Cvs) {
				producer = new CsvProducer(src);
			}
			else if (format == ProducerType.Flat) {
				producer = new FlatXmlProducer(new InputSource(src.toURI().toURL().toString()));
			}
			else if (format == ProducerType.Dtd) {
				producer = new FlatDtdProducer(new InputSource(src.toURI().toURL().toString()));
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
	                                         MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipalPP forPrincipal) {
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

	protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, @Nullable RegDate fin, @Nullable MotifFor motifFermeture,
	                                         TypeAutoriteFiscale type, int noOfs, MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipalPP forPrincipal) {
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

	protected static void assertForDebiteur(RegDate debut, @Nullable RegDate fin, TypeAutoriteFiscale taf, int noOFS, ForDebiteurPrestationImposable forFiscal) {
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
	protected static void assertDIPP(RegDate debut, RegDate fin, @Nullable TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                                 TypeDocument typeDocument, Long idCollRetour, @Nullable RegDate dateRetourImprimee, List<Declaration> declarations) {
		assertNotNull(declarations);
		assertEquals(declarations.size(), 1);
		assertDIPP(debut, fin, etat, typeContribuable, typeDocument, idCollRetour, dateRetourImprimee, declarations.get(0));
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
	protected static void assertDIPP(RegDate debut, RegDate fin, @Nullable TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                                 TypeDocument typeDocument, Long idCollRetour, @Nullable RegDate delaiRetourImprime, Declaration declaration) {
		assertNotNull(declaration);
		DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) declaration;
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
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, @Nullable CollectiviteAdministrative collectivite, TacheEnvoiDeclarationImpotPP tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDateDebut());
		assertEquals(dateFin, tache.getDateFin());
		assertEquals(typeCtb, tache.getTypeContribuable());
		assertEquals(typeDoc, tache.getTypeDocument());
		assertEquals(adresseRetour, tache.getAdresseRetour());
		if (collectivite != null) {
			assertEquals(collectivite, tache.getCollectiviteAdministrativeAssignee());
		}

	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, @Nullable CollectiviteAdministrative collectivite, TacheEnvoiDeclarationImpotPM tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDateDebut());
		assertEquals(dateFin, tache.getDateFin());
		assertEquals(typeCtb, tache.getTypeContribuable());
		assertEquals(typeDoc, tache.getTypeDocument());
		if (collectivite != null) {
			assertEquals(collectivite, tache.getCollectiviteAdministrativeAssignee());
		}
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, TacheEnvoiDeclarationImpotPP tache) {
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, adresseRetour, null, tache);
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TacheEnvoiDeclarationImpotPM tache) {
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, null, tache);
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin, TacheAnnulationDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDeclaration().getDateDebut());
		assertEquals(dateFin, tache.getDeclaration().getDateFin());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTacheAnnulationDI(TypeEtatTache etat, long diId, boolean annule, TacheAnnulationDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(Long.valueOf(diId), tache.getDeclaration().getId());
		assertEquals(annule, tache.isAnnule());
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
	                                     TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, List<? extends TacheEnvoiDeclarationImpot> taches) {
		assertNotNull(taches);
		assertEquals(1, taches.size());
		final TacheEnvoiDeclarationImpot tache = taches.get(0);
		assertEquals(TacheEnvoiDeclarationImpotPP.class, tache.getClass());
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, adresseRetour, (TacheEnvoiDeclarationImpotPP) tache);
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
	 * Ajoute une période fiscale dans la base de données (avec les délais usuels si le paramètre "addParametres" est à <code>true</code>)
	 */
	protected PeriodeFiscale addPeriodeFiscale(int annee, boolean addParametres) {
		final PeriodeFiscale periode = new PeriodeFiscale();
		periode.setAnnee(annee);
		if (addParametres) {
			periode.setDefaultPeriodeFiscaleParametres();
		}
		return merge(periode);
	}

	/**
	 * Ajoute une période fiscale dans la base de données (avec les délais usuels)
	 */
	protected PeriodeFiscale addPeriodeFiscale(int annee) {
		return addPeriodeFiscale(annee, true);
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
		doc = merge(doc);
		periode.addModeleDocument(doc);
		return doc;
	}

	/**
	 * Ajoute un nouveau modele de feuille dans la base de données
	 */
	protected ModeleFeuilleDocument addModeleFeuilleDocument(ModeleFeuille modele, ModeleDocument modeleDoc) {
		ModeleFeuilleDocument feuille = new ModeleFeuilleDocument();
		feuille.setNoCADEV(modele.getNoCADEV());
		feuille.setNoFormulaireACI(modele.getNoFormulaireACI());
		feuille.setIntituleFeuille(modele.getDescription());
		feuille.setPrincipal(modele.isPrincipal());
		feuille.setModeleDocument(modeleDoc);
		feuille = merge(feuille);
		return feuille;
	}

	/**
	 * Ajoute une LR au débiteur spécifié
	 */
	protected DeclarationImpotSource addListeRecapitulative(DebiteurPrestationImposable dpi, PeriodeFiscale periode, RegDate debut, RegDate fin, ModeleDocument modele) {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setModeCommunication(dpi.getModeCommunication());
		lr.setPeriodicite(dpi.getPeriodiciteAt(debut).getPeriodiciteDecompte());
		lr.setPeriode(periode);
		lr.setDateDebut(debut);
		lr.setDateFin(fin);
		lr.setModeleDocument(modele);
		lr.setTiers(dpi);
		lr = merge(lr);
		dpi.addDeclaration(lr);
		return lr;
	}

	/**
	 * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
	 */
	protected DeclarationImpotOrdinairePP addDeclarationImpot(ContribuableImpositionPersonnesPhysiques tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                          CollectiviteAdministrative retourCollectiviteAdministrative, TypeContribuable typeC, ModeleDocument modele) {

		final DeclarationImpotOrdinairePP d = new DeclarationImpotOrdinairePP();
		d.setPeriode(periode);
		d.setDateDebut(debut);
		d.setDateFin(fin);
		d.setTypeContribuable(typeC);
		d.setModeleDocument(modele);
		d.setRetourCollectiviteAdministrativeId(retourCollectiviteAdministrative == null ? null : retourCollectiviteAdministrative.getId());
		if (periode.getAnnee() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			d.setCodeSegment(0);
		}
		return assignerNumeroSequenceEtSaveDeclarationImpot(tiers, d);
	}

	protected <T extends DeclarationAvecNumeroSequence> T assignerNumeroSequenceEtSaveDeclarationImpot(Contribuable ctb, T di) {

		int numero = 0;
		final int annee = di.getPeriode().getAnnee();
		Set<Declaration> decls = ctb.getDeclarations();
		if (decls != null) {
			for (Declaration dd : decls) {
				if (dd.getPeriode().getAnnee() == annee) {
					++numero;
				}
			}
		}
		di.setNumero(numero + 1);

		di.setTiers(ctb);
		di = merge(di);

		ctb.addDeclaration(di);
		return di;
	}

	protected DeclarationImpotOrdinairePM addDeclarationImpot(ContribuableImpositionPersonnesMorales pm, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                          RegDate debutExercice, RegDate finExercice,
	                                                          CollectiviteAdministrative retourCollectiviteAdministrative,
	                                                          TypeContribuable typeContribuable, ModeleDocument modele) {
		final DeclarationImpotOrdinairePM d = new DeclarationImpotOrdinairePM();
		d.setPeriode(periode);
		d.setDateDebut(debut);
		d.setDateFin(fin);
		d.setDateDebutExerciceCommercial(debutExercice);
		d.setDateFinExerciceCommercial(finExercice);
		d.setTypeContribuable(typeContribuable);
		d.setModeleDocument(modele);
		d.setRetourCollectiviteAdministrativeId(retourCollectiviteAdministrative == null ? null : retourCollectiviteAdministrative.getId());
		return assignerNumeroSequenceEtSaveDeclarationImpot(pm, d);
	}

	protected DeclarationImpotOrdinairePM addDeclarationImpot(ContribuableImpositionPersonnesMorales pm, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                          CollectiviteAdministrative retourCollectiviteAdministrative,
	                                                          TypeContribuable typeContribuable, ModeleDocument modele) {
		return addDeclarationImpot(pm, periode, debut, fin, debut, fin, retourCollectiviteAdministrative, typeContribuable, modele);
	}

	protected QuestionnaireSNC addQuestionnaireSNC(Entreprise entreprise, PeriodeFiscale periode, RegDate dateDebut, RegDate dateFin) {
		final QuestionnaireSNC qsnc = new QuestionnaireSNC();
		qsnc.setPeriode(periode);
		qsnc.setDateDebut(dateDebut);
		qsnc.setDateFin(dateFin);
		return assignerNumeroSequenceEtSaveDeclarationImpot(entreprise, qsnc);
	}

	protected QuestionnaireSNC addQuestionnaireSNC(Entreprise entreprise, PeriodeFiscale periode) {
		final RegDate debut = RegDate.get(periode.getAnnee(), 1, 1);
		final RegDate fin = RegDate.get(periode.getAnnee(), 12, 31);
		return addQuestionnaireSNC(entreprise, periode, debut, fin);
	}

	/**
	 * Ajoute une tâche d'envoi de déclaration d'impôt PP avec les paramètres spécifiés.
	 */
	protected TacheEnvoiDeclarationImpotPP addTacheEnvoiDIPP(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                                         ContribuableImpositionPersonnesPhysiques contribuable, @Nullable Qualification qualification, @Nullable Integer codeSegment, @Nullable CollectiviteAdministrative colAdm) {
		TacheEnvoiDeclarationImpotPP tache = new TacheEnvoiDeclarationImpotPP(etat, dateEcheance, contribuable, dateDebut, dateFin, typeContribuable, typeDocument, qualification, codeSegment, TypeAdresseRetour.CEDI, colAdm);
		tache = merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'envoi de déclaration d'impôt PP avec les paramètres spécifiés.
	 */
	protected TacheEnvoiDeclarationImpotPM addTacheEnvoiDIPM(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                                         RegDate dateDebutExercice, RegDate dateFinExercice, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                                         ContribuableImpositionPersonnesMorales contribuable, CategorieEntreprise categoreEntreprise, @Nullable CollectiviteAdministrative colAdm) {
		TacheEnvoiDeclarationImpotPM tache = new TacheEnvoiDeclarationImpotPM(etat, dateEcheance, contribuable, dateDebut, dateFin, dateDebutExercice, dateFinExercice, typeContribuable, typeDocument, categoreEntreprise, colAdm);
		tache = merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheAnnulationDeclarationImpot addTacheAnnulDI(TypeEtatTache etat, RegDate dateEcheance, DeclarationImpotOrdinaire declaration, Contribuable contribuable,
	                                                          CollectiviteAdministrative colAdm) {
		TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(etat, dateEcheance, contribuable, declaration, colAdm);
		tache = merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'envoi de questionnaire SNC avec les paramètres spécifiés
	 */
	protected TacheEnvoiQuestionnaireSNC addTacheEnvoiQuestionnaireSNC(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                                                   CategorieEntreprise categorieEntreprise, Entreprise contribuable,
	                                                                   @Nullable CollectiviteAdministrative colAdm) {
		final TacheEnvoiQuestionnaireSNC tache = new TacheEnvoiQuestionnaireSNC(etat, dateEcheance, contribuable, dateDebut, dateFin, categorieEntreprise, colAdm);
		return merge(tache);
	}

	/**
	 * Ajoute une tâche d'annulation de questionnaire SNC avec les paramètres spécifiés
	 */
	protected TacheAnnulationQuestionnaireSNC addTacheAnnulationQuestionnaireSNC(TypeEtatTache etat, RegDate dateEcheance, QuestionnaireSNC questionnaire,
	                                                                             Entreprise entreprise, CollectiviteAdministrative colAdm) {
		final TacheAnnulationQuestionnaireSNC tache = new TacheAnnulationQuestionnaireSNC(etat, dateEcheance, entreprise, questionnaire, colAdm);
		return merge(tache);
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheControleDossier addTacheControleDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative colAdm) {
		TacheControleDossier tache = new TacheControleDossier(etat, dateEcheance, contribuable, colAdm);
		tache = merge(tache);
		return tache;
	}

	protected TacheTransmissionDossier addTacheTransmission(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheTransmissionDossier transmission = new TacheTransmissionDossier(etat, date(2010, 1, 1), ctb, ca);
		transmission = merge(transmission);
		return transmission;
	}

	protected TacheNouveauDossier addTacheNouveau(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheNouveauDossier nouveau = new TacheNouveauDossier(etat, date(2010, 1, 1), ctb, ca);
		nouveau = merge(nouveau);
		return nouveau;
	}

	protected TacheControleDossier addTacheControle(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheControleDossier controle = new TacheControleDossier(etat, date(2010, 1, 1), ctb, ca);
		controle = merge(controle);
		return controle;
	}

	protected Etiquette addEtiquette(String code, String libelle, TypeTiersEtiquette typeTiers, @Nullable CollectiviteAdministrative ca) {
		final Etiquette etiquette = new Etiquette(code, libelle, true, typeTiers, ca);
		return merge(etiquette);
	}

	protected EtiquetteTiers addEtiquetteTiers(Etiquette etiquette, Tiers tiers, RegDate dateDebut, RegDate dateFin) {
		final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(dateDebut, dateFin, etiquette);
		etiquetteTiers.setTiers(tiers);
		tiers.addEtiquette(etiquetteTiers);
		return etiquetteTiers;
	}

	protected PersonnePhysique addHabitant(long noIndividu) {
		final PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(noIndividu);
		return merge(hab);
	}

	protected PersonnePhysique addHabitant(long noTiers, long noIndividu) {
		final PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(noTiers);
		hab.setNumeroIndividu(noIndividu);
		return merge(hab);
	}

	/**
	 * Crée et ajoute dans la base de données un non-habitant minimal.
	 */
	protected PersonnePhysique addNonHabitant(@Nullable String prenom, String nom, @Nullable RegDate dateNaissance, @Nullable Sexe sexe) {
		return addNonHabitant(null, prenom, nom, dateNaissance, sexe);
	}

	protected PersonnePhysique addNonHabitant(@Nullable Long noTiers, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNumero(noTiers);
		nh.setPrenomUsuel(prenom);
		nh.setNom(nom);
		nh.setDateNaissance(dateNaissance);
		nh.setSexe(sexe);
		return merge(nh);
	}

	protected EvenementFiscalFor addEvenementFiscalFor(Tiers tiers, ForFiscal ff, RegDate dateValeur, EvenementFiscalFor.TypeEvenementFiscalFor type) {
		final EvenementFiscalFor evt = new EvenementFiscalFor(dateValeur, ff, type);
		evt.setTiers(tiers);
		return merge(evt);
	}

	/**
	 * Crée et ajoute dans la base de donnée un rapport d'appartenance ménage entre un ménage commun et une personne physique
	 */
	protected AppartenanceMenage addAppartenanceMenage(MenageCommun menage, PersonnePhysique pp, RegDate dateDebut, @Nullable RegDate dateFin, boolean annule) {
		AppartenanceMenage rapport = new AppartenanceMenage();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setObjet(menage);
		rapport.setSujet(pp);
		rapport.setAnnule(annule);
		rapport = merge(rapport);

		menage.addRapportObjet(rapport);
		pp.addRapportSujet(rapport);

		return rapport;
	}

	/**
	 * Crée et ajoute dans la base de données un menage-commun.
	 */
	protected EnsembleTiersCouple addEnsembleTiersCouple(PersonnePhysique principal, @Nullable PersonnePhysique conjoint, RegDate dateMariage, @Nullable RegDate dateFin) {
		return addEnsembleTiersCouple(null, principal, conjoint, dateMariage, dateFin);
	}

	/**
	 * Crée et ajoute dans la base de données un menage-commun.
	 */
	protected EnsembleTiersCouple addEnsembleTiersCouple(@Nullable Long noTiers, PersonnePhysique principal, @Nullable PersonnePhysique conjoint, RegDate dateMariage, @Nullable RegDate dateFin) {

		final MenageCommun menage = addMenageCommun(noTiers);
		principal = merge(principal);
		if (conjoint != null) {
			conjoint = merge(conjoint);
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

	protected MenageCommun addMenageCommun(@Nullable Long noTiers) {
		MenageCommun menage = new MenageCommun();
		menage.setNumero(noTiers);
		menage = merge(menage);
		return menage;
	}

	protected Parente addParente(PersonnePhysique enfant, PersonnePhysique parent, RegDate dateDebut, @Nullable RegDate dateFin) {
		final Parente parente = merge(new Parente(dateDebut, dateFin, parent, enfant));
		parent.addRapportObjet(parente);
		enfant.addRapportSujet(parente);
		return parente;
	}

	protected Tutelle addTutelle(PersonnePhysique pupille, Tiers tuteur, @Nullable CollectiviteAdministrative autoriteTutelaire, RegDate dateDebut, @Nullable RegDate dateFin) {
		Tutelle rapport = new Tutelle(dateDebut, dateFin, pupille, tuteur, autoriteTutelaire);
		rapport = merge(rapport);
		tuteur.addRapportObjet(rapport);
		pupille.addRapportSujet(rapport);
		return rapport;
	}

	protected Curatelle addCuratelle(PersonnePhysique pupille, Tiers curateur, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		Curatelle rapport = new Curatelle(dateDebut, dateFin, pupille, curateur, null);
		rapport = merge(rapport);
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
		rapport = merge(rapport);
		representant.addRapportObjet(rapport);
		represente.addRapportSujet(rapport);
		return rapport;
	}

	protected RepresentationConventionnelle addRepresentationConventionnelle(Tiers represente, Tiers representant, RegDate dateDebut, @Nullable RegDate dateFin, boolean extensionExecutionForcee) {
		RepresentationConventionnelle rapport = new RepresentationConventionnelle();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setObjet(representant);
		rapport.setSujet(represente);
		rapport.setExtensionExecutionForcee(extensionExecutionForcee);
		rapport = merge(rapport);
		representant.addRapportObjet(rapport);
		represente.addRapportSujet(rapport);
		return rapport;
	}

	protected ConseilLegal addConseilLegal(PersonnePhysique pupille, Tiers conseiller, RegDate dateDebut, @Nullable RegDate dateFin) {
		ConseilLegal rapport = new ConseilLegal(dateDebut, dateFin, pupille, conseiller, null);
		rapport = merge(rapport);
		conseiller.addRapportObjet(rapport);
		pupille.addRapportSujet(rapport);
		return rapport;
	}

	protected ActiviteEconomique addActiviteEconomique(PersonnePhysique pp, Etablissement etb, RegDate dateDebut, @Nullable RegDate dateFin, boolean principal) {
		ActiviteEconomique rapport = new ActiviteEconomique(dateDebut, dateFin, pp, etb, principal);
		rapport = merge(rapport);
		pp.addRapportSujet(rapport);
		etb.addRapportObjet(rapport);
		return rapport;
	}

	protected ActiviteEconomique addActiviteEconomique(Entreprise entreprise, Etablissement etb, RegDate dateDebut, @Nullable RegDate dateFin, boolean principal) {
		ActiviteEconomique rapport = new ActiviteEconomique(dateDebut, dateFin, entreprise, etb, principal);
		rapport = merge(rapport);
		entreprise.addRapportSujet(rapport);
		etb.addRapportObjet(rapport);
		return rapport;
	}

	protected FusionEntreprises addFusionEntreprises(Entreprise absorbante, Entreprise absorbee, RegDate dateBilanFusion) {
		FusionEntreprises fusion = new FusionEntreprises(dateBilanFusion, null, absorbee, absorbante);
		fusion = merge(fusion);
		absorbee.addRapportSujet(fusion);
		absorbante.addRapportObjet(fusion);
		return fusion;
	}

	protected ScissionEntreprise addScissionEntreprise(Entreprise scindee, Entreprise resultante, RegDate dateContratScission) {
		ScissionEntreprise scission = new ScissionEntreprise(dateContratScission, null, scindee, resultante);
		scission = merge(scission);
		scindee.addRapportSujet(scission);
		resultante.addRapportObjet(scission);
		return scission;
	}

	protected TransfertPatrimoine addTransfertPatrimoine(Entreprise emettrice, Entreprise receptrice, RegDate dateTransfert) {
		TransfertPatrimoine transfert = new TransfertPatrimoine(dateTransfert, null, emettrice, receptrice);
		transfert = merge(transfert);
		emettrice.addRapportSujet(transfert);
		receptrice.addRapportObjet(transfert);
		return transfert;
	}

	protected AutreCommunaute addAutreCommunaute(String nom) {
		final AutreCommunaute communaute = new AutreCommunaute();
		communaute.setNom(nom);
		return merge(communaute);
	}

	protected DebiteurPrestationImposable addDebiteur() {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi = merge(dpi);
		return dpi;
	}

	protected DebiteurPrestationImposable addDebiteur(Long numero) {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(numero);
		dpi = merge(dpi);
		return dpi;
	}

	protected Etablissement addEtablissement(@Nullable Long numero) {
		Etablissement eta = new Etablissement();
		eta.setNumero(numero);
		eta = merge(eta);
		return eta;
	}

	protected DebiteurPrestationImposable addDebiteur(String complementNom, Contribuable ctbLie, RegDate dateDebutContact) {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setComplementNom(complementNom);
		dpi = merge(dpi);

		ContactImpotSource rapport = new ContactImpotSource(dateDebutContact, null, ctbLie, dpi);
		rapport = merge(rapport);

		dpi.addRapportObjet(rapport);
		ctbLie.addRapportSujet(rapport);

		return dpi;
	}

	protected Entreprise addEntrepriseConnueAuCivil(long idCantonal) {
		final Entreprise ent = new Entreprise();
		ent.setNumeroEntreprise(idCantonal);
		return merge(ent);
	}

	protected Entreprise addEntrepriseInconnueAuCivil() {
		final Entreprise ent = new Entreprise();
		return merge(ent);
	}

	protected Entreprise addEntrepriseInconnueAuCivil(long noContribuable) {
		final Entreprise ent = new Entreprise(noContribuable);
		return merge(ent);
	}

	protected RaisonSocialeFiscaleEntreprise addRaisonSocialeFiscaleEntreprise(Entreprise entreprise, RegDate dateDebut, RegDate dateFin, String raisonSociale) {
		final RaisonSocialeFiscaleEntreprise raisonSocialeFiscale = new RaisonSocialeFiscaleEntreprise(dateDebut, dateFin, raisonSociale);
		entreprise.addDonneeCivile(raisonSocialeFiscale);
		return raisonSocialeFiscale;
	}

	protected Etablissement addEtablissement() {
		final Etablissement etb = new Etablissement();
		return merge(etb);
	}

	protected ReferenceAnnonceIDE addReferenceAnnonceIDE(String msgBusinessId, Etablissement etablissement) {
		final ReferenceAnnonceIDE referenceAnnonceIDE = new ReferenceAnnonceIDE(msgBusinessId, etablissement);
		return merge(referenceAnnonceIDE);
	}

	protected ActiviteEconomique addLienActiviteEconomique(Contribuable ctb, Etablissement etablissement, RegDate dateDebut, @Nullable RegDate dateFin, boolean principal) {
		final ActiviteEconomique ret = new ActiviteEconomique(dateDebut, dateFin, ctb, etablissement, principal);
		return merge(ret);
	}

	protected CollectiviteAdministrative addCollAdm(int numero) {
		final CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(numero);
		return merge(ca);
	}

	protected EtatDeclarationEmise addEtatDeclarationEmise(Declaration declaration, RegDate dateObtention) {
		final EtatDeclarationEmise etat = new EtatDeclarationEmise(dateObtention);
		declaration.addEtat(etat);
		return etat;
	}

	protected EtatDeclarationEchue addEtatDeclarationEchue(Declaration declaration, RegDate dateObtention) {
		final EtatDeclarationEchue etat = new EtatDeclarationEchue(dateObtention);
		declaration.addEtat(etat);
		return etat;
	}

	protected EtatDeclarationRetournee addEtatDeclarationRetournee(Declaration declaration, RegDate dateObtention) {
		return addEtatDeclarationRetournee(declaration, dateObtention, "TEST");
	}

	protected EtatDeclarationRetournee addEtatDeclarationRetournee(Declaration declaration, RegDate dateObtention, @Nullable String source) {
		final EtatDeclarationRetournee etat = new EtatDeclarationRetournee(dateObtention, source);
		declaration.addEtat(etat);
		return etat;
	}

	protected EtatDeclarationSommee addEtatDeclarationSommee(Declaration declaration, RegDate dateObtention, RegDate dateEnvoi, @Nullable Integer emolument) {
		Assert.assertTrue(declaration.isSommable());
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateObtention, dateEnvoi, emolument);
		declaration.addEtat(etat);
		return etat;
	}

	protected EtatDeclarationRappelee addEtatDeclarationRappelee(Declaration declaration, RegDate dateObtention, RegDate dateEnvoi) {
		Assert.assertTrue(declaration.isRappelable());
		final EtatDeclarationRappelee etat = new EtatDeclarationRappelee(dateObtention, dateEnvoi);
		declaration.addEtat(etat);
		return etat;
	}

	protected EtatDeclarationSuspendue addEtatDeclarationSuspendue(Declaration declaration, RegDate dateObtention) {
		final EtatDeclarationSuspendue etat = new EtatDeclarationSuspendue(dateObtention);
		declaration.addEtat(etat);
		return etat;
	}

	protected DelaiDeclaration addDelaiDeclaration(Declaration declaration, RegDate dateTraitement, RegDate delaiAccordeAu, EtatDelaiDeclaration etat) {
		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(etat);
		delai.setDateTraitement(dateTraitement);
		delai.setDateDemande(dateTraitement);
		delai.setDelaiAccordeAu(delaiAccordeAu);
		declaration.addDelai(delai);
		return delai;
	}

	/**
	 * Ajoute un droit d'accès (autorisation ou interdiction) entre un opérateur et un tiers.
	 */
	protected DroitAcces addDroitAcces(long noIndOperateur, PersonnePhysique pp, TypeDroitAcces type, Niveau niveau, RegDate debut,
			@Nullable RegDate fin) {

		DroitAcces da = new DroitAcces();
		da.setDateDebut(debut);
		da.setDateFin(fin);
		da.setNoIndividuOperateur(noIndOperateur);
		da.setType(type);
		da.setNiveau(niveau);
		da.setTiers(pp);

		da = merge(da);
		return da;
	}

	/**
	 * Ajoute un for principal sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture,
	                                               @Nullable MotifFor motifFermeture, Integer noOFS, TypeAutoriteFiscale type, ModeImposition modeImposition, MotifRattachement motif) {
		ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f.setModeImposition(modeImposition);
		f = tiersDAO.addAndSave(contribuable, f);
		return f;
	}


		/**
	 * Ajoute un for principal Source sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipalPP addForPrincipalSource(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS) {
		ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
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
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, Integer noOFS, MotifRattachement motif) {
		return addForSecondaire(tiers, ouverture, motifOuverture, noOFS, motif, GenreImpot.REVENU_FORTUNE);
	}

	/**
	 * Ajoute un for fiscal secondaire ouvert.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, Integer noOFS, MotifRattachement motif, GenreImpot genreImpot) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setGenreImpot(genreImpot);
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
		return addForSecondaire(tiers, ouverture, motifOuverture, fermeture, motifFermeture, noOFS, motif, GenreImpot.REVENU_FORTUNE);
	}

	/**
	 * Ajoute un for fiscal secondaire fermé.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
	                                               MotifFor motifFermeture, Integer noOFS, MotifRattachement motif, GenreImpot genreImpot) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(genreImpot);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f = tiersDAO.addAndSave(tiers, f);
		return f;
	}

	protected ForFiscalAutreImpot addForAutreImpot(Contribuable tiers, RegDate ouverture, @Nullable RegDate fermeture, Integer noOFS,
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

	protected Immeuble addImmeuble(Contribuable tiers, String numero, RegDate dateDebut, @Nullable RegDate dateFin, String nomCommune, String nature, TypeImmeuble typeImmeuble,
	                               GenrePropriete genrePropriete, int estimationFiscale, String referenceEstimationFiscale, String partPropriete, RegDate dateDerniereMutation,
	                               TypeMutation derniereMutation) {
		Immeuble i = new Immeuble();
		i.setNumero(numero);
		i.setIdRF("ID-RF-" + numero);
		i.setProprietaire(new Proprietaire("1111", 1111L));
		i.setDateDebut(dateDebut);
		i.setDateFin(dateFin);
		i.setNomCommune(nomCommune);
		i.setNature(nature);
		i.setTypeImmeuble(typeImmeuble);
		i.setGenrePropriete(genrePropriete);
		i.setEstimationFiscale(estimationFiscale);
		i.setReferenceEstimationFiscale(referenceEstimationFiscale);
		i.setPartPropriete(PartPropriete.parse(partPropriete));
		i.setDateDerniereMutation(dateDerniereMutation);
		i.setDerniereMutation(derniereMutation);
		i = tiersDAO.addAndSave(tiers, i);
		return i;
	}

	/**
	 * Ajoute une décision sur le contribuable spécifié.
	 */
	protected DecisionAci addDecisionAci(Contribuable contribuable, RegDate debut, @Nullable RegDate fin,
	                                             Integer noOFS, TypeAutoriteFiscale type, String  remarque) {
		DecisionAci decision = new DecisionAci();
		decision.setDateDebut(debut);
		decision.setDateFin(fin);
		decision.setTypeAutoriteFiscale(type);
		decision.setNumeroOfsAutoriteFiscale(noOFS);
		decision.setRemarque(remarque);
		decision = tiersDAO.addAndSave(contribuable, decision);
		return decision;
	}

	protected Bouclement addBouclement(Entreprise e, RegDate dateDebut, DayMonth ancrage, int periodeEnMois) {
		final Bouclement bouclement = new Bouclement();
		bouclement.setAncrage(ancrage);
		bouclement.setDateDebut(dateDebut);
		bouclement.setPeriodeMois(periodeEnMois);
		return tiersDAO.addAndSave(e, bouclement);
	}

	protected LettreBienvenue addLettreBienvenue(Entreprise e, RegDate dateEnvoi, RegDate delaiRetour, @Nullable RegDate dateRetour, @Nullable RegDate dateRappel, TypeLettreBienvenue type) {
		final LettreBienvenue lettre = new LettreBienvenue();
		lettre.setDateEnvoi(dateEnvoi);
		lettre.setDelaiRetour(delaiRetour);
		lettre.setDateRetour(dateRetour);
		lettre.setDateRappel(dateRappel);
		lettre.setType(type);
		return tiersDAO.addAndSave(e, lettre);
	}

	protected DemandeDegrevementICI addDemandeDegrevementICI(Entreprise e, RegDate dateEnvoi, RegDate delaiRetour, @Nullable RegDate dateRetour, @Nullable RegDate dateRappel, int periodeFiscale, ImmeubleRF immeuble) {
		final DemandeDegrevementICI demande = new DemandeDegrevementICI();
		demande.setDateEnvoi(dateEnvoi);
		demande.setDelaiRetour(delaiRetour);
		demande.setDateRetour(dateRetour);
		demande.setDateRappel(dateRappel);
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setImmeuble(immeuble);
		return addNumeroSequenceAndSave(e, demande);
	}

	private DemandeDegrevementICI addNumeroSequenceAndSave(Entreprise e, DemandeDegrevementICI demande) {
		final int pf = demande.getPeriodeFiscale();
		final int maxNosUtilises = Optional.ofNullable(e.getAutresDocumentsFiscaux()).orElseGet(Collections::emptySet).stream()
				.filter(doc -> doc instanceof DemandeDegrevementICI)
				.map(doc -> (DemandeDegrevementICI) doc)
				.filter(dd -> dd.getPeriodeFiscale() == pf)
				.mapToInt(DemandeDegrevementICI::getNumeroSequence)
				.max()
				.orElse(0);
		demande.setNumeroSequence(maxNosUtilises + 1);
		return tiersDAO.addAndSave(e, demande);
	}

	protected EtatEntreprise addEtatEntreprise(Entreprise e, RegDate dateObtention, TypeEtatEntreprise type, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise etat = new EtatEntreprise();
		etat.setDateObtention(dateObtention);
		etat.setType(type);
		etat.setGeneration(generation);
		return tiersDAO.addAndSave(e, etat);
	}

	protected FlagEntreprise addFlagEntreprise(Entreprise e, RegDate dateDebut, @Nullable RegDate dateFin, TypeFlagEntreprise type) {
		final FlagEntreprise flag = new FlagEntreprise();
		flag.setType(type);
		flag.setDateDebut(dateDebut);
		flag.setDateFin(dateFin);
		return tiersDAO.addAndSave(e, flag);
	}

	protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales ctb, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture,
	                                               @Nullable MotifFor motifFermeture, Integer noOFS, TypeAutoriteFiscale type, MotifRattachement motif, GenreImpot genreImpot) {

		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
		ffp.setDateDebut(ouverture);
		ffp.setMotifOuverture(motifOuverture);
		ffp.setDateFin(fermeture);
		ffp.setMotifFermeture(motifFermeture);
		ffp.setGenreImpot(genreImpot);
		ffp.setTypeAutoriteFiscale(type);
		ffp.setNumeroOfsAutoriteFiscale(noOFS);
		ffp.setMotifRattachement(motif);
		return tiersDAO.addAndSave(ctb, ffp);
	}

	/**
	 * Raccourci pour créer une RegDate.
	 */
	protected static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}

	/**
	 * Raccourci pour créer une RegDate (partielle).
	 */
	protected static RegDate date(int year, int month) {
		return RegDate.get(year, month);
	}

	/**
	 * Raccourci pour créer une RegDate (partielle).
	 */
	protected static RegDate date(int year) {
		return RegDate.get(year);
	}

	/**
	 * @param classes les classes dont on veut récupérer tous les représentants
	 * @return la liste de tous les tiers de la base qui sont des instances des classes données
	 */
	@SafeVarargs
	protected final <T extends Tiers> List<T> allTiersOfType(final Class<? extends T>... classes) {
		if (classes == null) {
			return Collections.emptyList();
		}
		final List<Tiers> all = tiersDAO.getAll();
		final List<T> filtered = new ArrayList<>(all.size());
		for (Tiers t : all) {
			for (Class<? extends T> clazz : classes) {
				if (clazz.isAssignableFrom(t.getClass())) {
					filtered.add((T) t);
				}
			}
		}
		return filtered;
	}

	protected PersonnePhysiqueRF addPersonnePhysiqueRF(String prenom, String nom, RegDate dateNaissance, String idRf, long noRf, Long numeroContribuable) {
		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setDateNaissance(dateNaissance);
		pp.setIdRF(idRf);
		pp.setNoRF(noRf);
		pp.setPrenom(prenom);
		pp.setNom(nom);
		pp.setNoContribuable(numeroContribuable);
		return merge(pp);
	}

	protected CollectivitePubliqueRF addCollectivitePubliqueRF(String raisonSociale, String idRf, long noRf, Long numeroContribuable) {
		final CollectivitePubliqueRF c = new CollectivitePubliqueRF();
		c.setRaisonSociale(raisonSociale);
		c.setIdRF(idRf);
		c.setNoRF(noRf);
		c.setNoContribuable(numeroContribuable);
		return merge(c);
	}

	protected PersonneMoraleRF addPersonneMoraleRF(String raisonSociale, String numeroRC, String idRf, long noRf, Long numeroContribuable) {
		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setRaisonSociale(raisonSociale);
		pm.setIdRF(idRf);
		pm.setNoRF(noRf);
		pm.setNoContribuable(numeroContribuable);
		pm.setNumeroRC(numeroRC);
		return merge(pm);
	}

	protected RapprochementRF addRapprochementRF(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, TypeRapprochementRF type, Contribuable ctb, TiersRF tiersRF, boolean annule) {
		final RapprochementRF rrf = new RapprochementRF();
		rrf.setAnnule(annule);
		rrf.setDateDebut(dateDebut);
		rrf.setDateFin(dateFin);
		rrf.setContribuable(ctb);
		rrf.setTiersRF(tiersRF);
		rrf.setTypeRapprochement(type);
		final RapprochementRF saved = merge(rrf);
		ctb.addRapprochementRF(saved);
		return saved;
	}

	protected BienFondRF addImmeubleRF(String idRF) {
		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF(idRF);
		return merge(immeuble);
	}

	protected CommunauteRF addCommunauteRF(String idRF, TypeCommunaute type) {
		final CommunauteRF communauteRF = new CommunauteRF();
		communauteRF.setIdRF(idRF);
		communauteRF.setType(type);
		return merge(communauteRF);
	}

	@NotNull
	protected DroitProprietePersonnePhysiqueRF addDroitPersonnePhysiqueRF(RegDate dateDebut, RegDate dateDebutOfficielle, RegDate dateFin, String motifDebut, String motifFin, String masterIdRF, IdentifiantAffaireRF numeroAffaire, Fraction part, GenrePropriete regime,
	                                                                      PersonnePhysiqueRF ayantDroit,
	                                                                      BienFondRF immeuble, CommunauteRF communaute) {
		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setAyantDroit(ayantDroit);
		droit.setCommunaute(communaute);
		droit.setImmeuble(immeuble);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutOfficielle(dateDebutOfficielle);
		droit.setDateFin(dateFin);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setNumeroAffaire(numeroAffaire);
		droit.setPart(part);
		droit.setRegime(regime);
		return merge(droit);
	}

	@NotNull
	protected DroitProprietePersonneMoraleRF addDroitPersonneMoraleRF(RegDate dateDebut, RegDate dateDebutOfficielle, RegDate dateFin, String motifDebut, String motifFin, String masterIdRF, IdentifiantAffaireRF numeroAffaire, Fraction part, GenrePropriete regime,
	                                                                  PersonneMoraleRF ayantDroit,
	                                                                  BienFondRF immeuble, CommunauteRF communaute) {
		final DroitProprietePersonneMoraleRF droit = new DroitProprietePersonneMoraleRF();
		droit.setAyantDroit(ayantDroit);
		droit.setCommunaute(communaute);
		droit.setImmeuble(immeuble);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutOfficielle(dateDebutOfficielle);
		droit.setDateFin(dateFin);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setNumeroAffaire(numeroAffaire);
		droit.setPart(part);
		droit.setRegime(regime);
		return merge(droit);
	}

	@NotNull
	protected DroitProprieteCommunauteRF addDroitCommunauteRF(RegDate dateDebut, RegDate dateDebutOfficielle, RegDate dateFin, String motifDebut, String motifFin, String masterIdRF, IdentifiantAffaireRF numeroAffaire, Fraction part, GenrePropriete regime,
	                                                          CommunauteRF communauteRF, BienFondRF immeuble) {
		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(communauteRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutOfficielle(dateDebutOfficielle);
		droit.setDateFin(dateFin);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setNumeroAffaire(numeroAffaire);
		droit.setPart(part);
		droit.setRegime(regime);
		return merge(droit);
	}

	@NotNull
	protected UsufruitRF addUsufruitRF(RegDate dateDebut, RegDate dateDebutOfficielle, RegDate dateFin, String motifDebut, String motifFin, String masterIdRF, IdentifiantAffaireRF numeroAffaire, IdentifiantDroitRF identifiantDroitRF,
	                                   TiersRF tiersRF, BienFondRF immeuble) {
		final UsufruitRF droit = new UsufruitRF();
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(tiersRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutOfficielle(dateDebutOfficielle);
		droit.setDateFin(dateFin);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setNumeroAffaire(numeroAffaire);
		droit.setIdentifiantDroit(identifiantDroitRF);
		return merge(droit);
	}

	@NotNull
	protected DroitHabitationRF addDroitHabitationRF(RegDate dateDebut, RegDate dateDebutOfficielle, RegDate dateFin, String motifDebut, String motifFin, String masterIdRF, IdentifiantAffaireRF numeroAffaire, IdentifiantDroitRF identifiantDroitRF,
	                                                 TiersRF tiersRF, BienFondRF immeuble) {
		final DroitHabitationRF droit = new DroitHabitationRF();
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(tiersRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutOfficielle(dateDebutOfficielle);
		droit.setDateFin(dateFin);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setNumeroAffaire(numeroAffaire);
		droit.setIdentifiantDroit(identifiantDroitRF);
		return merge(droit);
	}
}
