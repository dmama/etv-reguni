package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamString;
import ch.vd.uniregctb.tiers.Contribuable;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DumpAssujettissementsJob extends JobDefinition {

	//private final Logger LOGGER = Logger.getLogger(DumpAssujettissementsJob.class);

	public static final String NAME = "DumpAssujettissementsJob";
	private static final String CATEGORIE = "Debug";

	public static final String FILENAME = "FILENAME";

	private static final List<JobParam> params;

	static {
		params = new ArrayList<JobParam>();
		JobParam param0 = new JobParam();
		param0.setDescription("Fichier de sortie (local au serveur)");
		param0.setName(FILENAME);
		param0.setMandatory(true);
		param0.setType(new JobParamString());
		params.add(param0);
	}

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;

	public DumpAssujettissementsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		final String filename = (String) params.get(FILENAME);
		Assert.notNull(filename);

		// Chargement des ids des contribuables à processer
		statusManager.setMessage("Chargement des ids de tous les contribuables...");
		final List<Long> ids = getCtbIds(statusManager);

		FileWriter file = new FileWriter(filename);
		try {
			processAll(ids, file, statusManager);
		}
		finally {
			file.close();
		}

		Audit.success("Le batch de dump des assujettissements est terminé");
	}

	private void processAll(List<Long> ids, final FileWriter file, final StatusManager statusManager) {

		BatchTransactionTemplate<Long, BatchResults> template = new BatchTransactionTemplate<Long, BatchResults>(ids, 100, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, statusManager, hibernateTemplate);
		template.execute(new BatchTransactionTemplate.BatchCallback<Long, BatchResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, BatchResults rapport) throws Exception {

				statusManager.setMessage("Traitement du lot [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);
				for (Long id : batch) {
					file.write(String.valueOf(id) + ";");
					try {
						process(id, file);
					}
					catch (Exception e) {
						file.write("exception:" + e.getMessage() + "\n");
					}
				}
				return true;
			}
		});
	}

	private void process(Long id, FileWriter file) throws IOException {
		final Contribuable ctb = (Contribuable) hibernateTemplate.get(Contribuable.class, id);
		if (ctb == null) {
			file.write("contribuable not found\n");
			return;
		}

		final List<Assujettissement> list;
		try {
			list = Assujettissement.determine(ctb, null, true);
		}
		catch (AssujettissementException e) {
			file.write("assujettissement exception:" + e.getMessage() + "\n");
			return;
		}

		if (list == null || list.isEmpty()) {
			file.write("non-assujetti\n");
			return;
		}

		for (Assujettissement a : list) {
			file.write(a.getClass().getSimpleName() + "(" + a.getDateDebut() + "-" + a.getDateFin() + ");");
		}
		file.write("\n");
	}

	@SuppressWarnings("unchecked")
	private List<Long> getCtbIds(final StatusManager statusManager) {
		final List<Long> ids = hibernateTemplate.find("select cont.numero from Contribuable as cont order by cont.numero asc");
		statusManager.setMessage(String.format("%d contribuables trouvés", ids.size()));
		return ids;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
