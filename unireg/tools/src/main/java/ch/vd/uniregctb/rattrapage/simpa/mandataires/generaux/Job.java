package ch.vd.uniregctb.rattrapage.simpa.mandataires.generaux;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.Helper;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.MappingMigration;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.TypeTiers;
import ch.vd.uniregctb.type.TypeMandat;

/**
 * [SIFISC-19841] Récupération des données de mandataire modifiées dans SIMPA depuis la MeP Unireg du 13.06.2016
 */
public class Job {

	private enum VersionDB {
		_16R2,
		_16R3
	}

	private static final String EXTRACTION = "extraction-simpa.csv";
	private static final String MAPPING = "../mappings-migration.csv";

	private static final String VISA = "[Rattrapage-mutations-mandataires-" + RegDateHelper.toIndexString(RegDate.get()) + "]";

	private static final VersionDB CIBLE = VersionDB._16R3;

	private static final String SQL_FILE = "/tmp/rattrapage-auto-mandataires.sql";

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

	private static void traiterMandant(Long idMandant, List<ExtractionSimpa> mandats, Map<MappingMigration.Key, Long> mapping, PrintWriter pw) throws IOException {
		pw.println("--");
		pw.println("-- Traitement du mandant " + FormatNumeroHelper.numeroCTBToDisplay(idMandant));
		pw.println("--");
		pw.println();

		// on veut traiter les clôtures d'abord...
		final List<ExtractionSimpa> mandatsTries = new ArrayList<>(mandats);
		Collections.sort(mandatsTries, new Comparator<ExtractionSimpa>() {
			@Override
			public int compare(ExtractionSimpa o1, ExtractionSimpa o2) {
				return NullDateBehavior.LATEST.compare(o1.getDateResiliation(), o2.getDateResiliation());
			}
		});

		for (ExtractionSimpa mandat : mandatsTries) {
			// en fermeture, on regarde tout le monde
			if (mandat.getDateResiliation() != null) {
				// fermeture du mandat
				final long idMandataire;
				if (mandat.getTypeMandataire() != TypeTiers.ENTREPRISE) {
					final MappingMigration.Key key = new MappingMigration.Key(mandat.getTypeMandataire(), mandat.getNoMandataire());
					final Long idUnireg = mapping.get(key);
					if (idUnireg == null) {
						// pas de mapping, le mandataire n'a pas été migré... on parle donc d'une adresse mandataire
						pw.println("-- Fermeture de l'adresse mandataire de type " + mandat.getTypeMandat() + " ouverte (mandat vers un " + mandat.getTypeMandataire() + " non-migré - " + mandat.getNoMandataire() + ")");
						pw.println("UPDATE ADRESSE_MANDATAIRE SET LOG_MUSER='" + VISA + "', LOG_MDATE=CURRENT_DATE, DATE_FIN=" + RegDateHelper.toIndexString(mandat.getDateResiliation()));
						pw.println("WHERE CTB_ID=" + idMandant + " AND ANNULATION_DATE IS NULL AND DATE_FIN IS NULL AND TYPE_MANDAT='" + mandat.getTypeMandat() + "';");
						pw.println();
						continue;
					}
					idMandataire = idUnireg;
				}
				else {
					idMandataire = mandat.getNoMandataire();
				}

				// fermeture du lien de mandat avec le mandataire connu
				pw.println("-- Fermeture du lien de mandat de type " + mandat.getTypeMandat() + " ouvert vers l'entité " + mandat.getTypeMandataire() + " " + FormatNumeroHelper.numeroCTBToDisplay(idMandataire));
				pw.println("UPDATE RAPPORT_ENTRE_TIERS SET LOG_MUSER='" + VISA + "', LOG_MDATE=CURRENT_DATE, DATE_FIN=" + RegDateHelper.toIndexString(mandat.getDateResiliation()));
				pw.println("WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND ANNULATION_DATE IS NULL AND DATE_FIN IS NULL AND TYPE_MANDAT='" + mandat.getTypeMandat() + "' AND TIERS_SUJET_ID=" + idMandant + " AND TIERS_OBJET_ID=" + idMandataire + ";");
				pw.println();
			}
			else {
				// ne s'agit donc pas d'une fermeture... on pense d'avantage à une ouverture ou une modification...
				final long idMandataire;
				if (mandat.getTypeMandataire() != TypeTiers.ENTREPRISE) {
					final MappingMigration.Key key = new MappingMigration.Key(mandat.getTypeMandataire(), mandat.getNoMandataire());
					final Long idUnireg = mapping.get(key);
					if (idUnireg == null) {
						final String msg = String.format("Modification/création du mandat entre le mandant %s et le mandataire de type %s non-migré %d non prise en compte.",
						                                 FormatNumeroHelper.numeroCTBToDisplay(idMandant),
						                                 mandat.getTypeMandataire(),
						                                 mandat.getNoMandataire());
						System.err.println(msg);
						pw.println("-- " + msg);
						pw.println();
						continue;
					}
					idMandataire = idUnireg;
				}
				else {
					idMandataire = mandat.getNoMandataire();
				}

				// on suppose d'abord une mise-à-jour -> annulation de l'ancienne valeur
				pw.println("-- Annulation de l'ancien mandat de type " + mandat.getTypeMandat() + " ouvert vers l'entité " + mandat.getTypeMandataire() + " " + FormatNumeroHelper.numeroCTBToDisplay(idMandataire));
				pw.println("UPDATE RAPPORT_ENTRE_TIERS SET LOG_MUSER='" + VISA + "', LOG_MDATE=CURRENT_DATE, ANNULATION_USER='" + VISA + "', ANNULATION_DATE=CURRENT_DATE");
				pw.println("WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND ANNULATION_DATE IS NULL AND DATE_FIN IS NULL AND TYPE_MANDAT='" + mandat.getTypeMandat() + "' AND TIERS_SUJET_ID=" + idMandant + " AND TIERS_OBJET_ID=" + idMandataire + ";");
				pw.println();

				// puis insertion de la nouvelle valeur
				final String iban = mandat.getIban() != null ? FormatNumeroHelper.removeSpaceAndDash(mandat.getIban()).toUpperCase() : null;
				final String nomContact = StringUtils.trimToNull(mandat.getNomContact());
				final String prenomContact = StringUtils.trimToNull(mandat.getPrenomContact());
				final String telephone = removeNonNumbers(mandat.getTelephoneContact());
				final String copyCourriers = mandat.getTypeMandat() == TypeMandat.TIERS ? toDb(null) : "1";
				switch (CIBLE) {
				case _16R2:
					// la colonne WITH_COPY_MANDAT n'existait pas encore
					pw.println("-- Ajout du nouveau mandat de type " + mandat.getTypeMandat() + " ouvert vers l'entité " + mandat.getTypeMandataire() + " " + FormatNumeroHelper.numeroCTBToDisplay(idMandataire));
					pw.println("INSERT INTO RAPPORT_ENTRE_TIERS (ID, RAPPORT_ENTRE_TIERS_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, TIERS_SUJET_ID, TIERS_OBJET_ID, TYPE_MANDAT, IBAN_MANDAT, NOM_CONTACT_MANDAT, PRENOM_CONTACT_MANDAT, TEL_CONTACT_MANDAT)");
					pw.println("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'Mandat', CURRENT_DATE, '" + VISA + "', CURRENT_DATE, '" + VISA + "', " + RegDateHelper.toIndexString(mandat.getDateAttribution()) + ", " + idMandant + ", " + idMandataire + ", '" + mandat.getTypeMandat() + "', " + toDb(iban) + ", " + toDb(nomContact) + ", " + toDb(prenomContact) + ", " + toDb(telephone) + " FROM DUAL;");
					break;
				case _16R3:
					// apparition de la colonne WITH_COPY_MANDAT
					pw.println("-- Ajout du nouveau mandat de type " + mandat.getTypeMandat() + " ouvert vers l'entité " + mandat.getTypeMandataire() + " " + FormatNumeroHelper.numeroCTBToDisplay(idMandataire));
					pw.println("INSERT INTO RAPPORT_ENTRE_TIERS (ID, RAPPORT_ENTRE_TIERS_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, TIERS_SUJET_ID, TIERS_OBJET_ID, TYPE_MANDAT, IBAN_MANDAT, NOM_CONTACT_MANDAT, PRENOM_CONTACT_MANDAT, TEL_CONTACT_MANDAT, WITH_COPY_MANDAT)");
					pw.println("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'Mandat', CURRENT_DATE, '" + VISA + "', CURRENT_DATE, '" + VISA + "', " + RegDateHelper.toIndexString(mandat.getDateAttribution()) + ", " + idMandant + ", " + idMandataire + ", '" + mandat.getTypeMandat() + "', " + toDb(iban) + ", " + toDb(nomContact) + ", " + toDb(prenomContact) + ", " + toDb(telephone) + ", " + copyCourriers + " FROM DUAL;");
					break;
				default:
					throw new IllegalArgumentException("Cible non supportée !");
				}
				pw.println();
			}
		}
	}

	private static String toDb(@Nullable String value) {
		if (StringUtils.isBlank(value)) {
			return "NULL";
		}
		return String.format("'%s'", value);
	}

	@Nullable
	private static String removeNonNumbers(@Nullable String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		return StringUtils.trimToNull(value.replaceAll("[^0-9]", StringUtils.EMPTY));
	}
}
