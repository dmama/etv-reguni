package ch.vd.uniregctb.evenement.ide;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.document.AnnoncesIDERapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AnnonceIDEJob extends JobDefinition {

	public static final String NAME = "AnnonceIDEJob";

	public static final String SIMULATION = "SIMULATION";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private Dialect dialect;
	private SessionFactory sessionFactory;
	private TiersDAO tiersDAO;
	private ServiceIDEService serviceIDEService;
	private RapportService rapportService;


	public AnnonceIDEJob(int sortOrder, String description) {
		super(NAME, JobCategory.TIERS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setMandatory(true);
			param.setName(SIMULATION);
			param.setDescription("Mode simulation");
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, true);
		}
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean simulation = getBooleanValue(params, SIMULATION);

		List<Long> tiersAEvaluer = getTiersAEvaluer();
		final RegDate aujourdhui = RegDate.get();

		final AnnonceIDEJobResults results = new AnnonceIDEJobResults(simulation);

		for (final Long tiersId : tiersAEvaluer) {

			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

			AnnonceIDEJobResults resultatEntreprise = template.execute(new TransactionCallback<AnnonceIDEJobResults>() {
				@Override
				public AnnonceIDEJobResults doInTransaction(TransactionStatus status) {
					final Tiers tiers = tiersDAO.get(tiersId);
					if (tiers instanceof Entreprise) {
						Entreprise entreprise = (Entreprise) tiers;
						if (serviceIDEService.isServiceIDEObligEtendues(entreprise, aujourdhui)) {
							AuthenticationHelper.pushPrincipal(AuthenticationHelper.getCurrentPrincipal());
							try {
								return evalueEntreprise(entreprise, simulation);
							}
							finally {
								AuthenticationHelper.popPrincipal();
							}
						}
					}
					throw new IllegalArgumentException("Le tiers n'est pas une entreprise.");
				}
			});
			results.addAll(resultatEntreprise);
		}

		final AnnoncesIDERapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success(String.format("%s des annonces IDE sur les tiers sous contrôle ACI terminé%s.", simulation ? "Simulation de l'envoi" : "Envoi", simulation ? "e" : ""));
	}

	/**
	 * Recherche les tiers pour lesquels il faut considérer l'envoi d'une annonce à l'IDE (qui ont le flag ide_dirty == true)
	 * @return la liste des numéros de tiers concernés. Vide si aucun.
	 */
	@NotNull
	private List<Long> getTiersAEvaluer() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		List<Long> result = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final Session session = sessionFactory.openSession();
				try {
					final SQLQuery query = session.createSQLQuery("SELECT t.numero FROM tiers t WHERE t.ide_dirty = " + dialect.toBooleanValueString(true) + " AND t.annulation_date is null");
					final List list = query.list();
					final List<Long> resultat = new ArrayList<Long>();
					for (Object o : list) {
						resultat.add(((BigDecimal)o).longValue());
					}
					return resultat;
				}
				finally {
					session.close();
				}
			}
		});
		return result == null ? Collections.<Long>emptyList() : result;
	}

	/**
	 * Tente de synchroniser l'IDE à l'entreprise. Le service IDE est appelé pour évaluer s'il faut mettre à jour les informations de l'IDE en tenant compte
	 * des différents facteurs comme le fait qu'on avoir déjà annoncé des paramètres civils.
	 * @param entreprise l'entreprise visée
	 * @param simulation si <code>true</code>, n'envoie pas réellement d'annonce à l'IDE, mais rapporte le prototype de l'annonce qui serait émise.
	 * @return une liste d'annonces à l'IDE ou de prototypes d'annonces à l'IDE en cas de simulation.
	 */
	protected AnnonceIDEJobResults evalueEntreprise(final Entreprise entreprise, final boolean simulation) {
			AnnonceIDEJobResults results = new AnnonceIDEJobResults(simulation);
			try {
				final TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

				results = template.execute(new TransactionCallback<AnnonceIDEJobResults>() {
					@Override
					public AnnonceIDEJobResults doInTransaction(TransactionStatus status) {
						AnnonceIDEJobResults results = new AnnonceIDEJobResults(simulation);
						try {
							if (simulation) {
								final ProtoAnnonceIDE protoAnnonceIDE = (ProtoAnnonceIDE) serviceIDEService.simuleSynchronisationIDE(entreprise);
								results.addAnnonceIDE(entreprise.getNumero(), protoAnnonceIDE);
							}
							else {
								final AnnonceIDE annonceIDE = (AnnonceIDE) serviceIDEService.synchroniseIDE(entreprise);
								results.addAnnonceIDE(entreprise.getNumero(), annonceIDE);
							}
							final Session session = sessionFactory.openSession();
							try {
								final SQLQuery query = session.createSQLQuery("update TIERS set IDE_DIRTY = " + dialect.toBooleanValueString(false) + " where NUMERO = :id");
								query.setParameter("id", entreprise.getNumero());
								query.executeUpdate();

								session.flush();
							}
							finally {
								session.close();
							}
							return results;
						}
						catch (ServiceIDEException e) {
							// On doit faire le ménage si un problème est survenu pendant l'envoi, sinon on croira qu'on a émis l'annonce alors que ce n'est pas le cas.
							status.setRollbackOnly();
							results.addErrorException(entreprise.getNumero(), e);
							return results;
						}
					}
				});
			}
			catch (Exception e) {
				results.addErrorException(entreprise.getNumero(), e);
			}

			return results;
	}
}
