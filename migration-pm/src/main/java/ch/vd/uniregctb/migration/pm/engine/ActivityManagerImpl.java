package ch.vd.uniregctb.migration.pm.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeNatureDecisionTaxation;
import ch.vd.uniregctb.migration.pm.utils.DataLoadHelper;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;

/**
 * Entité qui maintient les flags d'activité pour les entreprises pendant la migration
 */
public class ActivityManagerImpl implements ActivityManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActivityManagerImpl.class);

	/**
	 * Etats des décisions de taxations qui indiquent une taxation en cours
	 */
	private static final Set<RegpmTypeEtatDecisionTaxation> ETATS_EN_COURS = EnumSet.of(RegpmTypeEtatDecisionTaxation.A_REVISER,
	                                                                                    RegpmTypeEtatDecisionTaxation.EN_RECLAMATION,
	                                                                                    RegpmTypeEtatDecisionTaxation.ERREUR_DE_CALCUL,
	                                                                                    RegpmTypeEtatDecisionTaxation.ERREUR_DE_TRANSCRIPTION);

	private final Map<Long, Boolean> donneesPerception;
	private final RegDate seuilActivite;

	/**
	 * @param filename le fichier qui contient, ligne par ligne, les numéros des entreprises dont la perception affirme qu'ils sont toujours actifs
	 * @param datesParticulieres bean qui permet d'accéder à la date du seuil d'activité (pour ce qui concerne l'assujettissement)
	 * @throws IOException en cas de souci avec le fichier fourni
	 */
	public ActivityManagerImpl(String filename, DatesParticulieres datesParticulieres) throws IOException {

		// récupération des données fournies par la perception
		try (Reader reader = StringUtils.isNotBlank(filename) ? new FileReader(filename) : null) {
			this.donneesPerception = readDonneesPerception(reader);
		}

		// stockage du seuil d'activité fourni
		this.seuilActivite = datesParticulieres.getSeuilActivite();

		// un peu de log...
		LOGGER.info("Nombre d'entreprises actives au niveau de la perception : " + this.donneesPerception.size());
		LOGGER.info("Seuil d'activité : " + RegDateHelper.dateToDisplayString(this.seuilActivite));
	}

	/**
	 * Constructeur utilisable pendant les tests (pour ne pas dépendre d'un fichier sur disque pour les données de la perception)
	 * @param is {@link InputStream} vers les données de la perception (consommé et fermé en sortie)
	 * @param seuilActivite si on trouve une activité de l'entreprise à cette date ou après, l'entreprise est considérée comme active
	 * @throws IOException en cas de souci avec le flux fourni
	 */
	ActivityManagerImpl(InputStream is, RegDate seuilActivite) throws IOException {

		// récupération des données fournies par la perception
		try (Reader reader = is != null ? new InputStreamReader(is) : null) {
			this.donneesPerception = readDonneesPerception(reader);
		}

		// stockage du seuil d'activité fourni
		this.seuilActivite = seuilActivite;

		// un peu de log...
		LOGGER.info("Nombre d'entreprises actives au niveau de la perception : " + this.donneesPerception.values().stream().filter(adbSeul -> !adbSeul).count());
		LOGGER.info("Seuil d'activité : " + RegDateHelper.dateToDisplayString(this.seuilActivite));
	}

	/**
	 * Lecture depuis un fichier d'entrée de la liste des identifiants des entreprises dont la perception nous indique
	 * qu'elles sont actives
	 * @param reader reader sur le flux contenant les numéros de contribuable des entreprises concernées
	 * @return une map des identifiants lus dans le flux donné, associés à un booléen 'ADB seul' (= seulement acte de défaut de bien)
	 * @throws IOException en cas de problème avec le flux d'entrée
	 */
	@NotNull
	private static Map<Long, Boolean> readDonneesPerception(@Nullable Reader reader) throws IOException {
		// pour les tests de base, aucun fichier n'est fourni -> aucune entreprise active annoncée par la perception
		if (reader != null) {
			// on constitue la map (les éventuels doublons sont enlevés et la recherche facilitée)
			final Pattern pattern = Pattern.compile("([0-9]{1,10});([ON])");
			try (BufferedReader br = new BufferedReader(reader)) {
				final List<Pair<Long, Boolean>> data = DataLoadHelper.loadData(br, pattern, matcher -> Pair.of(Long.parseLong(matcher.group(1)), "O".equals(matcher.group(2))));
				return data.stream()
						.collect(Collectors.toMap(Pair::getLeft,
						                          Pair::getRight,
						                          (b1, b2) -> b1 && b2));
			}
		}
		else {
			LOGGER.warn("Aucune donnée de numéros de contribuables actifs en provenance de la perception...");
			return Collections.emptyMap();
		}
	}

	@Override
	public boolean isActive(RegpmEntreprise entreprise) {
		return isActiveAssujettissement(entreprise, seuilActivite)
				|| isActiveTaxation(entreprise)
				|| isActivePerception(entreprise);
	}

	/**
	 * @param entreprise entreprise dont l'assujettissement est à vérifier
	 * @param seuil date de référence du test
	 * @return <code>true</code> s'il existe un assujettissement significatif à la date donnée ou plus tard
	 */
	private static boolean isActiveAssujettissement(RegpmEntreprise entreprise, RegDate seuil) {
		// [SIFISC-17160] En fonction de la catégorie d'entreprise, l'activité n'est pas tout-à-fait mesurée de la même façon
		final RegpmCategoriePersonneMorale categorie = getDerniereCategoriePersonneMorale(entreprise);

		// en particulier, pour les sociétés de personnes (SC, SNC, l'activité ne se mesure pas à l'aune d'un assujettissement)
		if (categorie == RegpmCategoriePersonneMorale.SP) {
			return entreprise.getQuestionnairesSNC().stream()
					.filter(q -> q.getEtat() != RegpmTypeEtatQuestionnaireSNC.ANNULE)
					.filter(q -> q.getDateAnnulation() == null)
					.filter(q -> q.getAnneeFiscale() >= seuil.year())
					.findAny()
					.isPresent();
		}
		else {
			return entreprise.getAssujettissements().stream()
					.filter(a -> a.getType() != RegpmTypeAssujettissement.SANS)
					.filter(a -> NullDateBehavior.LATEST.compare(seuil, a.getDateFin()) <= 0)
					.findAny()
					.isPresent();
		}
	}

	/**
	 * @param entreprise une entreprise de RegPM
	 * @return sa dernière catégorie connue
	 */
	private static RegpmCategoriePersonneMorale getDerniereCategoriePersonneMorale(RegpmEntreprise entreprise) {

		// d'abord un calcul strict en ignorant les dates de début de validité nulles
		// (tout comme dans le code de migration de la forme juridique)

		final RegpmCategoriePersonneMorale categorieStricte = entreprise.getFormesJuridiques().stream()
				.filter(fj -> !fj.isRectifiee())
				.filter(fj -> NullDateBehavior.LATEST.compare(fj.getDateValidite(), RegDate.get()) <= 0)
				.max(Comparator.naturalOrder())
				.map(RegpmFormeJuridique::getType)
				.map(RegpmTypeFormeJuridique::getCategorie)
				.orElse(null);
		if (categorieStricte != null) {
			return categorieStricte;
		}

		// dans le cas où aucune catégorie n'a été trouvée en raison d'une date de début de validité nulle,
		// on va la prendre quand-même...

		return entreprise.getFormesJuridiques().stream()
				.filter(fj -> !fj.isRectifiee())
				.filter(fj -> fj.getDateValidite() == null)
				.max(Comparator.comparingInt(fj -> fj.getPk().getSeqNo()))
				.map(RegpmFormeJuridique::getType)
				.map(RegpmTypeFormeJuridique::getCategorie)
				.orElse(null);
	}

	/**
	 * @param entreprise entreprise dont l'état de taxation est à vérifier
	 * @return <code>true</code> s'il existe une taxation en cours à la date donnée ou plus tard
	 */
	private static boolean isActiveTaxation(RegpmEntreprise entreprise) {
		return entreprise.getDossiersFiscaux().stream()
				.filter(df -> df.getModeImposition() == RegpmModeImposition.POST)
				.map(RegpmDossierFiscal::getEnvironnementsTaxation)
				.flatMap(Set::stream)
				.map(RegpmEnvironnementTaxation::getDecisionsTaxation)
				.flatMap(Set::stream)
				.filter(RegpmDecisionTaxation::isDerniereTaxation)
				.filter(decision -> decision.getEtatCourant() != RegpmTypeEtatDecisionTaxation.ANNULEE)
				.filter(decision -> decision.getNatureDecision() == RegpmTypeNatureDecisionTaxation.PROVISOIRE || ETATS_EN_COURS.contains(decision.getEtatCourant()))
				.findAny()
				.isPresent();
	}

	/**
	 * @param entreprise entreprise dont l'état de perception est à vérifier
	 * @return <code>true</code> si l'entreprise était listée comme active dans le fichier fourni en entrée par la perception
	 */
	private boolean isActivePerception(RegpmEntreprise entreprise) {
		// Ici, on suppose encore une fois que le numéro utilisé dans Unireg (SIPF, en fait) est le même
		// que celui qui était connu dans le mainframe...
		final Boolean adbSeul = donneesPerception.get(entreprise.getId());
		return adbSeul != null && !adbSeul;
	}
}

