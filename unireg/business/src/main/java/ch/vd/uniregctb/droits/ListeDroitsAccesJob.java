package ch.vd.uniregctb.droits;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ListeDroitsAccesRapport;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * [SIFISC-7956] Job qui produit la liste des droits d'accès sur les dossiers protégés
 */
public class ListeDroitsAccesJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(ListeDroitsAccesJob.class);

	public static final String NAME = "ListeDroitsAccesJob";
	public static final String CATEGORIE = "Stats";
	public static final String DATE_VALEUR = "DATE_VALEUR";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceSecuriteService securiteService;
	private RapportService rapportService;

	public ListeDroitsAccesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date valeur");
		param.setName(DATE_VALEUR);
		param.setMandatory(true);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
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
				return hibernateTemplate.find("select distinct da.id from DroitAcces da where da.annulationDate is null order by da.tiers.id asc");
			}
		});

		final ListeDroitsAccesResults rapportFinal = new ListeDroitsAccesResults(dateValeur, tiersService, adresseService);

		// on résoud les noms, prénom, oid, adresses, opérateurs, ... des droits d'accès
		final BatchTransactionTemplate<Number, ListeDroitsAccesResults> t =
				new BatchTransactionTemplate<Number, ListeDroitsAccesResults>(daIds, 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, hibernateTemplate);
		t.setReadonly(true);
		t.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Number, ListeDroitsAccesResults>() {
			@Override
			public ListeDroitsAccesResults createSubRapport() {
				return new ListeDroitsAccesResults(dateValeur, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(final List<Number> batch, ListeDroitsAccesResults rapport) throws Exception {
				//noinspection unchecked
				final List<DroitAcces> list = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery("from DroitAcces da where da.id in (:ids) order by da.tiers.id asc");
						query.setParameterList("ids", batch);
						return query.list();
					}
				});
				for (DroitAcces da : list) {
					if (!da.isValidAt(dateValeur)) {
						continue;
					}
					statusManager.setMessage("Traitement du contribuable n°" + da.getTiers().getNumero(), percent);
					final AdresseEnvoiDetaillee adresseEnvoi = getAdresseDomicile(da.getTiers());
					final Integer oid = tiersService.getOfficeImpotIdAt(da.getTiers(), dateValeur);
					final Operateur operateur = securiteService.getOperateur(da.getNoIndividuOperateur());
					rapport.addDroitAcces(da.getTiers().getNumero(), oid, adresseEnvoi, da.getType(), da.getNiveau(), operateur);
				}
				return true;
			}

			private AdresseEnvoiDetaillee getAdresseDomicile(PersonnePhysique tiers) {
				try {
					return adresseService.getAdresseEnvoi(tiers, dateValeur, TypeAdresseFiscale.DOMICILE, false);
				}
				catch (Exception e) {
					LOGGER.error("Exception dans le calcul de l'adresse du tiers n°" + tiers.getNumero(), e);
					AdresseEnvoiDetaillee a = new AdresseEnvoiDetaillee(tiers, null, null, null, true);
					a.addNomPrenom(new NomPrenom("#erreur : " + e.getMessage(), ""));
					a.addLine("#erreur : " + e.getMessage());
					return a;
				}
			}
		});

		rapportFinal.setInterrompu(statusManager.interrupted());
		rapportFinal.end();

		final ListeDroitsAccesRapport rapport = rapportService.generateRapport(rapportFinal, statusManager);

		setLastRunReport(rapport);
		Audit.success("La production de la liste des droits d'accès au " + RegDateHelper.dateToDisplayString(dateValeur) + " est terminée.", rapport);
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
