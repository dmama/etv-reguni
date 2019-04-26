package ch.vd.unireg.droits;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ListeDroitsAccesRapport;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * [SIFISC-7956] Job qui produit la liste des droits d'accès sur les dossiers protégés
 */
public class ListeDroitsAccesJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListeDroitsAccesJob.class);

	public static final String NAME = "ListeDroitsAccesJob";
	public static final String DATE_VALEUR = "DATE_VALEUR";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceSecuriteService securiteService;
	private RapportService rapportService;

	public ListeDroitsAccesJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date valeur");
		param.setName(DATE_VALEUR);
		param.setMandatory(true);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager statusManager = getStatusManager();
		final RegDate dateValeur = getRegDateValue(params, DATE_VALEUR);

		// on charge les ids de tous les droits d'accès existants
		statusManager.setMessage("Chargement de la liste des dossiers protégés...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Number> daIds = template.execute(new TxCallback<List<Number>>() {
			@Override
			public List<Number> execute(TransactionStatus status) throws Exception {
				//noinspection unchecked
				return hibernateTemplate.find("select da.id from DroitAcces da where da.annulationDate is null order by da.tiers.id asc", null);
			}
		});

		final ListeDroitsAccesResults rapportFinal = new ListeDroitsAccesResults(dateValeur, tiersService, adresseService);

		// on résoud les noms, prénom, oid, adresses, opérateurs, ... des droits d'accès
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Number, ListeDroitsAccesResults> t =
				new BatchTransactionTemplateWithResults<>(daIds, 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager);
		t.setReadonly(true);
		t.execute(rapportFinal, new BatchWithResultsCallback<Number, ListeDroitsAccesResults>() {
			@Override
			public ListeDroitsAccesResults createSubRapport() {
				return new ListeDroitsAccesResults(dateValeur, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(final List<Number> batch, ListeDroitsAccesResults rapport) throws Exception {
				final List<DroitAcces> list = hibernateTemplate.execute(new HibernateCallback<List<DroitAcces>>() {
					@Override
					public List<DroitAcces> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery("from DroitAcces da where da.id in (:ids) order by da.tiers.id asc");
						query.setParameterList("ids", batch);
						//noinspection unchecked
						return query.list();
					}
				});
				for (DroitAcces da : list) {
					if (!da.isValidAt(dateValeur)) {
						continue;
					}
					statusManager.setMessage("Traitement du contribuable n°" + da.getTiers().getNumero(), progressMonitor.getProgressInPercent());
					final AdresseEnvoiDetaillee adresseEnvoi = getAdresseDomicile(da.getTiers());
					final Contribuable porteurAssujettissement = findPorteurAssujettissement(da.getTiers(), dateValeur);
					final Contribuable ctb = porteurAssujettissement != null ? porteurAssujettissement : da.getTiers();
					final Integer oid = tiersService.getOfficeImpotIdAt(ctb, dateValeur);
					final String visa = da.getVisaOperateur();
					if (visa == null) {
						throw new IllegalArgumentException("Le visa du droit n°" + da.getId() + " est nul.");
					}
					Operateur operateur = securiteService.getOperateur(visa);
					if (operateur == null) {
						//SIFISC-26187 Pas d'opérateur trouvé, on créé un opérateur fantome pour l'affichage du message d'erreur dans le rapport
						final String msgErreur = String.format("Opérateur %s non trouvé dans host-interfaces", visa);
						operateur = new Operateur();
						operateur.setCode(visa);
						operateur.setNom("");
						operateur.setPrenom(msgErreur);

					}
					rapport.addDroitAcces(da.getTiers().getNumero(), oid, adresseEnvoi, da.getType(), da.getNiveau(), operateur);
				}
				return true;
			}

			private AdresseEnvoiDetaillee getAdresseDomicile(Contribuable tiers) {
				try {
					return adresseService.getAdresseEnvoi(tiers, dateValeur, TypeAdresseFiscale.DOMICILE, false);
				}
				catch (Exception e) {
					LOGGER.error("Exception dans le calcul de l'adresse du tiers n°" + tiers.getNumero(), e);
					AdresseEnvoiDetaillee a = adresseService.getDummyAdresseEnvoi(tiers);
					a.addNomPrenom(new NomPrenom("#erreur : " + e.getMessage(), ""));
					a.addLine("#erreur : " + e.getMessage());
					return a;
				}
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(statusManager.isInterrupted());
		rapportFinal.end();

		final ListeDroitsAccesRapport rapport = rapportService.generateRapport(rapportFinal, statusManager);

		setLastRunReport(rapport);
		audit.success("La production de la liste des droits d'accès au " + RegDateHelper.dateToDisplayString(dateValeur) + " est terminée.", rapport);
	}

	/**
	 * Permet de retrouver le ctb assujetti pour le contribuable passé en paramètre
	 * @param p le contribuable à analyser
	 * @param dateValeur la fate de référence;
	 * @return le contribuable assujetti ou null si aucun assujetissement n'a été trouvé
	 */
	private Contribuable findPorteurAssujettissement(Contribuable p, RegDate dateValeur){
		Assujettissement assujettissement = tiersService.getAssujettissement(p, dateValeur);
		if (assujettissement == null && p instanceof PersonnePhysique) {
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple((PersonnePhysique) p, dateValeur);
			if (ensembleTiersCouple != null) {
				assujettissement = tiersService.getAssujettissement(ensembleTiersCouple.getMenage(),dateValeur);
			}

		}
		return assujettissement != null ? assujettissement.getContribuable() : null;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSecuriteService(ServiceSecuriteService securiteService) {
		this.securiteService = securiteService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
