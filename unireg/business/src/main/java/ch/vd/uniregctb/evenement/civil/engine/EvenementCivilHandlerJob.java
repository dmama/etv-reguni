package ch.vd.uniregctb.evenement.civil.engine;

import java.util.Map;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchRetryProcessor;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementCivilHandlerJob";
	private static final String CATEGORIE = "Events";
	
	private static final String REGPP = "REGPP";
	private static final String ECH = "ECH";

	private EvenementCivilProcessor processorRegPP;
	private EvenementCivilEchRetryProcessor processorEch;

	public EvenementCivilHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements Reg-PP");
			param.setName(REGPP);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, true);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements e-CH");
			param.setName(ECH);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, true);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final boolean evtRegPP = getBooleanValue(params, REGPP);
		final boolean evtEch = getBooleanValue(params, ECH);

		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements civils
		if (evtRegPP) {
			status.setMessage("Traitement des événements Reg-PP...");
			processorRegPP.traiteEvenementsCivils(status);
		}
		if (evtEch) {
			status.setMessage("Traitement des événements e-CH...");
			processorEch.retraiteEvenements(status);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessorRegPP(EvenementCivilProcessor processorRegPP) {
		this.processorRegPP = processorRegPP;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessorEch(EvenementCivilEchRetryProcessor processorEch) {
		this.processorEch = processorEch;
	}
}
