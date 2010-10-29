package ch.vd.uniregctb.indexer.jobs;

import java.util.Map;

import ch.vd.uniregctb.indexer.tiers.OfficeImpotIndexer;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

/**
 * Job qui mets-à-jour l'information de l'office d'impôt au niveau de chaque tiers
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class OfficeImpotIndexerJob extends JobDefinition {

	public static final String NAME = "OfficeImpotIndexerJob";
	private static final String CATEGORIE = "OID";

	public static final String FORCE_ALL = "FORCE_ALL";

	private OfficeImpotIndexer indexer;

	public void setIndexer(OfficeImpotIndexer indexer) {
		this.indexer = indexer;
	}

	public OfficeImpotIndexerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Force la mise-à-jour de tous les tiers");
		param.setName(FORCE_ALL);
		param.setMandatory(false);
		param.setType(new JobParamBoolean());
		addParameterDefinition(param, Boolean.FALSE);
	}

	@Override
	public void doExecute(Map<String, Object> params) throws Exception {

		final boolean forceAll = getBooleanValue(params, FORCE_ALL);
		if (forceAll) {
			indexer.indexTousLesTiers(getStatusManager());
		}
		else {
			indexer.indexTiersAvecOfficeImpotInconnu(getStatusManager());
		}
	}
}
