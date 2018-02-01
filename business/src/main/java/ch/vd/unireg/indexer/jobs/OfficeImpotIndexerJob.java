package ch.vd.unireg.indexer.jobs;

import java.util.Map;

import ch.vd.unireg.indexer.tiers.OfficeImpotIndexer;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;

/**
 * Job qui mets-à-jour l'information de l'office d'impôt au niveau de chaque tiers
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class OfficeImpotIndexerJob extends JobDefinition {

	public static final String NAME = "OfficeImpotIndexerJob";

	public static final String FORCE_ALL = "FORCE_ALL";

	private OfficeImpotIndexer indexer;

	public void setIndexer(OfficeImpotIndexer indexer) {
		this.indexer = indexer;
	}

	public OfficeImpotIndexerJob(int sortOrder, String description) {
		super(NAME, JobCategory.OID, sortOrder, description);

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
