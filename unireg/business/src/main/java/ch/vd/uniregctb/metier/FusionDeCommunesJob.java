package ch.vd.uniregctb.metier;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.FusionDeCommunesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * Job qui effectue les changements sur les fors fiscaux suite à une fusion de communes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class FusionDeCommunesJob extends JobDefinition {

	public static final String NAME = "FusionDeCommunesJob";
	private static final String CATEGORIE = "Fors";

	private static final String ANCIENNES_COMMUNES = "ANCIENNES_COMMUNES";
	private static final String NOUVELLE_COMMUNE = "NOUVELLE_COMMUNE";
	private static final String DATE_FUSION = "DATE_FUSION";

	private static final List<JobParam> params;

	private PlatformTransactionManager transactionManager;
	private MetierService metierService;
	private RapportService rapportService;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Numéros OFS des anciennes communes");
			param.setName(ANCIENNES_COMMUNES);
			param.setMandatory(true);
			param.setType(new JobParamString());
			params.add(param);
		}

		{
			JobParam param = new JobParam();
			param.setDescription("Nouvelle commune");
			param.setName(NOUVELLE_COMMUNE);
			param.setMandatory(true);
			param.setType(new JobParamCommune());
			params.add(param);
		}

		{
			JobParam param = new JobParam();
			param.setDescription("Date de fusion");
			param.setName(DATE_FUSION);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			params.add(param);
		}
	}

	public FusionDeCommunesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final Set<Integer> anciensNosOfs = getNosOfs(params, ANCIENNES_COMMUNES);
		final int nouveauNoOfs = getIntegerValue(params, NOUVELLE_COMMUNE);
		final RegDate dateFusion = getRegDateValue(params, DATE_FUSION);
		final RegDate dateTraitement = RegDate.get();

		final FusionDeCommunesResults results = metierService.fusionDeCommunes(anciensNosOfs, nouveauNoOfs, dateFusion, dateTraitement, getStatusManager());

		// Exécution du rapport dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		FusionDeCommunesRapport rapport = (FusionDeCommunesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		Audit.success("Le traitement de la fusion des communes est terminé.", rapport);
	}

	private Set<Integer> getNosOfs(HashMap<String, Object> params, String key) {
		final String string = (String) params.get(key);
		final String[] split = string.split("[ ,;]");
		final Set<Integer> numeros = new HashSet<Integer>();
		for (String n : split) {
			if (StringUtils.isNotEmpty(n)) {
				final Integer no = Integer.valueOf(n);
				numeros.add(no);
			}
		}
		return numeros;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
