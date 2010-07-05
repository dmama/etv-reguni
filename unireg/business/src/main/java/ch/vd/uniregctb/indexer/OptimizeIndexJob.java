package ch.vd.uniregctb.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;

public class OptimizeIndexJob extends JobDefinition {

	// private final Logger LOGGER = Logger.getLogger(UpdateSequencesJob.class);

	public static final String NAME = "OptimizeIndexJob";
	private static final String CATEGORIE = "Indexeur";

	private static final List<JobParam> params;
	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		defaultParams = new HashMap<String, Object>();
	}

	private GlobalIndex index;

	public OptimizeIndexJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
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
