package ch.vd.uniregctb.indexer;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class OptimizeIndexJob extends JobDefinition {

	// private final Logger LOGGER = LoggerFactory.getLogger(UpdateSequencesJob.class);

	public static final String NAME = "OptimizeIndexJob";

	private Map<String, GlobalIndex> indexes;

	public OptimizeIndexJob(int sortOrder, String description) {
		super(NAME, JobCategory.INDEXEUR, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		for (Map.Entry<String, GlobalIndex> entry : indexes.entrySet()) {
			try {
				entry.getValue().optimize();
				Audit.success(String.format("L'optimisation de l'index lucene '%s' est terminée.", entry.getKey()));
			}
			catch (Exception e) {
				Audit.error(String.format("Impossible d'optimiser l'index lucene '%s' pour la raison suivante: ", entry.getKey()) + e.getMessage());
				throw e;
			}
		}
	}

	public void setIndexes(Map<String, GlobalIndex> indexes) {
		this.indexes = indexes;
	}
}
