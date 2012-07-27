package ch.vd.uniregctb.identification.individus;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuImpl;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.IdentificationIndividusNonMigresRapport;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

public class IdentificationIndividusNonMigresJob extends JobDefinition implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(IdentificationIndividusNonMigresJob.class);

	private static final String NAME = "IdentificationIndividusNonMigres";
	private static final String CATEGORIE = "Stats";
	private static final String PARAM_MIGRATION_NH = "MigrationNH";
	private static final EnumAttributeIndividu[] ATTRIBUTE_INDIVIDUS = new EnumAttributeIndividu[]{EnumAttributeIndividu.ADRESSES, EnumAttributeIndividu.PERMIS, EnumAttributeIndividu.NATIONALITE};

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private GlobalTiersSearcher tiersSearcher;
	private ServiceCivil regppClient;
	private RcPersClient rcPersClient;
	private RapportService rapportService;
	private ServiceInfrastructureRaw infraService;

	private boolean migrationNH;

	private final List<StrategieIdentification> strategies = new ArrayList<StrategieIdentification>();


	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	public void setRegppClient(ServiceCivil regppClient) {
		this.regppClient = regppClient;
	}

	public void setRcPersClient(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setInfraService(ServiceInfrastructureRaw infraService) {
		this.infraService = infraService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		strategies.add(new IdentificationParNoAVS13(rcPersClient));
		strategies.add(new IdentificationParPrenomNomEtDateNaissance(rcPersClient));
		strategies.add(new IdentificationParPrenomEtNom(rcPersClient));
	}

	public IdentificationIndividusNonMigresJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
		final JobParam param = new JobParam();
		param.setDescription("Générer la liste des contribuables à migrer en Non-Habitant");
		param.setName(PARAM_MIGRATION_NH);
		param.setMandatory(true);
		param.setType(new JobParamBoolean());
		addParameterDefinition(param, Boolean.FALSE);
	}

	private static class Ids {
		private long ctbId;
		private long noInd;

		private Ids(long ctbId, long noInd) {
			this.ctbId = ctbId;
			this.noInd = noInd;
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();
		statusManager.setMessage("Recherche des personnes physiques non-indexées...");
		// final List<Ids> ids = getIdsDebug();
		final List<Ids> ids = getIds();

		migrationNH = getBooleanValue(params, PARAM_MIGRATION_NH);

		final IdentificationIndividusNonMigresResults results;
		results = new IdentificationIndividusNonMigresResults(RegDate.get(), ids.size(), migrationNH);

		for (int i = 0, idsSize = ids.size(); i < idsSize; i++) {
			final Ids id = ids.get(i);
			if (statusManager.interrupted()) {
				results.interrompu = true;
				break;
			}
			try {
				statusManager.setMessage("Traitement du contribuable n°" + FormatNumeroHelper.numeroCTBToDisplay(id.ctbId), (i * 100) / idsSize);
				identifieIndividu(results, id);
			}
			catch (RuntimeException e) {
				LOGGER.error(e, e);
				results.addException(id.ctbId, id.noInd, e);
			}
		}
		results.end();

		statusManager.setMessage("Génération du rapport...");
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final IdentificationIndividusNonMigresRapport rapport = t.execute(new TransactionCallback<IdentificationIndividusNonMigresRapport>() {
			@Override
			public IdentificationIndividusNonMigresRapport doInTransaction(TransactionStatus s) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		setLastRunReport(rapport);
	}

	private void identifieIndividu(IdentificationIndividusNonMigresResults results, Ids id) {
		// on récupère l'individu dans RegPP
		final Individu indRegPP = getIndividuRegPP(id.noInd);
		if (indRegPP == null) {
			results.addError(id.ctbId, id.noInd, IdentificationIndividusNonMigresResults.ErreurType.INDIVIDU_REGPP_INCONNU);
			return;
		}

		final IdentificationIndividu identRegPP;
		if (migrationNH) {
			identRegPP = new IdentificationIndividuMigrationNH(indRegPP);
		} else {
			identRegPP = new IdentificationIndividu(indRegPP);
		}

		if (StringUtils.isBlank(identRegPP.nom)) {
			results.addError(id.ctbId, identRegPP, IdentificationIndividusNonMigresResults.ErreurType.INDIVIDU_REGPP_NOM_VIDE);
			return;
		}

		// on essaie de récupèrer l'individu dans RcPers
		final Individu indRcPers = getIndividuRcPers(id.noInd);
		if (indRcPers != null) {
			// L'individu existe dans RcPers, il n'y a pas été indexés pour une autre raison (faux positif), on ignore
			results.addIgnore(id.ctbId, identRegPP, new IdentificationIndividu(indRcPers), IdentificationIndividusNonMigresResults.IgnoreType.INDIVIDU_RCPERS_CONNU);
			return;
		}

		// on recherche l'individu dans RcPers
		Long noIndIdentifie = null;
		String nomStrategie = null;
		for (StrategieIdentification strategy : strategies) {

			final List<Long> list = strategy.identifieIndividuRcPers(indRegPP);
			if (list.size() == 1) {
				// trouvé => on sort
				noIndIdentifie = list.get(0);
				nomStrategie = strategy.getNom();
				break;
			}

			if (list.size() > 1) { // plusieurs candidats => terminé
				results.addNonIdentifie(id.ctbId, identRegPP, IdentificationIndividusNonMigresResults.NonIdentificationType.PLUSIEURS_INDIVIDUS_RCPERS, "Individus trouvés = " + Arrays.toString(list.toArray()));
				return;
			}
		}

		if (noIndIdentifie == null) {
			// pas trouvé => terminé
			results.addNonIdentifie(id.ctbId, identRegPP, IdentificationIndividusNonMigresResults.NonIdentificationType.AUCUN_INDIVIDU_RCPERS);
			return;
		}

		// on récupère l'individu dans RcPers
		final Individu indRcPersIdentifie = getIndividuRcPers(noIndIdentifie);
		if (indRcPersIdentifie == null) {
			results.addError(id.ctbId, identRegPP, noIndIdentifie, IdentificationIndividusNonMigresResults.ErreurType.INDIVIDU_RCPERS_INCONNU);
			return;
		}
		final IdentificationIndividu identRcPers = new IdentificationIndividu(indRcPersIdentifie);

		// on vérifie si l'individu est déjà associé à un contribuable
		final List<Long> pp = getPPIdsByIndividu(noIndIdentifie);
		final String remarque = (pp == null || pp.isEmpty()) ? null : "Une ou plusieurs personnes physiques sont déjà liées avec l'individu identifié.";

		// ok, on a identifié l'individu
		results.addIdentifie(id.ctbId, identRegPP, identRcPers, nomStrategie, remarque, pp);
	}

	private List<Long> getPPIdsByIndividu(final long noIndIdentifie) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Long> doInTransaction(TransactionStatus transactionStatus) {
				return hibernateTemplate.find("select pp.numero from PersonnePhysique as pp where pp.numeroIndividu = " + noIndIdentifie);
			}
		});
	}

	private Individu getIndividuRegPP(long noInd) {

		// copié-collé de la méthode ServiceCivilHostInterfaces.getIndividu() adaptée pour les circonstances (et parce qu'on veut un accès direct au service)

		try {
			ch.vd.registre.civil.model.Individu ind;
			if (migrationNH) {
				ind = regppClient.getIndividu(noInd, 2400, ATTRIBUTE_INDIVIDUS);
			} else {
				ind = regppClient.getIndividu(noInd, 2400);
			}
			return IndividuImpl.get(ind, null);
		}
		catch (RemoteException e) {
			throw new ServiceCivilException(e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException(e);
		}
	}

	private Individu getIndividuRcPers(long noInd) {

		// copié-collé de la méthode ServiceCivilRCPers.getIndividu() adaptée pour les circonstances (et parce qu'on veut un accès direct au service)

		final ListOfPersons list = rcPersClient.getPersons(Arrays.asList(noInd), null, false);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu = " + noInd);
		}

		final Person person = list.getListOfResults().getResult().get(0).getPerson();
		final Individu individu = IndividuRCPers.get(person, null, true, infraService);
		if (individu != null) {
			long actual = individu.getNoTechnique();
			if (noInd != actual) {
				throw new IllegalArgumentException(String.format(
						"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noInd, actual));
			}
		}

		return individu;
	}

	private List<Ids> getIdsDebug() {

		final List<Ids> ids = new ArrayList<Ids>();

		// identifié 1 tiers
		ids.add(new Ids(10040161,771090));
		ids.add(new Ids(10130406,140789));
		ids.add(new Ids(10141458,158619));
		ids.add(new Ids(10141481,158652));
		ids.add(new Ids(10140694,157391));
		ids.add(new Ids(10140715,157431));
		ids.add(new Ids(10151175,174418));
		ids.add(new Ids(10150952,174042));
		ids.add(new Ids(10130763,141320));

		// identifiés, plusieurs tiers
		ids.add(new Ids(10240133,323500));

		// identifiés, sans tiers
		ids.add(new Ids(10290654,406971));

		// non identifies
		ids.add(new Ids(10141031,158013));
		ids.add(new Ids(10117276,913936));
		ids.add(new Ids(10124419,130883));
		ids.add(new Ids(10118285,917271));
		ids.add(new Ids(10125050,131955));
		ids.add(new Ids(10053706,840973));
		ids.add(new Ids(10041804,349348));
		ids.add(new Ids(10076570,849796));
		ids.add(new Ids(10128237,137299));
		ids.add(new Ids(10171312,207499));
		ids.add(new Ids(10171313,207500));
		ids.add(new Ids(10171315,207502));
		ids.add(new Ids(10149026,170503));
		ids.add(new Ids(10158635,186604));
		ids.add(new Ids(10057099,820658));
		ids.add(new Ids(10056998,819975));
		ids.add(new Ids(10171617,207970));
		ids.add(new Ids(10172201,208870));

		return ids;

	}

	/**
	 * @return les numéros de tiers des personnes physiques qui possèdent un numéro d'individu et qui ne sont pas indexées.
	 */
	@SuppressWarnings("unchecked")
	private List<Ids> getIds() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Ids> idsDb = template.execute(new TxCallback<List<Ids>>() {
			@Override
			public List<Ids> execute(TransactionStatus transactionStatus) throws Exception {
				final List<Ids> ids = new ArrayList<Ids>();
				final List lines = hibernateTemplate.find("select pp.numero, pp.numeroIndividu from PersonnePhysique as pp where pp.numeroIndividu is not null");
				for (Object o : lines) {
					final Object line[] = (Object[]) o;
					final Number ctbId = (Number) line[0];
					final Number noInd = (Number) line[1];
					ids.add(new Ids(ctbId.longValue(), noInd.longValue()));
				}
				return ids;
			}
		});

		final Set<Long> idsIndex = tiersSearcher.getAllIds();

		final List<Ids> ids = new ArrayList<Ids>();
		for (Ids key : idsDb) {
			if (!idsIndex.contains(key.ctbId)) {
				ids.add(key);
			}
		}
		return ids;
	}
}



