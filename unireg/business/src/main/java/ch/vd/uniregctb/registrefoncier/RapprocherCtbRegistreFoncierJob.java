package ch.vd.uniregctb.registrefoncier;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;

/**
 * Job qui rappoche les contribuables et les propriétaires fonciers, un rapport est généré à la fin du traitement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class RapprocherCtbRegistreFoncierJob extends JobDefinition {

	public static final String NAME = "RapprocherCtbRegistreFoncierJob";
	private static final String CATEGORIE = "Tiers";

	public static final String LISTE_PROPRIO = "LISTE_PROPRIO";

	private static final List<JobParam> params;

	private RegistreFoncierService registreFoncierService;
	private RapportService rapportService;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Fichier CSV des propriétaires fonciers");
			param.setName(LISTE_PROPRIO);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			params.add(param);
		}

	}

	public RapprocherCtbRegistreFoncierJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final byte[] listeProprio = (byte[]) params.get(LISTE_PROPRIO);
		if (listeProprio == null) {
			throw new RuntimeException("La liste des propriétaires doit être spécifiée.");
		}

		final StatusManager status = getStatusManager();
		final List<ProprietaireFoncier> listeProprietaireFoncier = extractProprioFromCSV(listeProprio, status);



		final RapprocherCtbResults results = registreFoncierService.rapprocherCtbRegistreFoncier(listeProprietaireFoncier, status, RegDate.get());
		final RapprocherCtbRapport rapport = rapportService.generateRapport(results, status);


		setLastRunReport(rapport);
		Audit.success("Le rapprochement des contribuables et des proprietaires du registre fonciers est terminée.",rapport);
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


		final List<ProprietaireFoncier> listeProprio = new ArrayList<ProprietaireFoncier>();
        final Pattern p = Pattern.compile("^([0-9]+);(.*?);(.*?);(.*?);(.*)");

		// on parse le fichier
        final String csvString = new String(csv,"ISO-8859-1");
		Scanner s = new Scanner(csvString);

        final String[] lines = csvString.split("[\n]");
        final int nombreProprio = lines.length;
        int proprioLu = 0;

		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				  //Audit.info("nombre de propriétaire tratés "+proprioLu);
                int percent = (proprioLu * 100) / nombreProprio;
                status.setMessage("Chargement des propriétaires fonciers",percent);
                if (status.interrupted()) {
                	break;
                }

				Matcher m = p.matcher(line);

				// on a un numero du registre foncier
				if (m.matches()) {

					Long numeroRegistreFoncier = null;
					String nom = null;
					String prenom = null;
					RegDate dateNaissance = null;
					Long numeroContribuable = null;

					// String[] tokens = line.split(";");

					numeroRegistreFoncier = Long.valueOf(m.group(1));

					nom = String.valueOf(m.group(2));
					prenom = String.valueOf(m.group(3));
					String stringDateNaissance = String.valueOf(m.group(4));

					if (!"".equals(stringDateNaissance)) {
                      if(stringDateNaissance.contains("/")){
                       stringDateNaissance = stringDateNaissance.replace("/",".");
                       }

						try {
							dateNaissance = RegDateHelper.displayStringToRegDate(stringDateNaissance, true);
						}
						catch (Exception e) {
					        Audit.error("La date de naissance "+stringDateNaissance+" pour le proprio "+numeroRegistreFoncier+" est incorrect : " + e.getMessage());

						}
					}
					numeroContribuable = Long.valueOf(m.group(5));
					listeProprio.add(new ProprietaireFoncier(numeroRegistreFoncier, nom, prenom, dateNaissance, numeroContribuable));
                    proprioLu++;
				}

			}
		}
		finally {
			s.close();
		}
		  Audit.info("nombre de propriétaire lus dans le fichier :"+proprioLu);
		return listeProprio;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public RegistreFoncierService getRegistreFoncierService() {
		return registreFoncierService;
	}
}
