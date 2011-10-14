package ch.vd.uniregctb.registrefoncier;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.ImmeubleDAO;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

import static ch.vd.uniregctb.registrefoncier.ImportImmeublesResults.ErreurType;
import static ch.vd.uniregctb.registrefoncier.ImportImmeublesResults.IgnoreType;

/**
 * Processeur qui gère l'importation des immeubles du registe-foncier dans Unireg à partir d'un fichier CSV.
 */
public class ImportImmeublesProcessor {

	private static final Logger LOGGER = Logger.getLogger(RapprocherCtbProcessor.class);

	private static final int BATCH_SIZE = 100;

	private static final String HEADER_NO_CTB = "NO_CTB";
	private static final String HEADER_GENRE_PERSONNE = "GENRE_PERSONNE";
	private static final String HEADER_NO_IMMEUBLE = "NO_IMMEUBLE";
	private static final String HEADER_NATURE = "NATURE";
	private static final String HEADER_ESTIMATION_FISCALE = "ESTIMATION_FISCALE";
	private static final String HEADER_DATE_ESTIMATION_FISCALE = "DATE_ESTIMATION_FISCALE";
	private static final String HEADER_ANCIENNE_ESTIMATION_FISCALE = "ANCIENNE_ESTIMATION_FISCALE";
	private static final String HEADER_GENRE_PROPRIETE = "GENRE_PROPRIETE";
	private static final String HEADER_PART_PROPRIETE = "PART_PROPRIETE";
	private static final String HEADER_DATE_DEBUT = "DATE_DEPOT_PJ";
	private static final String HEADER_DATE_FIN = "DATE_FIN";
	private static final String HEADER_URL = "URL";

	private enum GenrePersonne {
		PersonnePhysique,
		Entreprise,
		DroitPublic
	}

	private final ImmeubleDAO immeubleDAO;
	private final TiersDAO tiersDAO;
	private final TiersService tiersService;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	public ImportImmeublesProcessor(HibernateTemplate hibernateTemplate, ImmeubleDAO immeubleDAO, PlatformTransactionManager transactionManager, TiersDAO tiersDAO, TiersService tiersService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.immeubleDAO = immeubleDAO;
		this.tiersDAO = tiersDAO;
		this.tiersService = tiersService;
	}

	/**
	 * Importe les immeubles contenus dans un fichier CSV dans Unireg. La table IMMEUBLE sera entièrement vidée avant l'opération d'import.
	 *
	 * @param csvStream un flux des données à importer
	 * @param encoding  l'encoding du flux
	 * @param s         un status manager
	 * @return le résultat de l'importation
	 */
	public ImportImmeublesResults run(InputStream csvStream, String encoding, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ImportImmeublesResults rapportFinal = new ImportImmeublesResults();

		removeAllImmeubles(status);
		importAllImmeubles(csvStream, encoding, status, rapportFinal);

		rapportFinal.end();
		return rapportFinal;
	}

	private void removeAllImmeubles(StatusManager status) {
		status.setMessage("Effacement des anciens immeubles...");
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		t.execute(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus ts) throws Exception {
				immeubleDAO.removeAll();
				return null;
			}
		});
	}

	private void importAllImmeubles(InputStream csvStream, String encoding, final StatusManager status, final ImportImmeublesResults rapportFinal) {

		// Ouverture du flux CSV
		status.setMessage("Ouverture du fichier...");
		final Scanner csvIterator = new Scanner(csvStream, encoding);
		csvIterator.useDelimiter(Pattern.compile("[\r]?\n"));

		if (!csvIterator.hasNext()) {
			throw new IllegalArgumentException("Le fichier est vide !");
		}

		// Lecture de l'entête
		final List<String> headers = extractHeaders(csvIterator.next());

		// Processing des lignes de données
		final BatchTransactionTemplate<String, ImportImmeublesResults> template = new BatchTransactionTemplate<String, ImportImmeublesResults>(csvIterator, BATCH_SIZE,
				BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<String, ImportImmeublesResults>() {

			@Override
			public ImportImmeublesResults createSubRapport() {
				return new ImportImmeublesResults();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				super.afterTransactionRollback(e, willRetry);    //To change body of overridden methods use File | Settings | File Templates.
			}

			@Override
			public boolean doInTransaction(List<String> batch, ImportImmeublesResults rapport) throws Exception {

				for (String line : batch) {
					processLine(headers, line, rapport);

					final int lineProcessed = rapportFinal.nbLignes + rapport.nbLignes;
					if (lineProcessed % 100 == 0) {
						status.setMessage("Traitement de la ligne n°" + lineProcessed + "...");
					}

					if (status.interrupted()) {
						break;
					}
				}

				return true;
			}
		});

		// Logging
		if (status.interrupted()) {
			status.setMessage("L'import des immeubles a été interrompu."
					+ " Nombre d'immeubles importés au moment de l'interruption = " + rapportFinal.getNbImmeubles());
			rapportFinal.setInterrompu(true);
		}
		else {
			status.setMessage("L'import des immeubles  est terminé."
					+ " Nombre d'immeubles importés = " + rapportFinal.getNbImmeubles() + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}
	}

	private static List<String> extractHeaders(String headerLine) {
		final CSVParser parser = new CSVParser(';', '"');
		final String[] tokens;
		try {
			tokens = parser.parseLine(headerLine);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return Arrays.asList(tokens);
	}

	private void processLine(List<String> headers, String line, ImportImmeublesResults rapport) throws IOException {

		rapport.incNbLignes();

		final Map<String, String> data = parseLine(headers, line);

		final Immeuble immeuble = createImmeuble(data, rapport);
		if (immeuble == null) {
			return;
		}

		immeubleDAO.save(immeuble);
		rapport.addTraite(immeuble.getNumero(), immeuble.getProprietaire().getId());
	}

	/**
	 * Parse la ligne CSV spécifiée et génère la map header->value des valeurs de la ligne.
	 *
	 * @param headers la liste des entêtes
	 * @param line    la ligne courante
	 * @return une map header->value des tokens
	 * @throws IOException en cas d'erreur de parsing
	 */
	private static Map<String, String> parseLine(List<String> headers, String line) throws IOException {
		final CSVParser parser = new CSVParser(';', '"');
		final String[] tokens = parser.parseLine(line);
		final HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0, tokensLength = tokens.length; i < tokensLength; i++) {
			if (i < headers.size()) {
				map.put(headers.get(i), tokens[i]);
			}
		}
		return map;
	}

	/**
	 * Instancie un nouvel immeuble d'après les données d'une ligne CSV. L'ordre des colonnes est supposé être le suivant :
	 *
	 * @param data    les données extraites d'une ligne CSV
	 * @param rapport le rapport d'exécution qui sera complété si nécessaire
	 * @return l'immeuble créé; ou <b>null</b> si la ligne ne correspond pas à un immeuble (ligne d'entête ou ligne vide)
	 */
	private Immeuble createImmeuble(Map<String, String> data, ImportImmeublesResults rapport) {

		final String numero = data.get(HEADER_NO_IMMEUBLE);

		final RegDate dateDebut;
		try {
			dateDebut = parseRegDate(data.get(HEADER_DATE_DEBUT));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_DEBUT, "Date = " + data.get(HEADER_DATE_DEBUT));
			return null;
		}

		final GenrePersonne genrePersonne;
		try {
			genrePersonne = parseGenrePersonne(data.get(HEADER_GENRE_PERSONNE));
		}
		catch (IllegalArgumentException e) {
			rapport.addError(numero, ErreurType.BAD_GENRE_PERSONNE, "Genre personne = " + data.get(HEADER_GENRE_PERSONNE));
			return null;
		}

		final RegDate dateFin;
		try {
			dateFin = parseRegDate(data.get(HEADER_DATE_FIN));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_FIN, "Date = " + data.get(HEADER_DATE_FIN));
			return null;
		}

		final String nature = data.get(HEADER_NATURE);

		final int estimationFiscale;
		try {
			estimationFiscale = Integer.parseInt(data.get(HEADER_ESTIMATION_FISCALE));
		}
		catch (NumberFormatException e) {
			rapport.addError(numero, ErreurType.BAD_EF, "Estimation fiscale = " + data.get(HEADER_ESTIMATION_FISCALE));
			return null;
		}

		final RegDate dateEstimationFiscale;
		try {
			dateEstimationFiscale = parseRegDate(data.get(HEADER_DATE_ESTIMATION_FISCALE));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_EF, "Date = " + data.get(HEADER_DATE_ESTIMATION_FISCALE));
			return null;
		}

		final Integer ancienneEstimationFiscale;
		try {
			ancienneEstimationFiscale = parseInteger(data.get(HEADER_ANCIENNE_ESTIMATION_FISCALE));
		}
		catch (NumberFormatException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_ANCIENNE_EF, "Ancienne estimation fiscale = " + data.get(HEADER_ANCIENNE_ESTIMATION_FISCALE));
			return null;
		}

		final GenrePropriete genrePropriete;
		try {
			genrePropriete = parseGenrePropriete(data.get(HEADER_GENRE_PROPRIETE));
		}
		catch (IllegalArgumentException e) {
			rapport.addError(numero, ErreurType.BAD_GENRE_PROP, "Genre propriété = " + data.get(HEADER_GENRE_PROPRIETE));
			return null;
		}

		final PartPropriete partPropriete;
		try {
			partPropriete = PartPropriete.parse(data.get(HEADER_PART_PROPRIETE));
		}
		catch (IllegalArgumentException e) {
			rapport.addError(numero, ErreurType.BAD_PART_PROP, "Part de propriété = " + data.get(HEADER_PART_PROPRIETE));
			return null;
		}

		final URL lienRegistreFoncier;
		try {
			lienRegistreFoncier = parseURL(data.get(HEADER_URL));
		}
		catch (MalformedURLException e) {
			rapport.addError(numero, ErreurType.BAD_URL, "Url = " + data.get(HEADER_URL));
			return null;
		}

		final Long ctbId;
		try {
			ctbId = parseLong(data.get(HEADER_NO_CTB));
		}
		catch (NumberFormatException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_NO_CTB, "Numéro = " + data.get(HEADER_NO_CTB));
			return null;
		}
		if (ctbId == null || ctbId == 0) {
			rapport.addIgnore(numero, IgnoreType.CTB_NULL);
			return null;
		}

		final Contribuable proprietaire = (Contribuable) tiersDAO.get(ctbId);
		if (proprietaire == null) {
			rapport.addError(numero, ErreurType.CTB_INCONNU, "Le contribuable n°" + ctbId + " n'existe pas.");
			return null;
		}
		else if (proprietaire instanceof MenageCommun) {
			// les ménages-communs ne possèdent pas de personnalité juridique et ne peuvent pas posséder d'immeubles.
			rapport.addError(numero, ErreurType.CTB_MENAGE_COMMUN, buildMenageErrorDetails((MenageCommun) proprietaire));
			return null;
		}
		else if (proprietaire instanceof Entreprise) {
			if (genrePersonne != GenrePersonne.Entreprise) {
				// il y a incohérence incompatible du type de contribuable entre les deux registres, on notifie et on ne traite pas l'immeuble
				rapport.addAVerifier(numero, ImportImmeublesResults.AVerifierType.TYPE_INCOHERENT_NON_TRAITE, "Type dans le RF = [" + genrePersonne + "], type dans Unireg = [Entreprise]");
			}
			rapport.addIgnore(numero, IgnoreType.CTB_ENTREPRISE);
			return null;
		}
		else if (!(proprietaire instanceof PersonnePhysique)) {
			rapport.addError(numero, ErreurType.BAD_CTB_TYPE, "Le contribuable n°" + proprietaire.getNumero() + " est de type [" + proprietaire.getClass().getSimpleName() + "].");
			return null;
		}

		if (genrePersonne != GenrePersonne.PersonnePhysique) {
			// malgré l'incohérence des types de contribuable, on traite quand même l'immeuble car le contribuable est une personne physique
			rapport.addAVerifier(numero, ImportImmeublesResults.AVerifierType.TYPE_INCOHERENT_TRAITE, "Type dans le RF = [" + genrePersonne + "], type dans Unireg = [PersonnePhysique]");
		}

		final Immeuble immeuble = new Immeuble();
		immeuble.setId(ctbId);
		immeuble.setDateDebut(dateDebut);
		immeuble.setDateFin(dateFin);
		immeuble.setNumero(numero);
		immeuble.setNature(nature);
		immeuble.setEstimationFiscale(estimationFiscale);
		immeuble.setDateEstimationFiscale(dateEstimationFiscale);
		immeuble.setAncienneEstimationFiscale(ancienneEstimationFiscale);
		immeuble.setGenrePropriete(genrePropriete);
		immeuble.setPartPropriete(partPropriete);
		immeuble.setLienRegistreFoncier(lienRegistreFoncier);
		immeuble.setProprietaire(proprietaire);

		return immeuble;
	}

	private String buildMenageErrorDetails(@NotNull MenageCommun menageCommun) {
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);
		if (ensemble.getPrincipal() == null) {
			return "Le contribuable n°" + menageCommun.getNumero() + " est un ménage commun vide.";
		}
		else if (ensemble.getConjoint() == null) {
			return "Le contribuable n°" + menageCommun.getNumero() + " est un ménage commun constitué du principal = {" + buildPersonnePhysiqueDetails(ensemble.getPrincipal()) +
					"} et d'un conjoint inconnu.";
		}
		else {
			return "Le contribuable n°" + menageCommun.getNumero() + " est un ménage commun constitué du principal = {" + buildPersonnePhysiqueDetails(ensemble.getPrincipal()) +
					"} et du conjoint = {" + buildPersonnePhysiqueDetails(ensemble.getConjoint()) + "}.";
		}
	}

	private String buildPersonnePhysiqueDetails(PersonnePhysique pp) {
		final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
		final Sexe sexe = tiersService.getSexe(pp);
		return String.format("numéro=%d, prénom='%s', nom='%s', date de naissance=%s, sexe=%s", pp.getNumero(), nomPrenom.getPrenom(), nomPrenom.getNom(),
				RegDateHelper.dateToDisplayString(tiersService.getDateNaissance(pp)), (sexe == null ? "inconnu" : (sexe == Sexe.MASCULIN ? "masculin" : "féminin")));
	}

	private static URL parseURL(String str) throws MalformedURLException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			return new URL(str);
		}
	}

	private static Long parseLong(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			return Long.parseLong(str);
		}
	}

	private static GenrePersonne parseGenrePersonne(String s) {
		if ("P".equals(s)) {
			return GenrePersonne.PersonnePhysique;
		}
		else if ("M".equals(s)) {
			return GenrePersonne.Entreprise;
		}
		else if ("DP".equals(s)) {
			return GenrePersonne.DroitPublic;
		}
		else {
			throw new IllegalArgumentException("Genre de personne inconnu = [" + s + "]");
		}
	}

	private static GenrePropriete parseGenrePropriete(String s) {
		final int genre = Integer.parseInt(s);
		switch (genre) {
		case 1:
			return GenrePropriete.INDIVIDUELLE;
		case 2:
			return GenrePropriete.COPROPRIETE;
		case 3:
			return GenrePropriete.PAR_ETAGES;
		default:
			throw new IllegalArgumentException("Genre de propriété inconnu = [" + genre + "]");
		}
	}

	private static Integer parseInteger(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			return Integer.valueOf(str);
		}
	}

	private static RegDate parseRegDate(String str) throws ParseException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			return RegDateHelper.displayStringToRegDate(str, false);
		}
	}
}
