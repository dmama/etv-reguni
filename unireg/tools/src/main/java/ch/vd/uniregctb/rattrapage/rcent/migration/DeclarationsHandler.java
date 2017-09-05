package ch.vd.uniregctb.rattrapage.rcent.migration;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * INFO;31745;Active;CHE109345167;101598042;Génération d'une déclaration sur la PF 2012 à partir des dates [01.01.2012 -> 31.12.2012] de l'exercice commercial 17 et du dossier fiscal correspondant.
 * INFO;31745;Active;CHE109345167;101598042;Délai initial de retour fixé au 15.09.2013.
 * INFO;31745;Active;CHE109345167;101598042;Etat 'EMISE' migré au 03.01.2013.
 * INFO;31745;Active;CHE109345167;101598042;Etat 'SOMMEE' migré au 23.09.2013.
 * INFO;31745;Active;CHE109345167;101598042;Etat 'RETOURNEE' migré au 09.10.2013.
 * ...
 * INFO;31745;Active;CHE109345167;101598042;Génération d'une déclaration sur la PF 2015 à partir des dates [01.01.2015 -> 31.12.2015] de la période d'imposition calculée et du dossier fiscal 2015/1 sans exercice commercial lié.
 * INFO;31745;Active;CHE109345167;101598042;Délai initial de retour fixé au 15.09.2016.
 * INFO;31745;Active;CHE109345167;101598042;Etat 'EMISE' migré au 04.01.2016.
 */
public class DeclarationsHandler implements CategoryHandler {

	/**
	 * 1 -> numéro d'entreprise
	 * 2 -> PF
	 * 3 -> date de début de la période d'imposition et de l'exercice commercial (JJ.MM.AAAA)
	 * 4 -> date de fin de la période d'imposition et de l'exercice commercial (JJ.MM.AAAA)
	 */
	private static final Pattern DECLARATION_PATTERN = Pattern.compile("INFO;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;Génération d'une déclaration sur la PF ([0-9]+) à partir des dates \\[([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) -> ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\].*\\.");

	/**
	 * 1 -> numéro d'entreprise
	 * 2 -> date du délai initial (JJ.MM.AAAA)
	 */
	private static final Pattern DELAI_INITIAL_PATTERN = Pattern.compile("INFO;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;Délai initial (?:de retour )*fixé au ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\.");

	/**
	 * 1 -> numéro d'entreprise
	 * 2 -> date du délai accordé (JJ.MM.AAAA)
	 * 3 -> date de la demande de délai (JJ.MM.AAAA)
	 */
	private static final Pattern DELAI_PATTERN = Pattern.compile("INFO;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;Génération d'un délai accordé au ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) \\(demande du ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\)\\.");

	/**
	 * 1 -> numéro d'entreprise
	 * 2 -> type de l'état (cf {@link ch.vd.uniregctb.type.TypeEtatDeclaration})
	 * 2 -> date d'obtention de l'état en question
	 */
	private static final Pattern ETAT_PATTERN = Pattern.compile("INFO;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;Etat '([A-Z_]+)' migré au ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\\.");

	/**
	 * 1 -> numéro d'entreprise
	 * 2 -> pf
	 */
	private static final Pattern QUESTIONNAIRE_SNC_PATTERN = Pattern.compile("INFO;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;Génération d'un questionnaire SNC sur la période fiscale ([0-9]+)\\.");


	private static final String TEMP_DECLARATION_VISA = "SIFISC-19296.TEMP";
	private static final String CTXT_DECLARATION_IN_PROGRESS = "DECL_IN_PROGRESS";
	private static final String CTXT_DATE_DEMANDE_DELAI_INITIAL = "DEMANDE_DELAI_INITIAL";

	private interface MatcherHandler {
		void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException;
	}

	private static final Map<Pattern, MatcherHandler> HANDLERS = buildHandlers();

	private static Map<Pattern, MatcherHandler> buildHandlers() {
		final Map<Pattern, MatcherHandler> map = new HashMap<>();

		// une nouvelle déclaration
		map.put(DECLARATION_PATTERN, new MatcherHandler() {
			@Override
			public void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException {
				// ligne "génération d'une déclaration"...
				// - si une déclaration était déjà en cours, il faut finaliser son traitement (visa)
				if (context.containsKey(CTXT_DECLARATION_IN_PROGRESS)) {
					addVisaReset(b);
				}

				final long noEntreprise = Long.parseLong(matcher.group(1));
				final int pf = Integer.parseInt(matcher.group(2));
				final RegDate dateDebut = RegDateHelper.displayStringToRegDate(matcher.group(3), false);
				final RegDate dateFin = RegDateHelper.displayStringToRegDate(matcher.group(4), false);
				addDeclaration(b, noEntreprise, pf, dateDebut, dateFin);
				context.put(CTXT_DATE_DEMANDE_DELAI_INITIAL, dateFin.addDays(4));
				context.put(CTXT_DECLARATION_IN_PROGRESS, Boolean.TRUE);
			}
		});

		// le délai initial
		map.put(DELAI_INITIAL_PATTERN, new MatcherHandler() {
			@Override
			public void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException {
				final RegDate dateDelai = RegDateHelper.displayStringToRegDate(matcher.group(2), false);
				final RegDate dateDemande = (RegDate) context.get(CTXT_DATE_DEMANDE_DELAI_INITIAL);
				if (dateDemande == null) {
					throw new IllegalStateException("Délai initial sans déclaration préalable dans ligne " + matcher.group());
				}
				addDelai(b, dateDemande, dateDelai);
			}
		});

		// un autre délai
		map.put(DELAI_PATTERN, new MatcherHandler() {
			@Override
			public void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException {
				final RegDate delaiAccorde = RegDateHelper.displayStringToRegDate(matcher.group(2), false);
				final RegDate dateDemande = RegDateHelper.displayStringToRegDate(matcher.group(3), false);
				addDelai(b, dateDemande, delaiAccorde);
			}
		});

		// un état
		map.put(ETAT_PATTERN, new MatcherHandler() {
			@Override
			public void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException {
				final TypeEtatDeclaration type = TypeEtatDeclaration.valueOf(matcher.group(2));
				final RegDate dateObtention = RegDateHelper.displayStringToRegDate(matcher.group(3), false);
				addEtat(b, type, dateObtention);
			}
		});

		// un questionnaire SNC
		map.put(QUESTIONNAIRE_SNC_PATTERN, new MatcherHandler() {
			@Override
			public void handle(StringBuilder b, Matcher matcher, Map<String, Object> context) throws ParseException {
				// ligne "génération d'une déclaration"...
				// - si une déclaration était déjà en cours, il faut finaliser son traitement (visa)
				if (context.containsKey(CTXT_DECLARATION_IN_PROGRESS)) {
					addVisaReset(b);
				}

				final long noEntreprise = Long.parseLong(matcher.group(1));
				final int pf = Integer.parseInt(matcher.group(2));
				addQuestionnaireSNC(b, noEntreprise, pf);
				context.put(CTXT_DATE_DEMANDE_DELAI_INITIAL, RegDate.get(pf + 1, 1, 4));
				context.put(CTXT_DECLARATION_IN_PROGRESS, Boolean.TRUE);
			}
		});

		return map;
	}

	@Override
	public void buildSql(StringBuilder buffer, List<String> input) throws ParseException {
		final Map<String, Object> context = new HashMap<>();
		for (String line : input) {
			boolean matched = false;
			for (Map.Entry<Pattern, MatcherHandler> handlerEntry : HANDLERS.entrySet()) {
				final Matcher matcher = handlerEntry.getKey().matcher(line);
				if (matcher.matches()) {
					handlerEntry.getValue().handle(buffer, matcher, context);
					matched = true;
					break;
				}
			}
			if (!matched) {
				throw new ParseException("Invalid line : " + line, 0);
			}
		}

		// pour la dernière déclaration aussi, il faut faire ce reset...
		if (context.containsKey(CTXT_DECLARATION_IN_PROGRESS)) {
			addVisaReset(buffer);
		}
	}

	private static void addVisaReset(StringBuilder b) {
		b.append("-- Remise en place du visa sur la dernière déclaration créée").append(System.lineSeparator());
		b.append("UPDATE DOCUMENT_FISCAL SET LOG_CUSER='").append(Constants.VISA).append("' WHERE LOG_CUSER='").append(TEMP_DECLARATION_VISA).append("';").append(System.lineSeparator());
		b.append(System.lineSeparator());
	}

	private static void addDeclaration(StringBuilder b, long noEntreprise, int pf, RegDate dateDebut, RegDate dateFin) {
		b.append("-- Déclaration ").append(pf).append(" de l'entreprise ").append(noEntreprise).append(System.lineSeparator());
		b.append("INSERT INTO DOCUMENT_FISCAL (ID, DOCUMENT_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, NUMERO, LIBRE, PERIODE_ID, TIERS_ID, DATE_DEBUT_EXERCICE, DATE_FIN_EXERCICE)").append(System.lineSeparator());
		b.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'DIPM', CURRENT_DATE, '").append(TEMP_DECLARATION_VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
		b.append(dateDebut.index());
		b.append(", ");
		b.append(dateFin.index());
		b.append(", 1, 0, ID, ");
		b.append(noEntreprise);
		b.append(", ");
		b.append(dateDebut.index());
		b.append(", ");
		b.append(dateFin.index());
		b.append(System.lineSeparator());
		b.append("FROM PERIODE_FISCALE WHERE ANNEE=").append(pf).append(";").append(System.lineSeparator());
		b.append(System.lineSeparator());
	}

	private static void addQuestionnaireSNC(StringBuilder b, long noEntreprise, int pf) {
		b.append("-- Questionnaire SNC ").append(pf).append(" de l'entreprise ").append(noEntreprise).append(System.lineSeparator());
		b.append("INSERT INTO DOCUMENT_FISCAL (ID, DOCUMENT_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, NUMERO, LIBRE, PERIODE_ID, TIERS_ID)").append(System.lineSeparator());
		b.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, 'QSNC', CURRENT_DATE, '").append(TEMP_DECLARATION_VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
		b.append(pf).append("0101");
		b.append(", ");
		b.append(pf).append("1231");
		b.append(", 1, 0, ID, ");
		b.append(noEntreprise);
		b.append(System.lineSeparator());
		b.append("FROM PERIODE_FISCALE WHERE ANNEE=").append(pf).append(";").append(System.lineSeparator());
		b.append(System.lineSeparator());
	}

	private static void addDelai(StringBuilder b, RegDate dateDemande, RegDate dateDelai) {
		b.append("-- Délai au ").append(RegDateHelper.dateToDisplayString(dateDelai)).append(System.lineSeparator());
		b.append("INSERT INTO DELAI_DOCUMENT_FISCAL (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEMANDE, DATE_TRAITEMENT, DELAI_ACCORDE_AU, DOCUMENT_FISCAL_ID, ETAT, SURSIS)").append(System.lineSeparator());
		b.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
		b.append(dateDemande.index());
		b.append(", ");
		b.append(dateDemande.index());
		b.append(", ");
		b.append(dateDelai.index());
		b.append(", ID, '");
		b.append(EtatDelaiDocumentFiscal.ACCORDE);
		b.append("', 0").append(System.lineSeparator());
		b.append("FROM DOCUMENT_FISCAL WHERE LOG_CUSER='").append(TEMP_DECLARATION_VISA).append("';").append(System.lineSeparator());
		b.append(System.lineSeparator());
	}

	private static void addEtat(StringBuilder b, TypeEtatDeclaration type, RegDate dateObtention) {
		final RegDate dateEnvoiCourrier = (type == TypeEtatDeclaration.SOMMEE || type == TypeEtatDeclaration.RAPPELEE ? dateObtention : null);
		final String source = (type == TypeEtatDeclaration.RETOURNEE ? "SDI" : null);

		b.append("-- Etat ").append(type).append(" au ").append(RegDateHelper.dateToDisplayString(dateObtention)).append(System.lineSeparator());
		b.append("INSERT INTO ETAT_DOCUMENT_FISCAL (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_OBTENTION, TYPE, DOCUMENT_FISCAL_ID, DATE_ENVOI_COURRIER, SOURCE)").append(System.lineSeparator());
		b.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
		b.append(dateObtention.index());
		b.append(", '");
		b.append(type);
		b.append("', ID, ");
		if (dateEnvoiCourrier == null) {
			b.append("NULL, ");
		}
		else {
			b.append(dateEnvoiCourrier.index()).append(", ");
		}
		if (source == null) {
			b.append("NULL");
		}
		else {
			b.append("'").append(source).append("'");
		}
		b.append(System.lineSeparator());
		b.append("FROM DOCUMENT_FISCAL WHERE LOG_CUSER='").append(TEMP_DECLARATION_VISA).append("';").append(System.lineSeparator());
		b.append(System.lineSeparator());
	}
}
