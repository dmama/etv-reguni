package ch.vd.unireg.identification.contribuable;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.IdentifierContribuableFromListeRapport;
import ch.vd.unireg.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.type.Sexe;

/**
 * Job qui ikdentifie des contribuables a partir d'une liste contenant des informations civiles
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IdentifierContribuableFromListeJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentifierContribuableFromListeJob.class);

	public static final String NAME = "IdentifierContribuableFromListeJob";

	public static final String LISTE_TIERS = "LISTE_TIERS";
	public static final String NB_THREADS = "NB_THREADS";

	private IdentificationContribuableService identificationService;
	private RapportService rapportService;

	public IdentifierContribuableFromListeJob(int sortOrder, String description) {
		super(NAME, JobCategory.IDENTIFICATION, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des tiers à identifier");
			param.setName(LISTE_TIERS);
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

		final byte[] listeTiers = getFileContent(params, LISTE_TIERS);
		final int nbThreads = getIntegerValue(params, NB_THREADS);

		final StatusManager status = getStatusManager();
		final List<CriteresPersonne> ListeCriteresPersonnes = extractCriteresFromCSV(listeTiers, status);

		final IdentifierContribuableFromListeResults results = identificationService.identifieFromListe(ListeCriteresPersonnes, status, RegDate.get(), nbThreads);
		final IdentifierContribuableFromListeRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("L'identification de contribuable à partir d'une liste est terminée.", rapport);
	}



	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	/**
	 * Extrait les informations d'un fichier CSV, elles sont séparées par des virgules, des points-virgules ou des retours de ligne.
	 *
	 * @param csv
	 *            le contenu d'un fichier CSV
	 * @return une liste d'ids
	 * @throws UnsupportedEncodingException
	 */
	protected static List<CriteresPersonne> extractCriteresFromCSV(byte[] csv, StatusManager status) throws UnsupportedEncodingException {


		final List<CriteresPersonne> criteresPersonnes = new LinkedList<>();

		// On attend les colonnes suivantes (seule la première est réellement obligatoire dans le fichier, les autres colonnes sont parfois vides) :
		// 1. nom
		// 2. prénom (celui-ci contient parfois un point-virgule mais pas de chiffres - comme dans "André; en liquidation" - donc on s'assure qu'il est bien inclu ici et pas dans le nom ni la date de naissance
		// 3: Sexe: contient la valeur M ou F
		// 4. date de naissance au format DD.MM.YYYY ou DD/MM/YYYY (dates partielles autorisées)
		// 5. NAVS_13
		// 6. NAVS_11
		// 7: LIGNE_ADRESSE_1
		// 8: LIGNE_ADRESSE_2
		// 9: RUE
		// 10: NO_POLICE
		// 11: NO_APPARTEMENT
		// 12: NUMERO_CASE_POSTALE
		// 13: TEXTE_CASE_POSTALE
		// 14: LOCALITE
		// 15: LIEU
		// 16: NPA_SUISSE
		// 17: CHIFFRE_COMPLEMENTAIRE
		// 18: NO_ORDRE_POSTE_SUISSE
		// 19: NPA_ETRANGER
		// 20: CODE_PAYS
		// 21: TYPE_ADRESSE

		//Pattern complet
       // final Pattern p = Pattern.compile("^([^;]*)?;([^;]*)?;([M|F]*)?;([0-9.\\/]+)?;(\\d{13})?;(\\d{11})?;([^;]*)?;([^;]*)?;([^;]*)?;([^;]*)?;([^;]*)?;(\\d+)?;([^;]*)?;([^;]*)?;([^;]*)?;(\\d+)?;([^;]*)?;(\\d+)?;([^;]*)?;([^;]*)?;([^;]*)?");

		//Pattern simple
		final Pattern p = Pattern.compile("^([^;]*)?;([^;]*)?;([0-9.\\.]+)?(;.*)?");
		// on parse le fichier
        final String csvString = (csv != null ? new String(csv,"ISO-8859-1") : StringUtils.EMPTY);

		final String[] linesFromFile = csvString.split("[\n]");
		//On enlève le nom des colonnes ce qui correspond à la première ligne
		final String[] lines = ArrayUtils.remove(linesFromFile, 0);

		final int nombreDemandes = lines.length;
        int demandeLues = 0;

		try (Scanner s = new Scanner(csvString)) {
			while (s.hasNextLine()) {

				final String line = s.nextLine();

				final int percent = (demandeLues * 100) / nombreDemandes;
				status.setMessage("Chargement des critères de recherche des tiers", percent);
				if (status.isInterrupted()) {
					break;
				}

				final Matcher m = p.matcher(line);

				// on a un numero du registre foncier
				if (m.matches()) {
					final CriteresPersonne criteresPersonne = new CriteresPersonne();

					criteresPersonne.setNom(m.group(1));
					criteresPersonne.setPrenoms(m.group(2));
					//criteresPersonne.setSexe(getSexe(m.group(3)));
					String stringDateNaissance = m.group(3);
					RegDate dateNaissance = null;
					if (StringUtils.isNotBlank(stringDateNaissance)) {
						if (stringDateNaissance.contains("/")) {
							stringDateNaissance = stringDateNaissance.replace("/", ".");
						}
						stringDateNaissance = StringUtils.deleteWhitespace(stringDateNaissance);
						try {
							dateNaissance = RegDateHelper.displayStringToRegDate(stringDateNaissance, true);
						}
						catch (Exception e) {
							LOGGER.error(String.format("La date de naissance '%s' pour le tiers %s est incorrecte (elle sera ignorée) : %s", stringDateNaissance, criteresPersonne.getNom() + " "+criteresPersonne.getPrenoms(),
							                           e.getMessage()));
						}
					}
					criteresPersonne.setDateNaissance(dateNaissance);
				/*	criteresPersonne.setNAVS13(m.group(5));
					criteresPersonne.setNAVS11(m.group(6));
					CriteresAdresse adresse = new CriteresAdresse();
					adresse.setLigneAdresse1(m.group(7));
					adresse.setLigneAdresse2(m.group(8));
					adresse.setRue(m.group(9));
					adresse.setNoPolice(m.group(10));
					adresse.setNoAppartement(m.group(11));
					final String stringCasePostale = m.group(12);
					if (StringUtils.isNoneEmpty(stringCasePostale)) {
						adresse.setNumeroCasePostale(Integer.valueOf(stringCasePostale));
					}
					adresse.setTexteCasePostale(m.group(13));
					adresse.setLocalite(m.group(14));
					adresse.setLieu(m.group(15));
					final String stringNpa = m.group(16);
					if (StringUtils.isNoneEmpty(stringNpa)) {
						adresse.setNpaSuisse(Integer.valueOf(stringNpa));
					}
					adresse.setChiffreComplementaire(m.group(17));
					final String strNoOrdrePostal = m.group(18);
					if (StringUtils.isNoneEmpty(strNoOrdrePostal)) {
						adresse.setNoOrdrePosteSuisse(Integer.valueOf(strNoOrdrePostal));
					}

					adresse.setNpaEtranger(m.group(19));
					adresse.setCodePays(m.group(20));
					adresse.setTypeAdresse(getTypeAdresse(m.group(21)));
					criteresPersonne.setAdresse(adresse);*/
					criteresPersonnes.add(criteresPersonne);

					++demandeLues;
				}
				else {
					LOGGER.error(String.format("La ligne '%s' ne correspond pas au format attendu, elle sera ignorée", line));
				}
			}
		}

		// tri de la liste par numéro du registre foncier

		Audit.info("Nombre de demande d'identification lus dans le fichier : " + demandeLues);
		return criteresPersonnes;
	}

	private static CriteresAdresse.TypeAdresse getTypeAdresse(String type) {
		if (type== null) {
			return null;
		}
		if ("SUISSE".equals(type)) {
			return CriteresAdresse.TypeAdresse.SUISSE;
		}
		else if("ETRANGERE".equals(type)){
			return CriteresAdresse.TypeAdresse.ETRANGERE;
		}
		else{
			return null;
		}

	}

	private static Sexe getSexe(String codeSexe) {
		if (codeSexe== null) {
			return null;
		}
		if ("F".equals(codeSexe)) {
			return Sexe.FEMININ;
		}
		else if("M".equals(codeSexe)){
			return Sexe.MASCULIN;
		}
		else{
			return null;
		}

	}

	public void setIdentificationService(IdentificationContribuableService identificationService) {
		this.identificationService = identificationService;
	}
}
