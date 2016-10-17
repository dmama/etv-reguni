package ch.vd.uniregctb.rf;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job qui rappoche les contribuables et les propriétaires fonciers, un rapport est généré à la fin du traitement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class RapprocherCtbRegistreFoncierJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapprocherCtbRegistreFoncierJob.class);

	public static final String NAME = "RapprocherCtbRegistreFoncierJob";

	public static final String LISTE_PROPRIO = "LISTE_PROPRIO";
	public static final String NB_THREADS = "NB_THREADS";

	private RegistreFoncierService registreFoncierService;
	private RapportService rapportService;

	public RapprocherCtbRegistreFoncierJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des propriétaires fonciers");
			param.setName(LISTE_PROPRIO);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final byte[] listeProprio = getFileContent(params, LISTE_PROPRIO);
		final int nbThreads = getIntegerValue(params, NB_THREADS);

		final StatusManager status = getStatusManager();
		final List<ProprietaireFoncier> listeProprietaireFoncier = extractProprioFromCSV(listeProprio, status);

		final RapprocherCtbResults results = registreFoncierService.rapprocherCtbRegistreFoncier(listeProprietaireFoncier, status, RegDate.get(), nbThreads);
		final RapprocherCtbRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("Le rapprochement des contribuables et des proprietaires du registre foncier est terminée.", rapport);
	}



	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	/**
	 * Extrait les ids d'un fichier CSV contenant des ids séparés par des virgules, des points-virgules ou des retours de ligne.
	 *
	 * @param csv
	 *            le contenu d'un fichier CSV
	 * @return une liste d'ids
	 * @throws UnsupportedEncodingException
	 */
	protected static List<ProprietaireFoncier> extractProprioFromCSV(byte[] csv, StatusManager status) throws UnsupportedEncodingException {


		final List<ProprietaireFoncier> listeProprio = new LinkedList<>();

		// On attend les colonnes suivantes (seule la première est réellement obligatoire dans le fichier, les autres colonnes sont parfois vides) :
		// 1. numéro RF -> chiffres
		// 2. nom
		// 3. prénom (celui-ci contient parfois un point-virgule mais pas de chiffres - comme dans "André; en liquidation" - donc on s'assure qu'il est bien inclu ici et pas dans le nom ni la date de naissance
		// 4. date de naissance au format DD.MM.YYYY ou DD/MM/YYYY (dates partielles autorisées)
		// 5. numéro de contribuable
		// 6. données ignorées...
        final Pattern p = Pattern.compile("^([0-9]+);([^;]*)?;([^0-9]*)?;([0-9./]+)?;([0-9]+)?(;.*)?");

		// on parse le fichier
        final String csvString = (csv != null ? new String(csv,"ISO-8859-1") : StringUtils.EMPTY);

		final String[] lines = csvString.split("[\n]");
        final int nombreProprio = lines.length;
        int proprietairesLus = 0;

		try (Scanner s = new Scanner(csvString)) {
			while (s.hasNextLine()) {

				final String line = s.nextLine();

				final int percent = (proprietairesLus * 100) / nombreProprio;
				status.setMessage("Chargement des propriétaires fonciers", percent);
				if (status.interrupted()) {
					break;
				}

				final Matcher m = p.matcher(line);

				// on a un numero du registre foncier
				if (m.matches()) {

					final Long numeroRegistreFoncier = Long.valueOf(m.group(1));
					final String nom = m.group(2);
					final String prenom = m.group(3);
					String stringDateNaissance = m.group(4);
					RegDate dateNaissance = null;
					if (StringUtils.isNotBlank(stringDateNaissance)) {
						if (stringDateNaissance.contains("/")) {
							stringDateNaissance = stringDateNaissance.replace("/", ".");
						}
						try {
							dateNaissance = RegDateHelper.displayStringToRegDate(stringDateNaissance, true);
						}
						catch (Exception e) {
							LOGGER.error(String.format("La date de naissance '%s' pour le propriétaire %d est incorrecte (elle sera ignorée) : %s", stringDateNaissance, numeroRegistreFoncier,
							                           e.getMessage()));
						}
					}
					final String noCtbString = m.group(5);
					final Long numeroContribuable = StringUtils.isBlank(noCtbString) ? null : Long.valueOf(noCtbString);
					listeProprio.add(new ProprietaireFoncier(numeroRegistreFoncier, nom, prenom, dateNaissance, numeroContribuable));

					++proprietairesLus;
				}
				else {
					LOGGER.error(String.format("La ligne '%s' ne correspond pas au format attendu, elle sera ignorée", line));
				}
			}
		}

		// tri de la liste par numéro du registre foncier
		Collections.sort(listeProprio, Comparator.comparingLong(ProprietaireFoncier::getNumeroRegistreFoncier));

		Audit.info("Nombre de propriétaires lus dans le fichier : " + proprietairesLus);
		return listeProprio;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}
}
