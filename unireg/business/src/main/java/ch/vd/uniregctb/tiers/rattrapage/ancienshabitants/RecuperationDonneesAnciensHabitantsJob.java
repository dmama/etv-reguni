package ch.vd.uniregctb.tiers.rattrapage.ancienshabitants;

import java.util.Map;

import ch.vd.uniregctb.document.RecuperationDonneesAnciensHabitantsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersService;

public class RecuperationDonneesAnciensHabitantsJob extends JobDefinition {

	private static final String NAME = "RecuperationDonneesAnciensHabitantsJob";
	private static final String CATEGORIE = "Database";

	private static final String NB_THREADS = "NB_THREADS";
	private static final String FORCE = "FORCE";
	private static final String NOMS_PARENTS = "NOMS_PARENTS";
	private static final String TOUS_PRENOMS = "TOUS_PRENOMS";
	private static final String NOM_NAISSANCE = "NOM_NAISSANCE";

	private TiersService tiersService;
	private RapportService rapportService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public RecuperationDonneesAnciensHabitantsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Ecrasement des anciennes valeurs");
			param.setName(FORCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Noms/prénoms des parents");
			param.setName(NOMS_PARENTS);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Tous les prénoms du contribuable");
			param.setName(TOUS_PRENOMS);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nom de naissance du contribuable");
			param.setName(NOM_NAISSANCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 8);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean force = getBooleanValue(params, FORCE);
		final boolean parents = getBooleanValue(params, NOMS_PARENTS);
		final boolean prenoms = getBooleanValue(params, TOUS_PRENOMS);
		final boolean nomNaissance = getBooleanValue(params, NOM_NAISSANCE);
		if (!parents && !prenoms && !nomNaissance) {
			throw new IllegalArgumentException("Les paramètres " + NOMS_PARENTS + ", " + NOM_NAISSANCE + " et " + TOUS_PRENOMS + " ne devraient pas être tous à 'false'.");
		}

		final RecuperationDonneesAnciensHabitantsResults results = tiersService.recupereDonneesSurAnciensHabitants(nbThreads, force, parents, prenoms, nomNaissance, getStatusManager());
		final RecuperationDonneesAnciensHabitantsRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
	}
}
