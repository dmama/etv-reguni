package ch.vd.uniregctb.rattrapage.rcent.migration;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * INFO;8226;Inactive;CHE104488701;101448657;For principal COMMUNE_OU_FRACTION_VD/5561 [28.12.1987 -> 23.11.1999] généré.
 * INFO;8226;Inactive;CHE104488701;101448657;For secondaire 'immeuble' [25.01.1988 -> 04.12.1998] ajouté sur la commune 5561.
 */
public class ForsHandler implements CategoryHandler {

	private final Set<Long> identifiantsEntreprisesSP;

	public ForsHandler(Set<Long> identifiantsEntreprisesSP) {
		this.identifiantsEntreprisesSP = identifiantsEntreprisesSP;
	}

	/**
	 * 1 -> numéro entreprise
	 * 2 -> type autorité fiscale
	 * 3 -> numéro ofs autorité fiscale
	 * 4 -> date de début du for JJ.MM.AAAA
	 * 5 -> date de fin du for JJ.MM.AAAA ou ?
	 *
	 * TODO Motifs d'ouverture, de fermeture ?
	 * TODO Genre d'impôt (IBC / IRF) ?
	 */
	private static final Pattern FOR_PRINCIPAL_PATTERN = Pattern.compile("(?:INFO|WARN);([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;For principal (COMMUNE_OU_FRACTION_VD|COMMUNE_HC|PAYS_HS)/([0-9]+) \\[([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) -> (\\?|[0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\] généré\\.");

	/**
	 * 1 -> numéro entreprise
	 * 2 -> immeuble/établissement
	 * 3 -> date de début du for JJ.MM.AAAA
	 * 4 -> date de fin du for JJ.MM.AAAA ou ?
	 * 5 -> numéro ofs autorité fiscale
	 *
	 * TODO Genre d'impôt (IBC / IRF) ?
	 */
	private static final Pattern FOR_SECONDAIRE_PATTERN = Pattern.compile("(?:INFO|WARN);([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;For secondaire '(immeuble|activité)' \\[([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) -> (\\?|[0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\] ajouté sur la commune ([0-9]+)\\.");

	private static final Map<String, MotifRattachement> MOTIFS_RATTACHEMENT = buildMotifsRattachement();

	private static Map<String, MotifRattachement> buildMotifsRattachement() {
		final Map<String, MotifRattachement> map = new HashMap<>();
		map.put("immeuble", MotifRattachement.IMMEUBLE_PRIVE);
		map.put("activité", MotifRattachement.ETABLISSEMENT_STABLE);
		return map;
	}

	@Override
	public void buildSql(StringBuilder buffer, List<String> input) throws ParseException {
		for (String line : input) {
			final Matcher ffpMatcher = FOR_PRINCIPAL_PATTERN.matcher(line);
			if (ffpMatcher.matches()) {
				final long idEntreprise = Long.parseLong(ffpMatcher.group(1));

				buffer.append("INSERT INTO FOR_FISCAL (ID, FOR_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_OUVERTURE, MOTIF_OUVERTURE, DATE_FERMETURE, MOTIF_FERMETURE, GENRE_IMPOT, MOTIF_RATTACHEMENT, TYPE_AUT_FISC, NUMERO_OFS, TIERS_ID)").append(System.lineSeparator());
				buffer.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'ForFiscalPrincipalPM', CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
				buffer.append(RegDateHelper.displayStringToRegDate(ffpMatcher.group(4), false).index());
				buffer.append(", '");
				buffer.append(MotifFor.DEBUT_EXPLOITATION);
				buffer.append("', ");
				if ("?".equals(ffpMatcher.group(5))) {
					buffer.append("NULL, NULL, ");
				}
				else {
					buffer.append(RegDateHelper.displayStringToRegDate(ffpMatcher.group(5), false).index());
					buffer.append(", '");
					buffer.append(MotifFor.FIN_EXPLOITATION);
					buffer.append("', ");
				}
				buffer.append("'");
				buffer.append(identifiantsEntreprisesSP.contains(idEntreprise) ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL);
				buffer.append("', '");
				buffer.append(MotifRattachement.DOMICILE);
				buffer.append("', '");
				buffer.append(ffpMatcher.group(2));
				buffer.append("', ");
				buffer.append(ffpMatcher.group(3));
				buffer.append(", ");
				buffer.append(idEntreprise);
				buffer.append(" FROM DUAL;").append(System.lineSeparator());
				buffer.append(System.lineSeparator());
			}
			else {
				final Matcher ffsMatcher = FOR_SECONDAIRE_PATTERN.matcher(line);
				if (ffsMatcher.matches()) {
					final long idEntreprise = Long.parseLong(ffsMatcher.group(1));
					final MotifRattachement motifRattachement = MOTIFS_RATTACHEMENT.get(ffsMatcher.group(2));

					buffer.append("INSERT INTO FOR_FISCAL (ID, FOR_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_OUVERTURE, MOTIF_OUVERTURE, DATE_FERMETURE, MOTIF_FERMETURE, GENRE_IMPOT, MOTIF_RATTACHEMENT, TYPE_AUT_FISC, NUMERO_OFS, TIERS_ID)").append(System.lineSeparator());
					buffer.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'ForFiscalSecondaire', CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
					buffer.append(RegDateHelper.displayStringToRegDate(ffsMatcher.group(3), false).index());
					buffer.append(", '");
					buffer.append(motifRattachement == MotifRattachement.IMMEUBLE_PRIVE ? MotifFor.ACHAT_IMMOBILIER : MotifFor.DEBUT_EXPLOITATION);
					buffer.append("', ");
					if ("?".equals(ffsMatcher.group(4))) {
						buffer.append("NULL, NULL, ");
					}
					else {
						buffer.append(RegDateHelper.displayStringToRegDate(ffsMatcher.group(4), false).index());
						buffer.append(", '");
						buffer.append(motifRattachement == MotifRattachement.IMMEUBLE_PRIVE ? MotifFor.VENTE_IMMOBILIER : MotifFor.FIN_EXPLOITATION);
						buffer.append("', ");
					}
					buffer.append("'");
					buffer.append(identifiantsEntreprisesSP.contains(idEntreprise) ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL);
					buffer.append("', '");
					buffer.append(motifRattachement);
					buffer.append("', '");
					buffer.append(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					buffer.append("', ");
					buffer.append(ffsMatcher.group(5));
					buffer.append(", ");
					buffer.append(idEntreprise);
					buffer.append(" FROM DUAL;").append(System.lineSeparator());
					buffer.append(System.lineSeparator());
				}
				else {
					throw new ParseException("Invalid line : " + line, 0);
				}
			}
		}
	}
}
