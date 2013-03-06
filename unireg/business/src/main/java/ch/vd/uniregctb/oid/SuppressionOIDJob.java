package ch.vd.uniregctb.oid;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.SuppressionOIDRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class SuppressionOIDJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(SuppressionOIDJob.class);

	public static final String NAME = "SuppressionOIDJob";
	private static final String CATEGORIE = "OID";

	public static final String OID = "OID";

	private DataSource dataSource;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private RapportService rapportService;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;

	public SuppressionOIDJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("OID à supprimer");
		param.setName(OID);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, null);
	}

	private long getOfficeImpotId(final int oid) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<Long>() {
			@SuppressWarnings("unchecked")
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final CollectiviteAdministrative officeImpot = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(oid, true);
				if (officeImpot == null) {
					throw new IllegalArgumentException("L'office d'impôt n°" + oid + " est introuvable dans la base de donnée !");
				}

				return officeImpot.getId();
			}
		});
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager status = getStatusManager();

		final int oid = getIntegerValue(params, OID); // le numéro métier de l'office d'impôt

		final SuppressionOIDResults results = supprimerOID(oid, dateTraitement, status);

		// Exécution du rapport dans une transaction.
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final SuppressionOIDRapport rapport = template.execute(new TransactionCallback<SuppressionOIDRapport>() {
			@Override
			public SuppressionOIDRapport doInTransaction(TransactionStatus s) {
				try {
					return rapportService.generateRapport(results, status);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		Audit.success("La suppression de l'OID n°" + oid + " est terminée.", rapport);
	}

	/**
	 * Cette méthode va mettre-à-jour les différentes tables de la table et remplacer toutes les références à l'OID passé en paramètre (qui a été normalement supprimé) par des références au nouvel OID.
	 * Le nouvel OID est dépendant de la situation de chaque tiers.
	 *
	 * @param oid            le numéro métier de l'office d'impôt qui a été supprimé
	 * @param dateTraitement la date de traitement
	 * @param status         le status manager
	 * @return les résultats de la suppression
	 */
	protected SuppressionOIDResults supprimerOID(final int oid, final RegDate dateTraitement, final StatusManager status) {

		status.setMessage("Recherche des tiers à corriger...");
		final long officeImpotId = getOfficeImpotId(oid); // l'id hibernate du tiers "office d'impôt" correspondant
		final Set<Long> ids = getIdsOfTiersLinkedTo(oid, officeImpotId);

		final SuppressionOIDResults rapportFinal = new SuppressionOIDResults(oid, dateTraitement, tiersService, adresseService);
		rapportFinal.total = ids.size();

		final BatchTransactionTemplate<Long, SuppressionOIDResults> template =
				new BatchTransactionTemplate<Long, SuppressionOIDResults>(ids, 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, SuppressionOIDResults>() {

			@Override
			public SuppressionOIDResults createSubRapport() {
				return new SuppressionOIDResults(oid, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, SuppressionOIDResults rapport) throws Exception {
				final Long first = batch.get(0);
				final Long last = batch.get(batch.size() - 1);
				status.setMessage("Correction de l'OID sur les tiers n°" + first + " à " + last, percent);

				processBatch(batch, oid, officeImpotId, rapport);
				return !status.interrupted();
			}
		});

		if (status.interrupted()) {
			status.setMessage("La suppression de l'OID n°" + oid + " a été interrompue."
					                  + " Nombre de tiers traités au moment de l'interruption = " + rapportFinal.traites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La suppression de l'OID n°" + oid + " est terminée." +
					                  " Nombre de tiers traités = " + rapportFinal.traites.size() + ". Nombre d'erreurs = " + rapportFinal.errors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Cette méthode retourne les ids des tiers ayant un lien (à travers des déclarations, des tâches, ...) vers l'OID spécifié.
	 *
	 * @param oid           le numéro métier de l'office d'impôt
	 * @param officeImpotId l'id technique (de l'entité hibernate) de l'office d'impôt
	 * @return les ids des tiers demandés
	 */
	private Set<Long> getIdsOfTiersLinkedTo(final int oid, final long officeImpotId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<Set<Long>>() {
			@SuppressWarnings("unchecked")
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {

				final Object[] oidParam = new Object[] { oid };
				final Object[] officeImpotIdParam = new Object[] { officeImpotId };

				final Set<Long> ids = new TreeSet<Long>();

				// sur le tiers lui-même
				ids.addAll(hibernateTemplate.<Long>find("select tiers.id from Tiers tiers where tiers.officeImpotId = ?", oidParam, null));

				// sur ces déclarations
				ids.addAll(hibernateTemplate.<Long>find("select di.tiers.id from DeclarationImpotOrdinaire di where di.retourCollectiviteAdministrativeId = ?", officeImpotIdParam, null));

				// sur les mouvements de dossier
				ids.addAll(hibernateTemplate.<Long>find("select ed.contribuable.id from EnvoiDossier ed where ed.collectiviteAdministrativeEmettrice = ?", officeImpotIdParam, null));
				ids.addAll(hibernateTemplate.<Long>find("select ed.contribuable.id from EnvoiDossierVersCollectiviteAdministrative ed where ed.collectiviteAdministrativeDestinataire= ?",
				                                        officeImpotIdParam, null));
				ids.addAll(hibernateTemplate.<Long>find("select rd.contribuable.id from ReceptionDossier rd where rd.collectiviteAdministrativeReceptrice= ?", officeImpotIdParam, null));

				// sur les tâches
				ids.addAll(hibernateTemplate.<Long>find("select t.contribuable.id from Tache t where t.collectiviteAdministrativeAssignee = ?", officeImpotIdParam, null));

				return ids;
			}
		});
	}

	private void processBatch(final List<Long> batch, final int oid, final long officeImpotId, final SuppressionOIDResults rapport) {

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.execute(new ConnectionCallback<Object>() {
			@Override
			public Object doInConnection(Connection con) throws SQLException, DataAccessException {

				// on crée des opérations (prepare statements sql) à l'avance
				final List<UpdateOperation> operations = Arrays.<UpdateOperation>asList(new UpdateTiers(con),
				                                                                        new UpdateDeclarations(con),
				                                                                        new UpdateMouvementsDestinations(con),
				                                                                        new UpdateMouvementsEmetteurs(con),
				                                                                        new UpdateMouvementsRecepteurs(con),
				                                                                        new UpdateTaches(con));

				try {
					for (Long id : batch) {

						// le nouvel office d'impôt de chaque tiers peut être différent, on va donc le chercher maintenant
						final Tiers tiers = tiersDAO.get(id);
						final CollectiviteAdministrative newOfficeImpot = tiersService.getOfficeImpotAt(tiers, RegDate.get()); // on spécifie explicitement la date du jour pour ne pas tomber dans le cache de l'OID
						if (newOfficeImpot == null) {
							rapport.addOIDInconnu(id);
							continue;
						}

						// sanity check
						if (oid == newOfficeImpot.getNumeroCollectiviteAdministrative()) {
							throw new RuntimeException("Le nouvel office d'impôt calculé sur le tiers n°" + id + " est le même (" + newOfficeImpot.getNumeroCollectiviteAdministrative() +
									                           ") que l'ancien. Est-ce que le référentiel Fidor est à jour ?");
						}

						final String muser = "Fermeture-OID-" + oid + "-" + newOfficeImpot.getNumeroCollectiviteAdministrative();

						// on applique les changements
						final Set<String> tables = new HashSet<String>();
						for (UpdateOperation operation : operations) {
							if (operation.execute(id, oid, officeImpotId, newOfficeImpot.getNumeroCollectiviteAdministrative(), newOfficeImpot.getId(), muser) > 0) {
								tables.add(operation.getTable());
							}
						}

						rapport.addTraite(id, newOfficeImpot.getNumeroCollectiviteAdministrative(), tables);
					}

					return null;
				}
				finally {
					// on n'oublie pas de fermer les prepared statements...
					for (UpdateOperation operation : operations) {
						try {
							operation.close();
						}
						catch (SQLException e) {
							LOGGER.error("Impossible de fermer un prepared statement " + operation.getClass().getName(), e);
							// que puis-je faire d'autre...?
						}
					}
				}
			}
		});
	}

	private static interface UpdateOperation {

		String getTable();

		int execute(long id, final int oldOid, final long oldOfficeImpotId, final int newOid, final long newOfficeImpotId, String muser) throws SQLException;

		void close() throws SQLException;
	}

	public abstract class UpdateOperationImpl implements UpdateOperation {

		private final PreparedStatement st;

		protected UpdateOperationImpl(PreparedStatement st) {
			this.st = st;
		}

		@Override
		public final int execute(long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			fillStatementParameters(st, id, oldOid, oldOfficeImpotId, newOid, newOfficeImpotId, muser);
			return st.executeUpdate();
		}

		protected abstract void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException;

		@Override
		public void close() throws SQLException {
			st.close();
		}
	}

	public class UpdateTiers extends UpdateOperationImpl {

		public UpdateTiers(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE TIERS T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, OID=? WHERE NUMERO=? AND OID=?"));
		}

		@Override
		public String getTable() {
			return "TIERS";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setInt(2, newOid); // OID (nouveau)
			st.setLong(3, id); // NUMERO
			st.setInt(4, oldOid); // OID (ancien)
		}
	}

	public class UpdateDeclarations extends UpdateOperationImpl {

		public UpdateDeclarations(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE DECLARATION T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, RETOUR_COLL_ADMIN_ID=? WHERE TIERS_ID=? AND RETOUR_COLL_ADMIN_ID=?"));
		}

		@Override
		public String getTable() {
			return "DECLARATION";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setLong(2, newOfficeImpotId); // RETOUR_COLL_ADMIN_ID (nouveau)
			st.setLong(3, id); // TIERS_ID
			st.setLong(4, oldOfficeImpotId); // RETOUR_COLL_ADMIN_ID (ancien)
		}
	}

	public class UpdateMouvementsDestinations extends UpdateOperationImpl {

		public UpdateMouvementsDestinations(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE MOUVEMENT_DOSSIER T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, COLL_ADMIN_DEST_ID=? WHERE CTB_ID=? AND COLL_ADMIN_DEST_ID=?"));
		}

		@Override
		public String getTable() {
			return "MOUVEMENT";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setLong(2, newOfficeImpotId); // COLL_ADMIN_DEST_ID (nouveau)
			st.setLong(3, id); // CTB_ID
			st.setLong(4, oldOfficeImpotId); // COLL_ADMIN_DEST_ID (ancien)
		}
	}

	public class UpdateMouvementsEmetteurs extends UpdateOperationImpl {

		public UpdateMouvementsEmetteurs(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE MOUVEMENT_DOSSIER T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, COLL_ADMIN_EMETTRICE_ID=? WHERE CTB_ID=? AND COLL_ADMIN_EMETTRICE_ID=?"));
		}

		@Override
		public String getTable() {
			return "MOUVEMENT";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setLong(2, newOfficeImpotId); // COLL_ADMIN_EMETTRICE_ID (nouveau)
			st.setLong(3, id); // CTB_ID
			st.setLong(4, oldOfficeImpotId); // COLL_ADMIN_EMETTRICE_ID (ancien)
		}
	}

	public class UpdateMouvementsRecepteurs extends UpdateOperationImpl {

		public UpdateMouvementsRecepteurs(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE MOUVEMENT_DOSSIER T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, COLL_ADMIN_RECEPTRICE_ID=? WHERE CTB_ID=? AND COLL_ADMIN_RECEPTRICE_ID=?"));
		}

		@Override
		public String getTable() {
			return "MOUVEMENT";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setLong(2, newOfficeImpotId); // COLL_ADMIN_RECEPTRICE_ID (nouveau)
			st.setLong(3, id); // CTB_ID
			st.setLong(4, oldOfficeImpotId); // COLL_ADMIN_RECEPTRICE_ID (ancien)
		}
	}

	public class UpdateTaches extends UpdateOperationImpl {

		public UpdateTaches(Connection con) throws SQLException {
			super(con.prepareStatement("UPDATE TACHE T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, CA_ID=? WHERE CTB_ID=? AND CA_ID=?"));
		}

		@Override
		public String getTable() {
			return "TACHE";
		}

		@Override
		protected void fillStatementParameters(PreparedStatement st, long id, int oldOid, long oldOfficeImpotId, int newOid, long newOfficeImpotId, String muser) throws SQLException {
			st.setString(1, muser); // LOG_MUSER
			st.setLong(2, newOfficeImpotId); // CA_ID (nouveau)
			st.setLong(3, id); // CTB_ID
			st.setLong(4, oldOfficeImpotId); // CA_ID (ancien)
		}
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
