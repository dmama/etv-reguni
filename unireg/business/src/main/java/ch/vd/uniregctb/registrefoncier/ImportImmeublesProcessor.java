package ch.vd.uniregctb.registrefoncier;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.ImmeubleDAO;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.rf.Proprietaire;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
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
	private static final String HEADER_ID_PROPRIETAIRE = "ID_PROPRIETAIRE";
	private static final String HEADER_ID_INDIVIDU_RF = "NO_INDIVIDU";
	private static final String HEADER_GENRE_PERSONNE = "GENRE_PERSONNE";
	private static final String HEADER_ID_IMMEUBLE = "ID_IMMEUBLE";
	private static final String HEADER_NO_IMMEUBLE = "NO_IMMEUBLE";
	private static final String HEADER_NOM_COMMUNE= "LI_COMMUNE_VD";
	private static final String HEADER_NATURE = "NATURE";
	private static final String HEADER_ESTIMATION_FISCALE = "ESTIMATION_FISCALE";
	private static final String HEADER_REF_ESTIMATION_FISCALE = "REFERENCE_EF";
	private static final String HEADER_GENRE_PROPRIETE = "GENRE_PROPRIETE";
	private static final String HEADER_TYPE_IMMEUBLE = "TYPE_IMMEUBLE";
	private static final String HEADER_PART_PROPRIETE = "PART_PROPRIETE";
	private static final String HEADER_DATE_VALID_RF = "DATE_VALIDATION_RF";
	private static final String HEADER_DATE_DEBUT = "DATE_DEPOT_PJ";
	private static final String HEADER_DATE_FIN = "DATE_FIN";
	private static final String HEADER_DATE_DERNIERE_MUT = "DATE_DERNIERE_MUTATION";
	private static final String HEADER_DERNIERE_MUTATION = "TYPE_DERNIERE_MUTATION";
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
	private final AdresseService adresseService;

	public ImportImmeublesProcessor(HibernateTemplate hibernateTemplate, ImmeubleDAO immeubleDAO, PlatformTransactionManager transactionManager, TiersDAO tiersDAO, TiersService tiersService,
	                                AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.immeubleDAO = immeubleDAO;
		this.tiersDAO = tiersDAO;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
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
		final ImportImmeublesResults rapportFinal = new ImportImmeublesResults(tiersService, adresseService);

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

		// Import de toutes les lignes du flux CSV
		processAllLines(status, rapportFinal, csvIterator);

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

	private void processAllLines(final StatusManager status, final ImportImmeublesResults rapportFinal, Scanner csvIterator) {
		// Lecture de l'entête
		final List<String> headers = extractHeaders(csvIterator.next());

		// Processing des lignes de données
		final BatchTransactionTemplateWithResults<String, ImportImmeublesResults> template = new BatchTransactionTemplateWithResults<>(csvIterator, BATCH_SIZE,
		                                                                                                                               Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<String, ImportImmeublesResults>() {

			@Override
			public ImportImmeublesResults createSubRapport() {
				return new ImportImmeublesResults(tiersService, adresseService);
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
		}, null);
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
		rapport.addTraite(immeuble.getNumero(), immeuble.getContribuable().getId());
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
		final HashMap<String, String> map = new HashMap<>();
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

		final String numero = StringUtils.trimToNull(data.get(HEADER_NO_IMMEUBLE));
		if (numero == null) {
			rapport.addError(numero, ErreurType.BAD_NUMERO, "");
			return null;
		}

		final String idProprietaire = data.get(HEADER_ID_PROPRIETAIRE);
		if (StringUtils.isBlank(idProprietaire)) {
			rapport.addError(numero, ErreurType.BAD_ID_PROPRIETAIRE, "Id du propriété RF = " + data.get(HEADER_ID_PROPRIETAIRE));
		}

		final long idIndividuRF;
		try {
			idIndividuRF = Long.parseLong(data.get(HEADER_ID_INDIVIDU_RF));
		}
		catch (NumberFormatException e) {
			rapport.addError(numero, ErreurType.BAD_ID_IND_RF, "Id de l'individu RF = " + data.get(HEADER_ID_INDIVIDU_RF));
			return null;
		}

		final String idImmeuble = data.get(HEADER_ID_IMMEUBLE);
		if (StringUtils.isBlank(idImmeuble)) {
			rapport.addError(numero, ErreurType.BAD_ID_IMMEUBLE, "Id de l'immeuble RF = " + data.get(HEADER_ID_IMMEUBLE));
		}

		final RegDate dateValidRF;
		try {
			dateValidRF = parseTimestamp(data.get(HEADER_DATE_VALID_RF));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_MODIF, "Date = " + data.get(HEADER_DATE_VALID_RF));
			return null;
		}

		final RegDate dateDebut;
		try {
			dateDebut = parseTimestamp(data.get(HEADER_DATE_DEBUT));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_DEBUT, "Date = " + data.get(HEADER_DATE_DEBUT));
			return null;
		}

		final RegDate dateFin;
		try {
			dateFin = parseTimestamp(data.get(HEADER_DATE_FIN));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_FIN, "Date = " + data.get(HEADER_DATE_FIN));
			return null;
		}

		final RegDate dateDerniereMutation;
		try {
			dateDerniereMutation = parseTimestamp(data.get(HEADER_DATE_DERNIERE_MUT));
		}
		catch (ParseException e) {
			rapport.addError(numero, ErreurType.BAD_DATE_DERNIERE_MUTATION, "Date = " + data.get(HEADER_DATE_DERNIERE_MUT));
			return null;
		}

		final TypeMutation derniereMutation;
		try {
			derniereMutation = parseTypeMutation(data.get(HEADER_DERNIERE_MUTATION));
		}
		catch (IllegalArgumentException e) {
			rapport.addError(numero, ErreurType.BAD_TYPE_DERNIERE_MUTATION, "Type = " + data.get(HEADER_DERNIERE_MUTATION));
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

		final String nomCommune = StringUtils.trimToNull(data.get(HEADER_NOM_COMMUNE));
		if (nomCommune == null) {
			rapport.addError(numero, ErreurType.BAD_NOM_COMMUNE, "");
			return null;
		}

		final String nature = StringUtils.trimToNull(data.get(HEADER_NATURE));
		// pas de test sur la nature : elle n'est pas forcément renseignée

		final Integer estimationFiscale;
		try {
			estimationFiscale = parseInteger(data.get(HEADER_ESTIMATION_FISCALE));
		}
		catch (NumberFormatException e) {
			rapport.addError(numero, ErreurType.BAD_EF, "Estimation fiscale = " + data.get(HEADER_ESTIMATION_FISCALE));
			return null;
		}

		final String referenceEstimationFiscale = StringUtils.trimToNull(data.get(HEADER_REF_ESTIMATION_FISCALE));

		final TypeImmeuble typeImmeuble;
		try {
			typeImmeuble = parseTypeImmeuble(data.get(HEADER_TYPE_IMMEUBLE));
		}
		catch (IllegalArgumentException e) {
			rapport.addError(numero, ErreurType.BAD_TYPE_IMMEUBLE, "Type = " + data.get(HEADER_TYPE_IMMEUBLE));
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
		else if (proprietaire instanceof PersonnePhysique && !((PersonnePhysique) proprietaire).isConnuAuCivil()) {
			rapport.addError(numero, ErreurType.PP_INCONNUE_AU_CIVIL, "Le contribuable n°" + proprietaire.getNumero() + " est inconnu au contrôle des habitants.");
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
		immeuble.setIdRF(idImmeuble);
		immeuble.setProprietaire(new Proprietaire(idProprietaire, idIndividuRF));
		immeuble.setDateValidRF(dateValidRF);
		immeuble.setDateDebut(dateDebut);
		immeuble.setDateFin(dateFin);
		immeuble.setNumero(numero);
		immeuble.setNomCommune(nomCommune);
		immeuble.setNature(nature);
		immeuble.setEstimationFiscale(estimationFiscale);
		immeuble.setReferenceEstimationFiscale(referenceEstimationFiscale);
		immeuble.setTypeImmeuble(typeImmeuble);
		immeuble.setGenrePropriete(genrePropriete);
		immeuble.setPartPropriete(partPropriete);
		immeuble.setLienRegistreFoncier(lienRegistreFoncier);
		immeuble.setContribuable(proprietaire);
		immeuble.setDateDerniereMutation(dateDerniereMutation);
		immeuble.setDerniereMutation(derniereMutation);

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

	private static Integer parseInteger(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			return Integer.parseInt(str);
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
			throw new IllegalArgumentException("Genre de personne inconnu = [" + s + ']');
		}
	}

	private static TypeImmeuble parseTypeImmeuble(String s) {
		if ("PPE".equalsIgnoreCase(s)) {
			return TypeImmeuble.PPE;
		}
		else if ("DDP".equalsIgnoreCase(s)) {
			return TypeImmeuble.DROIT_DISTINCT_ET_PERMANENT;
		}
		else if ("COP".equalsIgnoreCase(s)) {
			return TypeImmeuble.PART_DE_COPROPRIETE;
		}
		else if ("B-F".equalsIgnoreCase(s)) {
			return TypeImmeuble.BIEN_FOND;
		}
		else {
			throw new IllegalArgumentException("Type d'immeuble inconnu = [" + s + "]");
		}
	}


	private static TypeMutation parseTypeMutation(String s) {
		if (StringUtils.isBlank(s)) {
			return null;
		}
		if ("Achat".equalsIgnoreCase(s)) {
			return TypeMutation.ACHAT;
		}
		else if ("Augmentation".equalsIgnoreCase(s)) {
			return TypeMutation.AUGMENTATION;
		}
		else if ("Cession".equalsIgnoreCase(s)) {
			return TypeMutation.CESSION;
		}
		else if ("Constitution de PPE".equalsIgnoreCase(s)) {
			return TypeMutation.CONSTITUTION_PPE;
		}
		else if ("Constitution de parts de propriété".equalsIgnoreCase(s)) {
			return TypeMutation.CONSTITUTION_PARTS_PROPRIETE;
		}
		else if ("Délivrance de legs".equalsIgnoreCase(s)) {
			return TypeMutation.DELIVRANCE_LEGS;
		}
		else if ("Division de bien-fonds".equalsIgnoreCase(s)) {
			return TypeMutation.DIVISION_BIEN_FONDS;
		}
		else if ("Donation".equalsIgnoreCase(s)) {
			return TypeMutation.DONATION;
		}
		else if ("Echange".equalsIgnoreCase(s)) {
			return TypeMutation.ECHANGE;
		}
		else if ("Groupement de bien-fonds".equalsIgnoreCase(s)) {
			return TypeMutation.GROUPEMENT_BIEN_FONDS;
		}
		else if ("Jugement".equalsIgnoreCase(s)) {
			return TypeMutation.JUGEMENT;
		}
		else if ("Partage".equalsIgnoreCase(s)) {
			return TypeMutation.PARTAGE;
		}
		else if ("Réalisation forcée".equalsIgnoreCase(s)) {
			return TypeMutation.REALISATION_FORCEE;
		}
		else if ("Remaniement parcellaire".equalsIgnoreCase(s)) {
			return TypeMutation.REMANIEMENT_PARCELLAIRE;
		}
		else if ("Succession".equalsIgnoreCase(s)) {
			return TypeMutation.SUCCESSION;
		}
		else if ("Transfert".equalsIgnoreCase(s)) {
			return TypeMutation.TRANSFERT;
		}
		else if ("Fin de propriété".equalsIgnoreCase(s)) {
			return TypeMutation.FIN_DE_PROPRIETE;
		}
		throw new IllegalArgumentException("Type de mutation inconnu = [" + s + "]");
	}

	private static GenrePropriete parseGenrePropriete(String s) {
		// voir SIFISC-4187
		final int genre = Integer.parseInt(s);
		switch (genre) {
		case 1:
			return GenrePropriete.INDIVIDUELLE;
		case 2:
			return GenrePropriete.COMMUNE;
		case 3:
			return GenrePropriete.COPROPRIETE;
		default:
			throw new IllegalArgumentException("Genre de propriété inconnu = [" + genre + ']');
		}
	}

	private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSSSSSSS");

	protected static RegDate parseTimestamp(String str) throws ParseException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		else {
			final Date date = TIMESTAMP.parse(str);
			if (date == null) {
				throw new ParseException("Date '" + str + "' cannot be parsed.", 0);
			}
			final RegDate regDate = RegDateHelper.get(date);
			if (regDate == null) {
				throw new ParseException("Date '" + str + "' cannot be parsed.", 0);
			}
			return regDate;
		}
	}
}
