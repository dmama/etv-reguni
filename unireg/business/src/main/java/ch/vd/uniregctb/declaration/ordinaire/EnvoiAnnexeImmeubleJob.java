package ch.vd.uniregctb.declaration.ordinaire;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.EnvoiAnnexeImmeubleRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job qui envoie à l'impression les Annexes imeubles de la Di de manière séparée
 */
public class EnvoiAnnexeImmeubleJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;


	public static final String NAME = "EnvoiAnnexeImmeubleJob";
	private static final String CATEGORIE = "DI";
	public static final String LISTE_CTB = "LISTE_CTB";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_ANNEXE = "NB_ANNEXEE";


	public EnvoiAnnexeImmeubleJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

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
			param.setDescription("Nombre d'annexe");
			param.setName(NB_ANNEXE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
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
		final List<ContribuableAvecImmeuble> listeCtbAcvecImmeuble = extractCtbFromCSV(listeCtb,status);
		final int nbMax = getIntegerValue(params, NB_ANNEXE);
		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui
		final EnvoiAnnexeImmeubleResults results = service.envoyerAnnexeImmeubleEnMasse(annee,dateTraitement,listeCtbAcvecImmeuble,nbMax, status);
		final EnvoiAnnexeImmeubleRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		final StringBuilder builder = new StringBuilder();
		builder.append("L'envoi des annexes immeubles en masse ");
		builder.append("pour l'année ");
		builder.append(annee);
		builder.append(" à la date du ");
		builder.append(RegDateHelper.dateToDisplayString(dateTraitement));

		builder.append(" est terminée.");
		Audit.success(builder.toString());
		Audit.success(builder.toString(), rapport);
	}

	/**
	 * Extrait les numéros de contribuables et le nombre d'immeubles séparés par unpoint virgule
	 *
	 * @param csv
	 *            le contenu d'un fichier CSV
	 * @return une liste de ctb possédant un  ou plusieurs immeuble
	 * @throws java.io.UnsupportedEncodingException
	 */
	protected static List<ContribuableAvecImmeuble> extractCtbFromCSV(byte[] csv, StatusManager status) throws UnsupportedEncodingException {


		final List<ContribuableAvecImmeuble> listeCtb = new ArrayList<ContribuableAvecImmeuble>();
        final Pattern p = Pattern.compile("^([0-9]+);([0-9]+)");

		// on parse le fichier
        final String csvString = new String(csv,"ISO-8859-1");
		Scanner s = new Scanner(csvString);

        final String[] lines = csvString.split("[\n]");
        final int nombrectb = lines.length;
        int CtbLu = 0;

		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				  //Audit.info("nombre de propriétaire tratés "+CtbLu);
                int percent = (CtbLu * 100) / nombrectb;
                status.setMessage("Chargement des contribuables propriétaires d'immeuble",percent);
                if (status.interrupted()) {
                	break;
                }

				Matcher m = p.matcher(line);

				// on a un numero de ctb
				if (m.matches()) {

					Long numeroCtb = null;
					int nombreImmeuble;


					numeroCtb = Long.valueOf(m.group(1));

					nombreImmeuble = Integer.valueOf(m.group(2));

					listeCtb.add(new ContribuableAvecImmeuble(numeroCtb, nombreImmeuble));
                    CtbLu++;
				}

			}
		}
		finally {
			s.close();
		}
		  Audit.info("nombre de contribuable lus dans le fichier :"+CtbLu);
		return listeCtb;
	}

}
