package ch.vd.uniregctb.declaration.ordinaire;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ImportCodesSegmentRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;

public class ImportCodesSegmentJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(ImportCodesSegmentJob.class);

	private static final String NAME = "ImportCodesSegmentJob";
	private static final String CATEGORIE = "DI";
	private static final String INPUT_FILE = "INPUT_FILE";

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public ImportCodesSegmentJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

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
		final List<ContribuableAvecCodeSegment> inputData = buildDataFromInputFile(input, statusManager, lignesLues);
		final ImportCodesSegmentResults res = diService.importerCodesSegment(inputData, statusManager);
		final ImportCodesSegmentRapport rapport = rapportService.generateRapport(res, lignesLues.intValue(), statusManager);
		setLastRunReport(rapport);
		Audit.success("L'import des codes segment est terminé", rapport);
	}

	/**
	 * @param csv contenu du fichier d'entrée
	 * @param status status manager pour les messages de progression
	 * @param nbrLignesLues si non-null, servira de réceptacle en sortie du nombre de lignes lues dans le fichier fourni
	 * @return la liste des informations lisibles dans le fichier d'entrée, doublons éliminés, triée par ordre croissant du numéro de contribuable
	 * @throws UnsupportedEncodingException si l'encoding ISO-8859-1 n'est pas supportés par la JVM
	 */
	protected static List<ContribuableAvecCodeSegment> buildDataFromInputFile(byte[] csv, StatusManager status, @Nullable MutableInt nbrLignesLues) throws UnsupportedEncodingException {
		final List<ContribuableAvecCodeSegment> liste = new LinkedList<ContribuableAvecCodeSegment>();
		final Pattern p = Pattern.compile("^([0-9]+);([0-9]+)(;.*)?$");

		status.setMessage("Chargement du fichier d'entrée");

		// on parse le fichier
		final String csvString = csv != null ? new String(csv, "ISO-8859-1") : StringUtils.EMPTY;
		int lignesCtbLues = 0;
		final Scanner s = new Scanner(csvString);
		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				if (status.interrupted()) {
					break;
				}

				final Matcher m = p.matcher(line);

				// on a un numero de ctb
				if (m.matches()) {
					final Long numeroCtb = Long.valueOf(m.group(1));
					final int codeSegment = Integer.valueOf(m.group(2));
					liste.add(new ContribuableAvecCodeSegment(numeroCtb, codeSegment));
					++ lignesCtbLues;
				}
				else {
					LOGGER.warn(String.format("Ligne ignorée dans le fichier d'entrée : '%s'", line));
				}
			}
		}
		finally {
			s.close();
		}
		Audit.info("Nombre de contribuables lus dans le fichier : " + lignesCtbLues);

		// élimination des doublons
		final Set<ContribuableAvecCodeSegment> sansDoublons = new HashSet<ContribuableAvecCodeSegment>(liste);
		Audit.info("Nombre de contribuables uniques présents dans le fichier : " + sansDoublons.size());

		// récupération dans une liste et tri dans l'ordre croissant des numéros de contribuables
		final List<ContribuableAvecCodeSegment> listeSansDoublons = new ArrayList<ContribuableAvecCodeSegment>(sansDoublons);
		Collections.sort(listeSansDoublons, new Comparator<ContribuableAvecCodeSegment>() {
			@Override
			public int compare(ContribuableAvecCodeSegment o1, ContribuableAvecCodeSegment o2) {
				return o1.getNoContribuable() < o2.getNoContribuable() ? -1 : (o1.getNoContribuable() > o2.getNoContribuable() ? 1 : 0);
			}
		});

		if (nbrLignesLues != null) {
			nbrLignesLues.setValue(lignesCtbLues);
		}
		return listeSansDoublons;
	}
}
