package ch.vd.unireg.evenement.civil.engine.ech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamString;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.utils.UniregModeHelper;

public class EvenementCivilEchCorrectionDumpJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchCorrectionDumpJob.class);

	private static final String NAME = "EvenementCivilEchCorrectionDumpJob";
	private static final String EVTS = "EVTS";
	private static final String TEST_ANCIEN_HABITANT = "TEST_ANCIEN_HABITANT";

	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private List<IndividuComparisonStrategy> strategies;

	public EvenementCivilEchCorrectionDumpJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);

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
		return environnement.equals("Developpement");
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final String evtsString = getStringValue(params, EVTS);
		final boolean testAncienHabitant = getBooleanValue(params, TEST_ANCIEN_HABITANT);
		final String[] ids = evtsString.split("[^0-9]+");
		final File file = File.createTempFile("erreurs-correction-", ".csv");
		try (FileOutputStream fos = new FileOutputStream(file);
		     PrintStream out = new PrintStream(fos)) {
			LOGGER.info("Dump dans le fichier : " + file);
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					final Long idEvt = Long.parseLong(id);
					final IndividuApresEvenement evtAfter = serviceCivil.getIndividuAfterEvent(idEvt);
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
							final IndividuApresEvenement evtAfterRef = serviceCivil.getIndividuAfterEvent(idEvtRef);
							checkDiffs(idEvt, evtAfterRef, evtAfter, testAncienHabitant, out);
						}
					}
				}
			}
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
					final Mutable<String> champ = new MutableObject<>();
					strategy.isFiscalementNeutre(originel, correction, champ);
					if (StringUtils.isNotBlank(champ.getValue())) {
						log(idEvt, correction.getTypeEvenement(), champ.getValue(), out);
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
		final List<IndividuComparisonStrategy> strategies = new ArrayList<>();
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
