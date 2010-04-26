package ch.vd.uniregctb.indexer.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	private static final List<JobParam> params;
	static {
		params = new ArrayList<JobParam>();
		JobParam param = new JobParam();
		param.setDescription("Force la mise-à-jour de tous les tiers");
		param.setName(FORCE_ALL);
		param.setMandatory(false);
		param.setType(new JobParamBoolean());
		params.add(param);
	}

	public void setIndexer(OfficeImpotIndexer indexer) {
		this.indexer = indexer;
	}

	public OfficeImpotIndexerJob(int sortOrder, HashMap<String, Object> defParams) {
		super(NAME, CATEGORIE, sortOrder, "Mettre-à-jour l'office d'impôt au niveau de chaque tiers", params, defParams);
	}

	@Override
	public void doExecute(HashMap<String, Object> params) throws Exception {

		boolean forceAll = false;
		if (params != null) {
			Boolean b = (Boolean) params.get(FORCE_ALL);
			if (b != null) {
				forceAll = b.booleanValue();
			}
		}

		if (forceAll) {
			indexer.indexTousLesTiers(getStatusManager());
		}
		else {
			indexer.indexTiersAvecOfficeImpotInconnu(getStatusManager());
		}
	}
}
