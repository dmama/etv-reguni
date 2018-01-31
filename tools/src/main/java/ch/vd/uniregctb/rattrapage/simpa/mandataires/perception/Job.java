package ch.vd.uniregctb.rattrapage.simpa.mandataires.perception;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.Helper;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.MappingMigration;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.TypeTiers;
import ch.vd.uniregctb.type.TypeMandat;

/**
 * [SIFISC-20397] Report des changements d'IBAN des mandats tiers de SIMPA vers Unireg
 */
public class Job {

	private static final String EXTRACTION = "extraction-simpa.csv";
	private static final String MAPPING = "../mappings-migration.csv";

	private static final String VISA = "[Rattrapage-iban-mandataires-" + RegDateHelper.toIndexString(RegDate.get()) + "]";
	private static final String SQL_FILE = "/tmp/rattrapage-auto-iban-mandataires-" + RegDateHelper.toIndexString(RegDate.get()) + ".sql";

	public static void main(String[] args) throws Exception {

		// extraction des lignes en provenance de SIMPA
		final Map<Long, List<ExtractionSimpa>> mandants = loadExtractionParMandant();

		// chargement des mappings de migration
		final Map<MappingMigration.Key, Long> mapping = loadMapping();

		// traitement par mandant
		try (FileOutputStream fos = new FileOutputStream(SQL_FILE);
		     Writer w = new OutputStreamWriter(fos, Charset.defaultCharset());
		     PrintWriter pw = new PrintWriter(w)) {

			for (Map.Entry<Long, List<ExtractionSimpa>> entry : mandants.entrySet()) {
				traiterMandant(entry.getKey(), entry.getValue(), mapping, pw);
			}
		}
	}

	private static Map<MappingMigration.Key, Long> loadMapping() throws IOException {
		final List<MappingMigration> data;
		try (InputStream is = Job.class.getResourceAsStream(MAPPING);
		     Reader r = new InputStreamReader(is, Charset.defaultCharset())) {

			data = Helper.loadFile(MAPPING, r, MappingMigration::of);
		}

		System.err.println("Fichier " + MAPPING + " : " + data.size() + " ligne(s) chargée(s).");

		final Map<MappingMigration.Key, Long> map = new HashMap<>(data.size());
		for (MappingMigration mapping : data) {
			map.put(mapping.getKey(), mapping.getIdUnireg());
		}
		return Collections.unmodifiableMap(map);
	}

	private static Map<Long, List<ExtractionSimpa>> loadExtractionParMandant() throws IOException {
		final List<ExtractionSimpa> extraction;
		try (InputStream is = Job.class.getResourceAsStream(EXTRACTION);
		     Reader r = new InputStreamReader(is, Charset.defaultCharset())) {

			extraction = Helper.loadFile(EXTRACTION, r, ExtractionSimpa::of);
		}

		System.err.println("Fichier " + EXTRACTION + " : " + extraction.size() + " ligne(s) chargée(s).");

		final Map<Long, List<ExtractionSimpa>> map = new TreeMap<>();
		for (ExtractionSimpa data : extraction) {
			final Long key = data.getNoMandant();
			final List<ExtractionSimpa> list;
			if (!map.containsKey(key)) {
				list = new LinkedList<>();
				map.put(key, list);
			}
			else {
				list = map.get(key);
			}
			list.add(data);
		}
		return Collections.unmodifiableMap(map);
	}

	private static class Data {
		final ExtractionSimpa mandat;
		final long idMandataireUnireg;

		public Data(ExtractionSimpa mandat, long idMandataireUnireg) {
			this.mandat = mandat;
			this.idMandataireUnireg = idMandataireUnireg;
		}
	}

	private static void traiterMandant(Long idMandant, List<ExtractionSimpa> mandats, Map<MappingMigration.Key, Long> mapping, PrintWriter pw) throws IOException {
		pw.println("--");
		pw.println("-- Traitement du mandant " + FormatNumeroHelper.numeroCTBToDisplay(idMandant));
		pw.println("--");
		pw.println();

		// listes des choses à faire
		final List<Data> clotures = new LinkedList<>();
		final List<Data> changementsIBAN = new LinkedList<>();
		final List<Data> ouvertures = new LinkedList<>();

		// détermination des actions
		for (ExtractionSimpa mandat : mandats) {
			if (mandat.getTypeMandat() == TypeMandat.TIERS) {
				final long idMandataire;
				if (mandat.getTypeMandataire() != TypeTiers.ENTREPRISE) {
					final MappingMigration.Key keyMandataire = new MappingMigration.Key(mandat.getTypeMandataire(), mandat.getNoMandataire());
					if (!mapping.containsKey(keyMandataire)) {
						final String msg = String.format("Donnée de mandataire T %s entre le mandant %s et le mandataire de type %s non-migré %d non prise en compte.",
						                                 DateRangeHelper.toDisplayString(mandat.getDateAttribution(), mandat.getDateResiliation()),
						                                 FormatNumeroHelper.numeroCTBToDisplay(idMandant),
						                                 mandat.getTypeMandataire(),
						                                 mandat.getNoMandataire());
						System.err.println(msg);
						pw.println("-- " + msg);
						pw.println();
						continue;
					}

					idMandataire = mapping.get(keyMandataire);
				}
				else {
					idMandataire = mandat.getNoMandataire();
				}

				// clôture ?
				final Data data = new Data(mandat, idMandataire);
				if (mandat.getDateResiliation() != null) {
					clotures.add(data);
				}

				// changement d'IBAN ?
				changementsIBAN.add(data);

				// ouverture (seulement pour les mandats encore ouverts après 31.12.2007)
				if (mandat.getDateResiliation() == null || mandat.getDateResiliation().isAfter(RegDate.get(2007, 12, 31))) {
					ouvertures.add(data);
				}
			}
		}

		// exécution des clôtures (simples - le numéro IBAN doit être resté le même... s'il a changé, on passera par une annulation-création)
		for (Data data : clotures) {
			pw.println(String.format("-- Clôture du mandat entre le mandant %s et le mandaire %s, ouvert le %s et fermé le %s",
			                         FormatNumeroHelper.numeroCTBToDisplay(data.mandat.getNoMandant()),
			                         FormatNumeroHelper.numeroCTBToDisplay(data.idMandataireUnireg),
			                         RegDateHelper.dateToDisplayString(data.mandat.getDateAttribution()),
			                         RegDateHelper.dateToDisplayString(data.mandat.getDateResiliation())));
			pw.println("UPDATE RAPPORT_ENTRE_TIERS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + VISA + "', DATE_FIN=" + data.mandat.getDateResiliation().index());
			pw.println("WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND DATE_DEBUT=" + data.mandat.getDateAttribution().index());
			pw.println("AND ANNULATION_DATE IS NULL AND DATE_FIN IS NULL AND TYPE_MANDAT='TIERS' AND TIERS_SUJET_ID=" + data.mandat.getNoMandant() + " AND TIERS_OBJET_ID=" + data.idMandataireUnireg);
			pw.println("AND IBAN_MANDAT='" + data.mandat.getIban() + "';");
			pw.println();
		}

		// exécution des changements de numéro IBAN (annulation + re-création avec nouveau numéro IBAN)
		for (Data data : changementsIBAN) {
			pw.println("-- Prise en compte d'un éventuel changement d'IBAN");
			pw.println("UPDATE RAPPORT_ENTRE_TIERS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + VISA + "', ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='" + VISA + "'");
			pw.println("WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND DATE_DEBUT=" + data.mandat.getDateAttribution().index());
			pw.println("AND ANNULATION_DATE IS NULL AND TYPE_MANDAT='TIERS' AND TIERS_SUJET_ID=" + data.mandat.getNoMandant() + " AND TIERS_OBJET_ID=" + data.idMandataireUnireg);
			pw.println("AND IBAN_MANDAT != '" + data.mandat.getIban() + "';");
			pw.println();

			pw.println("INSERT INTO RAPPORT_ENTRE_TIERS (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, RAPPORT_ENTRE_TIERS_TYPE, TYPE_MANDAT, TIERS_SUJET_ID, TIERS_OBJET_ID, IBAN_MANDAT, DATE_DEBUT, DATE_FIN)");
			pw.println(String.format("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '%s', CURRENT_DATE, '%s', 'Mandat', 'TIERS', %d, %d, '%s', %d, %s",
			                         VISA, VISA, data.mandat.getNoMandant(), data.idMandataireUnireg,
			                         data.mandat.getIban(),
			                         data.mandat.getDateAttribution().index(),
			                         data.mandat.getDateResiliation() != null ? data.mandat.getDateResiliation().index() : "NULL"));
			pw.println("FROM RAPPORT_ENTRE_TIERS WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND DATE_DEBUT=" + data.mandat.getDateAttribution().index());
			pw.println("AND ANNULATION_DATE IS NOT NULL AND ANNULATION_USER='" + VISA + "' AND TYPE_MANDAT='TIERS' AND TIERS_SUJET_ID=" + data.mandat.getNoMandant() + " AND TIERS_OBJET_ID=" + data.idMandataireUnireg);
			pw.println("AND IBAN_MANDAT != '" + data.mandat.getIban() + "';");
			pw.println();
		}

		// exécution des ouvertures de nouveaux mandats tiers (seulement pour les mandats encore ouverts après 2009)
		for (Data data : ouvertures) {
			pw.println("-- Création d'un nouveau mandat tiers si nécessaire");
			pw.println("INSERT INTO RAPPORT_ENTRE_TIERS (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, RAPPORT_ENTRE_TIERS_TYPE, TYPE_MANDAT, TIERS_SUJET_ID, TIERS_OBJET_ID, IBAN_MANDAT, DATE_DEBUT, DATE_FIN)");
			pw.println(String.format("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '%s', CURRENT_DATE, '%s', 'Mandat', 'TIERS', %d, %d, '%s', %d, %s",
			                         VISA, VISA, data.mandat.getNoMandant(), data.idMandataireUnireg,
			                         data.mandat.getIban(),
			                         data.mandat.getDateAttribution().index(),
			                         data.mandat.getDateResiliation() != null ? data.mandat.getDateResiliation().index() : "NULL"));
			pw.println("FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM RAPPORT_ENTRE_TIERS WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND DATE_DEBUT=" + data.mandat.getDateAttribution().index());
			pw.println("AND ANNULATION_DATE IS NULL AND TYPE_MANDAT='TIERS' AND TIERS_SUJET_ID=" + data.mandat.getNoMandant() + " AND TIERS_OBJET_ID=" + data.idMandataireUnireg +");");
			pw.println();
		}

		pw.println();
	}
}
