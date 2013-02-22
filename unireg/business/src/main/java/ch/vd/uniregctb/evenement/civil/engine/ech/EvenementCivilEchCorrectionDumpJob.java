package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamString;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.utils.UniregModeHelper;

public class EvenementCivilEchCorrectionDumpJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchCorrectionDumpJob.class);

	private static final String NAME = "EvenementCivilEchCorrectionDumpJob";
	private static final String CATEGORY = "Events";

	private static final String EVTS = "EVTS";
	private static final String TEST_ANCIEN_HABITANT = "TEST_ANCIEN_HABITANT";

	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private List<IndividuComparisonStrategy> strategies;

	public EvenementCivilEchCorrectionDumpJob(int sortOrder, String description) {
		super(NAME, CATEGORY, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénement(s)");
			param.setName(EVTS);
			param.setMandatory(true);
			param.setType(new JobParamString());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Test ancien habitant");
			param.setName(TEST_ANCIEN_HABITANT);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		strategies = buildStrategies(serviceInfra);
	}

	@Override
	public boolean isVisible() {
		final String environnement = UniregModeHelper.getEnvironnement();
		return environnement.equals("Developpement") || environnement.equals("Standalone");
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final String evtsString = getStringValue(params, EVTS);
		final boolean testAncienHabitant = getBooleanValue(params, TEST_ANCIEN_HABITANT);
		final String[] ids = evtsString.split("[^0-9]+");
		final File file = File.createTempFile("erreurs-correction-", ".csv");
		final PrintStream out = new PrintStream(new FileOutputStream(file));
		try {
			LOGGER.info("Dump dans le fichier : " + file);
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					final Long idEvt = Long.parseLong(id);
					final IndividuApresEvenement evtAfter = serviceCivil.getIndividuFromEvent(idEvt);
					if (evtAfter == null) {
						log(idEvt, null, "Evénement inconnu", out);
					}
					else if (evtAfter.getActionEvenement() != ActionEvenementCivilEch.CORRECTION) {
						log(idEvt, evtAfter.getTypeEvenement(), "Pas une correction", out);
					}
					else {
						final Long idEvtRef = evtAfter.getIdEvenementRef();
						if (idEvtRef == null) {
							log(idEvt, evtAfter.getTypeEvenement(), "Pas d'événement de référence", out);
						}
						else {
							final IndividuApresEvenement evtAfterRef = serviceCivil.getIndividuFromEvent(idEvtRef);
							checkDiffs(idEvt, evtAfterRef, evtAfter, testAncienHabitant, out);
						}
					}
				}
			}
		}
		finally {
			out.close();
		}
	}

	private void log(long idEvt, TypeEvenementCivilEch type, String logMessage, PrintStream out) {
		out.println(String.format("%d;%s;%s", idEvt, type, logMessage));
	}

	private void checkDiffs(long idEvt, IndividuApresEvenement originel, IndividuApresEvenement correction, boolean testAncienHabitant, PrintStream out) {
		try {
			if (testAncienHabitant && isAncienHabitant(correction.getIndividu().getNoTechnique())) {
				log(idEvt, correction.getTypeEvenement(), "ancien habitant", out);
			}
			else {
				for (IndividuComparisonStrategy strategy : strategies) {
					final DataHolder<String> champ = new DataHolder<String>();
					strategy.isFiscalementNeutre(originel, correction, champ);
					if (StringUtils.isNotBlank(champ.get())) {
						log(idEvt, correction.getTypeEvenement(), champ.get(), out);
					}
				}
			}
		}
		catch (Exception e) {
			log(idEvt, correction.getTypeEvenement(), "EXCEPTION: " + e.getMessage(), out);
		}
	}

	private boolean isAncienHabitant(final long noIndividu) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				return pp != null && !pp.isHabitantVD();
			}
		});
	}

	private static List<IndividuComparisonStrategy> buildStrategies(ServiceInfrastructureService serviceInfrastructureService) {
		final List<IndividuComparisonStrategy> strategies = new ArrayList<IndividuComparisonStrategy>();
		strategies.add(new AdresseContactComparisonStrategy());
		strategies.add(new AdresseResidencePrincipaleComparisonStrategy(serviceInfrastructureService));
		strategies.add(new AdresseResidenceSecondaireComparisonStrategy(serviceInfrastructureService));
		strategies.add(new DateDecesComparisonStrategy());
		strategies.add(new DateNaissanceComparisonStrategy());
		strategies.add(new EtatCivilComparisonStrategy());
		strategies.add(new NationaliteComparisonStrategy());
		strategies.add(new PermisComparisonStrategy());
		strategies.add(new RelationsComparisonStrategy());
		return strategies;
	}
}
