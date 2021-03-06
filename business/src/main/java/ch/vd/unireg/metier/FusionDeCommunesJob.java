package ch.vd.unireg.metier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.document.FusionDeCommunesRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamCommune;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.scheduler.JobParamString;

/**
 * Job qui effectue les changements sur les fors fiscaux suite à une fusion de communes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class FusionDeCommunesJob extends JobDefinition {

	public static final String NAME = "FusionDeCommunesJob";
	private static final String ANCIENNES_COMMUNES = "ANCIENNES_COMMUNES";
	private static final String NOUVELLE_COMMUNE = "NOUVELLE_COMMUNE";
	private static final String DATE_FUSION = "DATE_FUSION";

	private PlatformTransactionManager transactionManager;
	private MetierService metierService;
	private RapportService rapportService;

	public FusionDeCommunesJob(int sortOrder, String description) {
		super(NAME, JobCategory.FORS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Numéros OFS des anciennes communes");
			param.setName(ANCIENNES_COMMUNES);
			param.setMandatory(true);
			param.setType(new JobParamString());
			addParameterDefinition(param, null);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Nouvelle commune");
			param.setName(NOUVELLE_COMMUNE);
			param.setMandatory(true);
			param.setType(new JobParamCommune(JobParamCommune.TypeCommune.COMMUNE_CH));
			addParameterDefinition(param, null);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Date de fusion");
			param.setName(DATE_FUSION);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final Set<Integer> anciensNosOfs = getNosOfs(params, ANCIENNES_COMMUNES);
		final int nouveauNoOfs = getIntegerValue(params, NOUVELLE_COMMUNE);
		final RegDate dateFusion = getRegDateValue(params, DATE_FUSION);
		final RegDate dateTraitement = RegDate.get();

		final FusionDeCommunesResults results = metierService.fusionDeCommunes(anciensNosOfs, nouveauNoOfs, dateFusion, dateTraitement, getStatusManager());

		// Exécution du rapport dans une transaction.
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		FusionDeCommunesRapport rapport = template.execute(status -> {
			try {
				return rapportService.generateRapport(results, getStatusManager());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		setLastRunReport(rapport);
		audit.success("Le traitement de la fusion des communes est terminé.", rapport);
	}

	private Set<Integer> getNosOfs(Map<String, Object> params, String key) {
		final String string = getStringValue(params, key);
		final String[] split = string.split("[ ,;]");
		final Set<Integer> numeros = new HashSet<>();
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

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
