package ch.vd.uniregctb.migration.pm.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeNatureDecisionTaxation;

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

	private final Set<Long> numerosContribuablesActifsPerception;
	private final RegDate seuilActivite;

	/**
	 * @param filename le fichier qui contient, ligne par ligne, les numéros des entreprises dont la perception affirme qu'ils sont toujours actifs
	 * @param seuilActivite si on trouve une activité de l'entreprise à cette date ou après, l'entreprise est considérée comme active
	 * @throws IOException en cas de souci avec le fichier fourni
	 */
	public ActivityManagerImpl(String filename, RegDate seuilActivite) throws IOException {

		// récupération des données fournies par la perception
		try (Reader reader = StringUtils.isNotBlank(filename) ? new FileReader(filename) : null) {
			this.numerosContribuablesActifsPerception = readPerceptionActiveIds(reader);
		}

		// stockage du seuil d'activité fourni
		this.seuilActivite = seuilActivite;

		// un peu de log...
		LOGGER.info("Nombre d'entreprises actives au niveau de la perception : " + this.numerosContribuablesActifsPerception.size());
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
			this.numerosContribuablesActifsPerception = readPerceptionActiveIds(reader);
		}

		// stockage du seuil d'activité fourni
		this.seuilActivite = seuilActivite;

		// un peu de log...
		LOGGER.info("Nombre d'entreprises actives au niveau de la perception : " + this.numerosContribuablesActifsPerception.size());
		LOGGER.info("Seuil d'activité : " + RegDateHelper.dateToDisplayString(this.seuilActivite));
	}

	/**
	 * Lecture depuis un fichier d'entrée de la liste des identifiants des entreprises dont la perception nous indique
	 * qu'elles sont actives
	 * @param reader reader sur le flux contenant les numéros de contribuable des entreprises concernées
	 * @return un ensemble des identifiants lus dans le flux donné
	 * @throws IOException en cas de problème avec le flux d'entrée
	 */
	@NotNull
	private static Set<Long> readPerceptionActiveIds(@Nullable Reader reader) throws IOException {
		// pour les tests de base, aucun fichier n'est fourni -> aucune entreprise active annoncée par la perception
		if (reader != null) {

			// d'abord une liste chaînée car on n'a aucune idée du nombre d'éléments
			final List<Long> liste = new LinkedList<>();

			final Pattern pattern = Pattern.compile("[0-9]{1,5}");      // les numéros de PM sont constitués d'un à 5 chiffres, pour l'instant...

			// remplissage de la liste d'après les lignes du fichier
			try (BufferedReader br = new BufferedReader(reader)) {

				// ligne par ligne, on lit les numéros de contribuables
				String ligne;
				while ((ligne = br.readLine()) != null) {
					final Matcher matcher = pattern.matcher(ligne);
					if (matcher.matches()) {
						final long id = Long.parseLong(matcher.group());
						liste.add(id);
					}
					else {
						LOGGER.warn("Ligne ignorée dans le fichier des contribuables actifs en perception : '" + ligne + "'");
					}
				}
			}

			// puis constitution d'un ensemble (la taille est maintenant connue, les éventuels doublons sont enlevés et la recherche facilitée)
			return new HashSet<>(liste);
		}
		else {
			LOGGER.warn("Aucune donnée de numéros de contribuables actifs en provenance de la perception...");
			return Collections.emptySet();
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
		return entreprise.getAssujettissements().stream()
				.filter(a -> a.getType() != RegpmTypeAssujettissement.SANS)
				.filter(a -> NullDateBehavior.LATEST.compare(seuil, a.getDateFin()) <= 0)
				.findAny()
				.isPresent();
	}

	/**
	 * @param entreprise entreprise dont l'état de taxation est à vérifier
	 * @return <code>true</code> s'il existe une taxation en cours à la date donnée ou plus tard
	 */
	private static boolean isActiveTaxation(RegpmEntreprise entreprise) {
		return entreprise.getDossiersFiscaux().stream()
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
	 * @return <code>true</code> si l'entreprise était listée dans le fichier fourni en entrée par la perception
	 */
	private boolean isActivePerception(RegpmEntreprise entreprise) {
		// Ici, on suppose encore une fois que le numéro utilisé dans Unireg (SIPF, en fait) est le même
		// que celui qui était connu dans le mainframe...
		return numerosContribuablesActifsPerception.contains(entreprise.getId());
	}
}

