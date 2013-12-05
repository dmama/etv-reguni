package ch.vd.uniregctb.indexer;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;

public class OptimizeIndexJob extends JobDefinition {

	// private final Logger LOGGER = Logger.getLogger(UpdateSequencesJob.class);

	public static final String NAME = "OptimizeIndexJob";
	private static final String CATEGORIE = "Indexeur";

	private GlobalIndex index;

	public OptimizeIndexJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		try {
			index.optimize();
			Audit.success("L'optimisation de l'index lucene est terminée.");
		}
		catch (Exception e) {
			Audit.error("Impossible d'optimiser l'index lucene pour la raison suivante: " + e.getMessage());
			throw e;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndex(GlobalIndex index) {
		this.index = index;
	}
}
