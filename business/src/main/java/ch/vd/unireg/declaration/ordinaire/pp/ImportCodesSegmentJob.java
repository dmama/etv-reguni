package ch.vd.unireg.declaration.ordinaire.pp;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.ImportCodesSegmentRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;

public class ImportCodesSegmentJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportCodesSegmentJob.class);

	private static final String NAME = "ImportCodesSegmentJob";
	private static final String INPUT_FILE = "INPUT_FILE";

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public ImportCodesSegmentJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des codes segment");
			param.setName(INPUT_FILE);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final byte[] input = getFileContent(params, INPUT_FILE);
		final StatusManager statusManager = getStatusManager();
		final MutableInt lignesLues = new MutableInt(0);
		final List<ContribuableAvecCodeSegment> inputData = buildDataFromInputFile(input, statusManager, lignesLues, audit);
		final ImportCodesSegmentResults res = diService.importerCodesSegment(inputData, statusManager);
		final ImportCodesSegmentRapport rapport = rapportService.generateRapport(res, lignesLues.intValue(), statusManager);
		setLastRunReport(rapport);
		audit.success("L'import des codes segment est terminé", rapport);
	}

	/**
	 * @param csv contenu du fichier d'entrée
	 * @param status status manager pour les messages de progression
	 * @param nbrLignesLues si non-null, servira de réceptacle en sortie du nombre de lignes lues dans le fichier fourni
	 * @param audit
	 * @return la liste des informations lisibles dans le fichier d'entrée, doublons éliminés, triée par ordre croissant du numéro de contribuable
	 * @throws UnsupportedEncodingException si l'encoding ISO-8859-1 n'est pas supportés par la JVM
	 */
	protected static List<ContribuableAvecCodeSegment> buildDataFromInputFile(byte[] csv, StatusManager status, @Nullable MutableInt nbrLignesLues, AuditManager audit) throws UnsupportedEncodingException {
		final List<ContribuableAvecCodeSegment> liste = new LinkedList<>();
		final Pattern p = Pattern.compile("^([0-9]+);([0-9]+)(;.*)?$");

		status.setMessage("Chargement du fichier d'entrée");

		// on parse le fichier
		final String csvString = csv != null ? new String(csv, "ISO-8859-1") : StringUtils.EMPTY;
		int lignesCtbLues = 0;
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
					final int codeSegment = Integer.valueOf(m.group(2));
					liste.add(new ContribuableAvecCodeSegment(numeroCtb, codeSegment));
					++lignesCtbLues;
				}
				else {
					LOGGER.warn(String.format("Ligne ignorée dans le fichier d'entrée : '%s'", line));
				}
			}
		}
		audit.info("Nombre de contribuables lus dans le fichier : " + lignesCtbLues);

		// élimination des doublons
		final Set<ContribuableAvecCodeSegment> sansDoublons = new HashSet<>(liste);
		audit.info("Nombre de contribuables uniques présents dans le fichier : " + sansDoublons.size());

		// récupération dans une liste et tri dans l'ordre croissant des numéros de contribuables
		final List<ContribuableAvecCodeSegment> listeSansDoublons = new ArrayList<>(sansDoublons);
		listeSansDoublons.sort(Comparator.comparing(ContribuableAvecCodeSegment::getNoContribuable));

		if (nbrLignesLues != null) {
			nbrLignesLues.setValue(lignesCtbLues);
		}
		return listeSansDoublons;
	}
}
