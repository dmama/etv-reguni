package ch.vd.unireg.oid;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.SuppressionOIDRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class SuppressionOIDJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(SuppressionOIDJob.class);

	public static final String NAME = "SuppressionOIDJob";
	public static final String OID = "OID";

	private DataSource dataSource;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private RapportService rapportService;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;

	public SuppressionOIDJob(int sortOrder, String description) {
		super(NAME, JobCategory.OID, sortOrder, description);

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
		return template.execute(status -> {
			final CollectiviteAdministrative officeImpot = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(oid, true);
			if (officeImpot == null) {
				throw new IllegalArgumentException("L'office d'impôt n°" + oid + " est introuvable dans la base de donnée !");
			}
			return officeImpot.getId();
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
		final SuppressionOIDRapport rapport = template.execute(s -> {
			try {
				return rapportService.generateRapport(results, status);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		setLastRunReport(rapport);
		audit.success("La suppression de l'OID n°" + oid + " est terminée.", rapport);
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

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, SuppressionOIDResults> template =
				new BatchTransactionTemplateWithResults<>(ids, 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, SuppressionOIDResults>() {

			@Override
			public SuppressionOIDResults createSubRapport() {
				return new SuppressionOIDResults(oid, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, SuppressionOIDResults rapport) throws Exception {
				final Long first = batch.get(0);
				final Long last = batch.get(batch.size() - 1);
				status.setMessage("Correction de l'OID sur les tiers n°" + first + " à " + last, progressMonitor.getProgressInPercent());

				processBatch(batch, oid, officeImpotId, rapport);
				return !status.isInterrupted();
			}
		}, progressMonitor);

		if (status.isInterrupted()) {
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
		return template.execute(status -> {
			final Map<String, Integer> oidParam = new HashMap<>(1);
			oidParam.put("oid", oid);

			final Map<String, Long> officeImpotIdParam = new HashMap<>(1);
			officeImpotIdParam.put("officeImpotId", officeImpotId);

			final Set<Long> ids = new TreeSet<>();

			// sur le tiers lui-même
			ids.addAll(hibernateTemplate.find("select tiers.id from Tiers tiers where tiers.officeImpotId=:oid", oidParam, null));

			// sur ces déclarations
			ids.addAll(hibernateTemplate.find("select di.tiers.id from DeclarationImpotOrdinaire di where di.retourCollectiviteAdministrativeId=:officeImpotId", officeImpotIdParam, null));

			// sur les mouvements de dossier
			ids.addAll(hibernateTemplate.find("select ed.contribuable.id from EnvoiDossier ed where ed.collectiviteAdministrativeEmettrice.id=:officeImpotId", officeImpotIdParam, null));
			ids.addAll(hibernateTemplate.find("select ed.contribuable.id from EnvoiDossierVersCollectiviteAdministrative ed where ed.collectiviteAdministrativeDestinataire.id=:officeImpotId",
			                                  officeImpotIdParam, null));
			ids.addAll(hibernateTemplate.find("select rd.contribuable.id from ReceptionDossier rd where rd.collectiviteAdministrativeReceptrice.id=:officeImpotId", officeImpotIdParam, null));

			// sur les tâches
			ids.addAll(hibernateTemplate.find("select t.contribuable.id from Tache t where t.collectiviteAdministrativeAssignee.id=:officeImpotId", officeImpotIdParam, null));
			return ids;
		});
	}

	private void processBatch(final List<Long> batch, final int oid, final long officeImpotId, final SuppressionOIDResults rapport) {

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.execute((ConnectionCallback<Object>) con -> {

			// on crée des opérations (prepare statements sql) à l'avance
			try (UpdateOperation updTiers = new UpdateTiers(con);
			     UpdateOperation updDeclaration = new UpdateDeclarations(con);
			     UpdateOperation updMvtDest = new UpdateMouvementsDestinations(con);
			     UpdateOperation updMvtEmetteur = new UpdateMouvementsEmetteurs(con);
			     UpdateOperation updMvtRecepteur = new UpdateMouvementsRecepteurs(con);
			     UpdateOperation updTache = new UpdateTaches(con)) {

				final List<UpdateOperation> operations = Arrays.asList(updTiers, updDeclaration, updMvtDest, updMvtEmetteur, updMvtRecepteur, updTache);
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
					final Set<String> tables = new HashSet<>();
					for (UpdateOperation operation : operations) {
						if (operation.execute(id, oid, officeImpotId, newOfficeImpot.getNumeroCollectiviteAdministrative(), newOfficeImpot.getId(), muser) > 0) {
							tables.add(operation.getTable());
						}
					}

					rapport.addTraite(id, newOfficeImpot.getNumeroCollectiviteAdministrative(), tables);
				}

				return null;
			}
		});
	}

	private interface UpdateOperation extends AutoCloseable {
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
			super(con.prepareStatement("UPDATE DOCUMENT_FISCAL T SET LOG_MDATE=CURRENT_DATE, LOG_MUSER=?, RETOUR_COLL_ADMIN_ID=? WHERE TIERS_ID=? AND RETOUR_COLL_ADMIN_ID=?"));
		}

		@Override
		public String getTable() {
			return "DOCUMENT_FISCAL";
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
