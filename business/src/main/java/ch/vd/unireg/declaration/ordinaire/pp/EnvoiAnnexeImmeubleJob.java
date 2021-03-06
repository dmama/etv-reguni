package ch.vd.unireg.declaration.ordinaire.pp;

import java.io.UnsupportedEncodingException;
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
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EnvoiAnnexeImmeubleRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamInteger;

/**
 * Job qui envoie à l'impression les Annexes imeubles de la Di de manière séparée
 */
public class EnvoiAnnexeImmeubleJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiAnnexeImmeubleJob.class);

	public static final String NAME = "EnvoiAnnexeImmeubleJob";
	public static final String LISTE_CTB = "LISTE_CTB";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_MAX = "NB_MAX";

	private DeclarationImpotService service;
	private RapportService rapportService;

	public EnvoiAnnexeImmeubleJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

		{
			final RegDate today = RegDate.get();
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, today.year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximum d'envois");
			param.setName(NB_MAX);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 100);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des contribuables");
			param.setName(LISTE_CTB);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}

	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();
		// Récupération des paramètres
		final int annee = getIntegerValue(params, PERIODE_FISCALE);
		final byte[] listeCtb = getFileContent(params, LISTE_CTB);
		final List<ContribuableAvecImmeuble> listeCtbAcvecImmeuble = extractCtbFromCSV(listeCtb, status, audit);
		final int nbMax = getIntegerValue(params, NB_MAX);
		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui
		final EnvoiAnnexeImmeubleResults results = service.envoyerAnnexeImmeubleEnMasse(annee, dateTraitement, listeCtbAcvecImmeuble, nbMax, status);
		final EnvoiAnnexeImmeubleRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		final StringBuilder builder = new StringBuilder();
		builder.append("L'envoi des annexes immeubles en masse pour l'année ");
		builder.append(annee);
		builder.append(" à la date du ");
		builder.append(RegDateHelper.dateToDisplayString(dateTraitement));

		builder.append(" est terminée.");
		audit.success(builder.toString(), rapport);
	}

	/**
	 * Extrait les numéros de contribuables et le nombre d'immeubles séparés par un point-virgule
	 *
	 * @param csv le contenu d'un fichier CSV
	 * @param status status manager
	 * @param audit
	 * @return une liste de ctb possédant un  ou plusieurs immeuble
	 * @throws java.io.UnsupportedEncodingException si l'encodage ISO-8859-1 n'est pas supporté par la JVM
	 */
	protected static List<ContribuableAvecImmeuble> extractCtbFromCSV(byte[] csv, StatusManager status, AuditManager audit) throws UnsupportedEncodingException {

		final List<ContribuableAvecImmeuble> listeCtb = new LinkedList<>();
		final Pattern p = Pattern.compile("^([0-9]+);([0-9]+)(;.*)?$");

		status.setMessage("Chargement du fichier d'entrée");

		// on parse le fichier
		final String csvString = csv != null ? new String(csv, "ISO-8859-1") : StringUtils.EMPTY;
		int ctbsLus = 0;
		try (Scanner s = new Scanner(csvString)) {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				if (status.isInterrupted()) {
					break;
				}

				final Matcher m = p.matcher(line);

				// on a un numero de ctb
				if (m.matches()) {
					final Long numeroCtb = Long.valueOf(m.group(1));
					final int nombreImmeubles = Integer.valueOf(m.group(2));
					listeCtb.add(new ContribuableAvecImmeuble(numeroCtb, nombreImmeubles));
					++ctbsLus;
				}
				else {
					LOGGER.warn(String.format("Ligne ignorée dans le fichier d'entrée : '%s'", line));
				}
			}
		}
		audit.info("Nombre de contribuables lus dans le fichier : " + ctbsLus);

		// tri dans l'ordre croissant des numéros de contribuables
		listeCtb.sort(Comparator.comparing(ContribuableAvecImmeuble::getNumeroContribuable));

		return listeCtb;
	}
}
