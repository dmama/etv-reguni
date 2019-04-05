package ch.vd.unireg.metier.bouclement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.BouclementHelper;
import ch.vd.unireg.common.MovingWindow;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.MotifFor;

/**
 * Implémentation du service de fourniture d'information autour des bouclements et exercices comptables
 */
public class BouclementServiceImpl implements BouclementService {

	private ParametreAppService parametreAppService;

	@NotNull
	@Override
	public List<ExerciceCommercial> getExercicesCommerciaux(Entreprise entreprise) {
		return getExercicesCommerciauxJusqua(entreprise, RegDate.get());
	}

	@Nullable
	@Override
	public ExerciceCommercial getExerciceCommercialAt(Entreprise entreprise, RegDate date) {
		final RegDate dateLimite = RegDateHelper.maximum(date, RegDate.get(), NullDateBehavior.EARLIEST);
		return DateRangeHelper.rangeAt(getExercicesCommerciauxJusqua(entreprise, dateLimite), date);
	}

	/**
	 * @param entreprise entreprise dont on veut les exercices commerciaux
	 * @param dateLimite date de référence (a priori aujourd'hui ou dans le futur) pour la détermination du dernier exercice à renvoyer (si l'entreprise est encore active)
	 * @return les exercices commerciaux de cette entreprise (jusqu'à au plus tard l'exercice de la date de référence, ou, s'il n'y en a plus, le dernier exercice connu)
	 */
	private List<ExerciceCommercial> getExercicesCommerciauxJusqua(Entreprise entreprise, @NotNull RegDate dateLimite) {
		final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
		final List<Bouclement> bouclements = AnnulableHelper.sansElementsAnnules(entreprise.getBouclements());
		final boolean noFors = forsPrincipaux.isEmpty();
		final boolean noBouclements = bouclements.isEmpty();

		if (noFors && noBouclements) {
			// rien de rien...
			return Collections.emptyList();
		}

		final RegDate dateDebutPremierExercice;
		if (entreprise.getDateDebutPremierExerciceCommercial() != null) {
			dateDebutPremierExercice = entreprise.getDateDebutPremierExerciceCommercial();
		}
		else if (noFors) {
			// on va supposer une date de début au lendemain du premier bouclement connu
			dateDebutPremierExercice = getDateProchainBouclement(bouclements, RegDateHelper.getEarlyDate(), false).getOneDayAfter();
		}
		else {
			// création à l'ouverture du premier for principal ? -> c'est la date de début du premier exercice
			final ForFiscalPrincipalPM premierForPrincipal = forsPrincipaux.get(0);
			final MotifFor premierMotif = premierForPrincipal.getMotifOuverture();
			if (premierMotif == MotifFor.DEBUT_EXPLOITATION || noBouclements) {
				// il s'agit donc de la création de la société, ou sinon, on n'a pas vraiment d'autre donnée de toute façon
				dateDebutPremierExercice = premierForPrincipal.getDateDebut();
			}
			else {
				// il s'agit donc d'un déménagement (ou de la création d'un établissement ou l'achat d'un immeuble...) par exemple, la PM existait
				// déjà avant avec des données connues de bouclements
				final RegDate dateBouclementConnueAvantDebutFor = getDateDernierBouclement(bouclements, premierForPrincipal.getDateDebut(), false);
				if (dateBouclementConnueAvantDebutFor == null) {
					// pas de bouclement connu avant le démarrage du for, on prend la date du for
					// TODO [SIPM] date de début du for ou une année avant le premier bouclement connu après le début du for ?
					dateDebutPremierExercice = premierForPrincipal.getDateDebut();
				}
				else {
					dateDebutPremierExercice = dateBouclementConnueAvantDebutFor.getOneDayAfter();
				}
			}
		}

		final RegDate dateFinDernierExercice;
		if (noFors) {
			// la seule limite de fin sera celle de l'exercice courant
			dateFinDernierExercice = getDateProchainBouclement(bouclements, dateLimite, true);
		}
		else {
			// ici, nous avons des fors principaux

			final ForFiscalPrincipalPM dernierForPrincipal = forsPrincipaux.get(forsPrincipaux.size() - 1);
			if (dernierForPrincipal.getDateFin() != null) {
				// [SIFISC-17850] si le dernier for principal est fermé, on s'arrête là, sauf si le motif de fermeture est "FAILLITE"
				// (s'il n'y a pas de cycle de bouclements connu, on s'arrête quand-même à la fin du for)
				if (dernierForPrincipal.getMotifFermeture() == MotifFor.FAILLITE && !noBouclements) {
					// en cas de faillite, on continue jusqu'à la fin du cycle en cours
					dateFinDernierExercice = getDateProchainBouclement(bouclements, dernierForPrincipal.getDateFin(), true);
				}
				else {
					dateFinDernierExercice = dernierForPrincipal.getDateFin();
				}
			}
			else if (noBouclements) {
				// arbitrairement, fin de l'exercice à la fin de cette année
				dateFinDernierExercice = RegDate.get(dateLimite.year(), 12, 31);
			}
			else {
				// for encore ouvert -> la seule limite de fin sera celle de l'exercice courant
				dateFinDernierExercice = getDateProchainBouclement(bouclements, dateLimite, true);
			}
		}

		return getExercicesCommerciaux(bouclements, new DateRangeHelper.Range(dateDebutPremierExercice, dateFinDernierExercice), false);
	}

	/**
	 * @param src une collection de bouclements
	 * @return une map filtrée (sans les bouclements annulés) et indexée/triée (par date de début)
	 */
	@NotNull
	private static NavigableMap<RegDate, Bouclement> buildFilteredSortedMap(Iterable<Bouclement> src) {
		final NavigableMap<RegDate, Bouclement> map = new TreeMap<>();
		if (src != null) {
			for (Bouclement bouclement : src) {
				if (!bouclement.isAnnule()) {
					map.put(bouclement.getDateDebut(), bouclement);
				}
			}
		}
		return map;
	}

	/**
	 * Apparemment, dans les RegDate, 28.02.2015 + 12M -> 28.2.2016 même si 2016 est bissextile
	 * @param date date de départ
	 * @param nbMonths nombre de mois (positif !)
	 * @param ancrageOnEndOfMonth <code>true</code> si on doit se coller à la fin du mois
	 * @return la date de départ déplacée de <i>n</i> mois vers le futur
	 * @throws java.lang.IllegalArgumentException si le nombre de mois demandé est strictement négatif
	 */
	private static RegDate addMonths(RegDate date, int nbMonths, boolean ancrageOnEndOfMonth) {
		if (nbMonths < 0) {
			throw new IllegalArgumentException("Seuls les déplacements vers le futur sont supportés ici.");
		}
		final RegDate brutto = date.addMonths(nbMonths);
		return ancrageOnEndOfMonth ? brutto.getLastDayOfTheMonth() : brutto;
	}

	@Override
	public RegDate getDateProchainBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee) {
		final NavigableMap<RegDate, Bouclement> map = buildFilteredSortedMap(bouclements);
		return getDateProchainBouclement(map, dateReference, dateReferenceAcceptee);
	}

	private static RegDate getDateProchainBouclement(NavigableMap<RegDate, Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee) {
		final RegDate premiereDateAcceptable = dateReferenceAcceptee ? dateReference : dateReference.getOneDayAfter();
		final Map.Entry<RegDate, Bouclement> active = bouclements.floorEntry(premiereDateAcceptable);
		final Map.Entry<RegDate, Bouclement> next = bouclements.higherEntry(premiereDateAcceptable);

		if (active == null && next == null) {
			// aucun bouclement connu... pas de date à proposer...
			return null;
		}
		else if (active == null) {
			// date de référence fournie avant le premier ancrage connu, donc la prochaine date de bouclement est la première de la première périodicité
			final Bouclement nextBouclement = next.getValue();
			return nextBouclement.getAncrage().nextAfterOrEqual(nextBouclement.getDateDebut());
		}

		// nous somme au milieu d'une période active...
		// on reprend depuis l'ancrage initial de la période active, en suivant la période
		final Bouclement activeBouclement = active.getValue();
		final RegDate seuilNext = next == null ? null : next.getKey();      // démarrage de la prochaine périodicité
		final DayMonth ancrageCourant = activeBouclement.getAncrage();
		final boolean ancrageOnEndOfMonth = ancrageCourant.isEndOfMonth();
		final int periodeMois = activeBouclement.getPeriodeMois();
		if (periodeMois <= 0) {
			throw new IllegalArgumentException("Le champ 'periodeMois' d'un bouclement ne devrait jamais être négatif ou nul (trouvé " + periodeMois + ") !");
		}

		RegDate candidate = ancrageCourant.nextAfterOrEqual(activeBouclement.getDateDebut());
		while (candidate.isBefore(premiereDateAcceptable) && (seuilNext == null || candidate.isBefore(seuilNext))) {
			candidate = addMonths(candidate, periodeMois, ancrageOnEndOfMonth);
		}

		// si on a dépassé la date de début de la prochaine périodicité, c'est elle qui dirige, maintenant...
		if (seuilNext != null && candidate.isAfterOrEqual(seuilNext)) {
			// appel récursif pour repartir à chaque changement de périodicité
			return getDateProchainBouclement(bouclements, seuilNext, true);
		}
		else {
			return candidate;
		}
	}

	@Override
	public RegDate getDateDernierBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee) {
		final NavigableMap<RegDate, Bouclement> map = buildFilteredSortedMap(bouclements);
		return getDateDernierBouclement(map, dateReference, dateReferenceAcceptee);
	}

	private static RegDate getDateDernierBouclement(NavigableMap<RegDate, Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee) {
		final RegDate derniereDateAcceptable = dateReferenceAcceptee ? dateReference : dateReference.getOneDayBefore();
		final Map.Entry<RegDate, Bouclement> active = bouclements.floorEntry(derniereDateAcceptable);

		// pas d'active -> pas de bouclement antérieur
		if (active == null) {
			return null;
		}

		// première date de bouclement dans la périodicité active
		final RegDate premiereDate = active.getValue().getAncrage().nextAfterOrEqual(active.getKey());
		if (premiereDate.isBeforeOrEqual(derniereDateAcceptable)) {
			// tout se passe dans la périodicité active
			final int periodeMois = active.getValue().getPeriodeMois();
			if (periodeMois <= 0) {
				throw new IllegalArgumentException("Le champ 'periodeMois' d'un bouclement ne devrait jamais être négatif ou nul (trouvé " + periodeMois + ") !");
			}

			// tout se passe dans la périodicité active
			RegDate last = premiereDate;
			RegDate next;
			while ((next = last.addMonths(periodeMois)).isBeforeOrEqual(derniereDateAcceptable)) {
				last = next;
			}
			return last;
		}
		else {
			// c'est donc dans une périodicité antérieure...
			return getDateDernierBouclement(bouclements, active.getKey().getOneDayBefore(), true);
		}
	}

	@NotNull
	@Override
	public List<ExerciceCommercial> getExercicesCommerciaux(Collection<Bouclement> bouclements, @NotNull DateRange range, boolean intersecting) {
		final NavigableMap<RegDate, Bouclement> map = buildFilteredSortedMap(bouclements);
		final List<ExerciceCommercial> liste = new LinkedList<>();

		if (range.getDateDebut() == null || range.getDateFin() == null) {
			throw new IllegalArgumentException("Le range ne doit être ouvert ni à gauche, ni à droite (trouvé " + DateRangeHelper.toDisplayString(range) + ")");
		}

		RegDate fin = getDateDernierBouclement(map, range.getDateDebut(), false);
		if (fin == null) {
			fin = range.getDateDebut().getOneDayBefore();
		}
		while (fin.isBefore(range.getDateFin())) {
			final RegDate debut = fin.getOneDayAfter();
			fin = getDateProchainBouclement(map, debut, true);
			if (fin == null) {
				// cela signifie en fait qu'il n'y a aucun bouclement... le range retourné est donc assez arbitraire !
				fin = range.getDateFin();
			}
			liste.add(new ExerciceCommercial(debut, fin));
		}

		// et on ajoute le rognage si nécessaire
		if (!liste.isEmpty() && !intersecting) {
			// le commencement
			final ListIterator<ExerciceCommercial> iterPremier = liste.listIterator();
			final ExerciceCommercial premier = iterPremier.next();
			if (range.getDateDebut().isAfter(premier.getDateDebut())) {
				iterPremier.set(new ExerciceCommercial(range.getDateDebut(), premier.getDateFin()));
			}

			// la fin
			final ListIterator<ExerciceCommercial> iterDernier = liste.listIterator(liste.size());
			final ExerciceCommercial dernier = iterDernier.previous();
			if (range.getDateFin().isBefore(dernier.getDateFin())) {
				iterDernier.set(new ExerciceCommercial(dernier.getDateDebut(), range.getDateFin()));
			}
		}

		// transformation en ArrayList maintenant que l'on connaît la taille
		return new ArrayList<>(liste);
	}

	/**
	 * @param source collection d'entrée
	 * @param <T> types des objets dans la collection
	 * @return ensemble trié des valeurs (non-nulles) présentes dans la collection d'entrée
	 */
	private static <T extends Comparable<T>> NavigableSet<T> sortAndRemoveNull(Collection<T> source) {
		final Set<T> hashSet = new HashSet<>();
		if (source != null) {
			hashSet.addAll(source);
		}
		hashSet.remove(null);
		return new TreeSet<>(hashSet);
	}

	/**
	 * @param origin date de départ
	 * @param target date d'arrivée
	 * @return nombre de mois minimal pour partir de la date de départ et arriver au moins à la date d'arrivée
	 */
	private static int getMonths(@NotNull RegDate origin, @NotNull RegDate target) {

		final DayMonth dmOrigin = DayMonth.get(origin);
		final DayMonth dmTarget = DayMonth.get(target);

		// il existe un nombre entier d'année entre les deux points ?
		if (dmOrigin.isEndOfMonth() && dmTarget.isEndOfMonth() && dmOrigin.month() == dmTarget.month()) {
			return 12 * (target.year() - origin.year());
		}
		// il existe un nombre entier de mois entre les deux points ?
		else if (DayMonth.isSameDayOfMonth(dmOrigin, dmTarget)) {
			return 12 * (target.year() - origin.year()) + target.month() - origin.month();
		}
		else {
			// on construit une période qui passe de manière certaine au dessus du prochain point
			final int pm =  12 * (target.year() - origin.year()) + target.month() - origin.month();
			return origin.addMonths(pm).isBefore(target) ? pm + 1 : pm;
		}
	}

	/**
	 * @param reference une date de référence
	 * @return le premier jour du mois de la date de référence
	 */
	private static RegDate getDebutMois(RegDate reference) {
		return RegDate.get(reference.year(), reference.month(), 1);
	}

	@Override
	public List<Bouclement> extractBouclementsDepuisDates(Collection<RegDate> datesBouclements, int periodeMoisFinale) {

		// blindage simple contre les collections vides/nulles en entrée
		final NavigableSet<RegDate> sortedDates = sortAndRemoveNull(datesBouclements);
		if (sortedDates.isEmpty()) {
			return Collections.emptyList();
		}

		// la liste finale
		final List<Bouclement> liste = new LinkedList<>();

		// dernier bouclement inséré dans la collection finale (= bouclement actif)
		Bouclement tmp = null;

		// on boucle sur les dates, en ayant toujours en tête la suivante et la précédente
		final MovingWindow<RegDate> wnd = new MovingWindow<>(new ArrayList<>(sortedDates));
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<RegDate> snap = wnd.next();
			final RegDate dateCourante = snap.getCurrent();
			final RegDate dateSuivante = snap.getNext();
			final RegDate datePrecedente = snap.getPrevious();
			final RegDate dateDebutChoisie;

			if (tmp != null) {

				// tmp != null -> ce n'est donc pas le premier passage, donc datePrecedente est forcément non-vide
				final DayMonth dmDatePrecedente = DayMonth.get(datePrecedente);
				final RegDate dateAttendueSansChangement = addMonths(datePrecedente, tmp.getPeriodeMois(), dmDatePrecedente.isEndOfMonth());
				if (dateCourante == dateAttendueSansChangement && (dateSuivante != null || periodeMoisFinale == tmp.getPeriodeMois())) {
					// on continue sur la même lancée -> pas de changement de direction à prévoir
					continue;
				}

				// plage possible pour le changement de direction ?
				{
					final RegDate max = RegDateHelper.minimum(dateCourante, dateAttendueSansChangement.getOneDayBefore(), NullDateBehavior.LATEST);
					final RegDate min = datePrecedente.getOneDayAfter();
					final RegDate dateDebutMoisMax = getDebutMois(max);
					if (dateDebutMoisMax.isAfterOrEqual(min)) {
						// on essaie de mettre les dates de début en début de mois quand c'est possible
						dateDebutChoisie = dateDebutMoisMax;
					}
					else {
						// début de mois pas possible : on prend la date minimale qui fait le boulot
						dateDebutChoisie = min;
					}
				}

				// allons-nous y arriver comme ça ?
				if (DayMonth.get(dateCourante).nextAfterOrEqual(dateDebutChoisie) != dateCourante) {
					// a priori, cela signifie que l'axe de changement de direction est plus sur la longueur de la période
					// -> on va se replacer avant la date précédente et poser un changement de longueur de période là
					final RegDate debut;
					{
						final RegDate min = snap.getPreviousBeforePrevious() != null ? snap.getPreviousBeforePrevious().getOneDayAfter() : getDebutMois(datePrecedente);
						final RegDate debutMoisMax = getDebutMois(datePrecedente);
						debut = debutMoisMax.isAfterOrEqual(min) ? debutMoisMax : min;
					}

					// on introduit un bouclement qui fait faire le saut directement
					final int periode = getMonths(datePrecedente, dateCourante);
					tmp = new Bouclement();
					tmp.setDateDebut(debut);
					tmp.setAncrage(DayMonth.get(datePrecedente));
					tmp.setPeriodeMois(periode);
					liste.add(tmp);

					// si on était sur la dernière date et que l'appelant veut une période finale différente de la période active,
					// il faut redresser le cap sur la fin
					if (dateSuivante == null && periode != periodeMoisFinale) {
						final RegDate min = datePrecedente.getOneDayAfter();
						final RegDate debutMoisMax = getDebutMois(dateCourante);
						final RegDate debutDernier = debutMoisMax.isAfterOrEqual(min) ? debutMoisMax : min;
						tmp = new Bouclement();
						tmp.setDateDebut(debutDernier);
						tmp.setAncrage(DayMonth.get(dateCourante));
						tmp.setPeriodeMois(periodeMoisFinale);
						liste.add(tmp);
					}

					continue;
				}
			}
			else {
				// on essaie de mettre les dates de début en début de mois
				dateDebutChoisie = getDebutMois(dateCourante);
			}

			// écart entre la date courante et la suivante ?
			final int periodeMois = dateSuivante == null ? periodeMoisFinale : getMonths(dateCourante, dateSuivante);
			int periodeMoisChoisie = periodeMois;

			// mais si l'écart n'est modifié que temporairement (= s'il revient à la même valeur juste après), ce qui correspond à un cas de décalage de date de bouclement
			// en conservant le même cycle, on peut peut-être s'économiser un peu de sueur
			final RegDate dateSuivanteSuivante = snap.getNextAfterNext();
			if (tmp != null && dateSuivanteSuivante != null) {
				// si la dateSuivanteSuivante n'est pas nulle, c'est que la dateSuivante n'est pas nulle non plus (on a enlevé les nulls...)
				//noinspection ConstantConditions
				final int periodeMoisSuivante = getMonths(dateSuivante, dateSuivanteSuivante);
				if (periodeMoisSuivante == tmp.getPeriodeMois() && periodeMois < 2 * periodeMoisSuivante) {
					periodeMoisChoisie = periodeMoisSuivante;
				}
			}

			tmp = new Bouclement();
			tmp.setDateDebut(dateDebutChoisie);
			tmp.setAncrage(DayMonth.get(dateCourante));
			tmp.setPeriodeMois(periodeMoisChoisie);
			liste.add(tmp);
		}

		// repassage en ArrayList une fois la taille connue
		return new ArrayList<>(liste);
	}

	@Override
	public void setDateDebutPremierExerciceCommercial(@NotNull Entreprise entreprise, @NotNull RegDate nouvelleDate) throws BouclementException {

		final RegDate ancienneDate = entreprise.getDateDebutPremierExerciceCommercial();

		// la date de début du premier exercice commercial ne doit pas s'écarter trop de la date de fin de l'exercice
		// (comme c'est le premier, on accorde un peu plus...) N'est valable que si une date de début de premièpre exercice est
		//renseignée
		final List<ExerciceCommercial> anciensExercicesCommerciaux = getExercicesCommerciaux(entreprise);
		final int anneePremierBouclement = anciensExercicesCommerciaux.get(0).getDateFin().year();
		if (entreprise.hasDateDebutPremierExercice() &&  anneePremierBouclement - nouvelleDate.year() > 1) {
			throw new BouclementException(String.format("Impossible de modifier ou d'ajouter la date de début du premier exercice commercial du %s au %s car celui-ci s'étendrait alors sur plus de deux années civiles.",
			                                            RegDateHelper.dateToDisplayString(ancienneDate),
			                                            RegDateHelper.dateToDisplayString(nouvelleDate)));
		}

		// les autres problèmes seront détectés à la validation de l'entreprise...
		entreprise.setDateDebutPremierExerciceCommercial(nouvelleDate);
	}

	@Override
	public void corrigeDateDebutPremierExerciceCommercial(@NotNull Entreprise entreprise, @NotNull RegDate nouvelleDate) throws BouclementException {

		final RegDate ancienneDate = entreprise.getDateDebutPremierExerciceCommercial();

		// 1. remplacement de l'ancienne date par la nouvelle et impact jusqu'à la prochaine période fixée
		final List<ExerciceCommercial> anciensExercicesCommerciaux = getExercicesCommerciaux(entreprise);
		ExerciceCommercial premierExerciceCommercial = anciensExercicesCommerciaux.get(0);
		RegDate anciennePremiereDateDebutExerciceCommercial = null;
		//SIFISC-30422 on a une date de début d'exercice renseigné on active le contrôle
		if (premierExerciceCommercial != null && entreprise.hasDateDebutPremierExercice()) {
			anciennePremiereDateDebutExerciceCommercial = premierExerciceCommercial.getDateDebut();
			if (nouvelleDate.isAfterOrEqual(premierExerciceCommercial.getDateDebut())) {
				throw new BouclementException(String.format("La nouvelle date de début (%s) doit être inférieure à celle du premier exercice commercial.",
				                                        RegDateHelper.dateToDisplayString(nouvelleDate)));
			}
		}

		entreprise.setDateDebutPremierExerciceCommercial(nouvelleDate);

		final SortedSet<RegDate> nouvellesDatesBouclement = new TreeSet<>();
		// Ajout des exercices commerciaux existants
		for (ExerciceCommercial exercice : anciensExercicesCommerciaux) {
			nouvellesDatesBouclement.add(exercice.getDateFin());
		}
		// Ajout des nouvelles dates de bouclement depuis la nouvelle date de début
		if (anciennePremiereDateDebutExerciceCommercial != null) {
			for (RegDate date = nouvelleDate; date.compareTo(anciennePremiereDateDebutExerciceCommercial) < 0; date = date.addYears(1)) {
				RegDate dateFinBouclement = date.addYears(1).getOneDayBefore();
				if (dateFinBouclement.isAfterOrEqual(anciennePremiereDateDebutExerciceCommercial)) {
					dateFinBouclement = anciennePremiereDateDebutExerciceCommercial.getOneDayBefore();
				}
				nouvellesDatesBouclement.add(dateFinBouclement);
			}
		}

		// 2. re-calcul des cycles
		final List<Bouclement> nouveauxBouclements = extractBouclementsDepuisDates(nouvellesDatesBouclement, 12);

		// 3. comparaison avant/après et application des différences
		BouclementHelper.resetBouclements(entreprise, nouveauxBouclements);

		// 4. contrôle final des dates de bouclements
		controleDatesBouclements(ancienneDate, nouvelleDate, anciensExercicesCommerciaux, nouvellesDatesBouclement, entreprise);

	}

	@Override
	public void changeDateFinBouclement(@NotNull Entreprise entreprise, @NotNull RegDate ancienneDate, @NotNull RegDate nouvelleDate) throws BouclementException {

		// premier contrôle : s'il y a une DI non-annulée qui intersecte la période entre les deux dates, on refuse la modification
		final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
		final DateRange deplacement = new DateRangeHelper.Range(RegDateHelper.minimum(ancienneDate, nouvelleDate, NullDateBehavior.LATEST),
		                                                        RegDateHelper.maximum(ancienneDate, nouvelleDate, NullDateBehavior.EARLIEST));
		if (DateRangeHelper.intersect(deplacement, dis)) {
			throw new BouclementException(String.format("Le déplacement de la date de bouclement du %s au %s est impossible car au moins une déclaration d'impôt est impactée.",
			                                        RegDateHelper.dateToDisplayString(ancienneDate),
			                                        RegDateHelper.dateToDisplayString(nouvelleDate)));
		}

		// on fait la modification demandée
		// 1. on détermine les nouvelles dates de bouclement (remplacement de l'ancienne date par la nouvelle et impact jusqu'à la prochaine période fixée)
		final List<ExerciceCommercial> anciensExercicesCommerciaux = getExercicesCommerciaux(entreprise);
		final RegDate prochainBouclementFixeApresNouvelleDate = getPremierBouclementFixeApres(nouvelleDate, dis);
		final SortedSet<RegDate> nouvellesDatesBouclement = new TreeSet<>();
		for (ExerciceCommercial exercice : anciensExercicesCommerciaux) {
			if (!deplacement.isValidAt(exercice.getDateFin()) && !RegDateHelper.isBetween(exercice.getDateFin(), nouvelleDate, prochainBouclementFixeApresNouvelleDate, NullDateBehavior.LATEST)) {
				nouvellesDatesBouclement.add(exercice.getDateFin());
			}
		}
		nouvellesDatesBouclement.add(nouvelleDate);
		if (prochainBouclementFixeApresNouvelleDate != null) {
			for (RegDate date = nouvelleDate.addYears(1); date.compareTo(prochainBouclementFixeApresNouvelleDate) < 0; date = date.addYears(1)) {
				nouvellesDatesBouclement.add(date);
			}
		}

		// 2. re-calcul des cycles
		final List<Bouclement> nouveauxBouclements = extractBouclementsDepuisDates(nouvellesDatesBouclement, 12);

		// 3. comparaison avant/après et application des différences
		BouclementHelper.resetBouclements(entreprise, nouveauxBouclements);

		// 4. contrôle final des dates de bouclements
		controleDatesBouclements(ancienneDate, nouvelleDate, anciensExercicesCommerciaux, nouvellesDatesBouclement, entreprise);
	}

	private void controleDatesBouclements(RegDate ancienneDate, RegDate nouvelleDate, List<ExerciceCommercial> anciensExercicesCommerciaux,
	                                      SortedSet<RegDate> nouvellesDatesBouclement, Entreprise entreprise) {
		// contrôle des dates (au moins un bouclement par an sauf potentiellement la première année)
		// ([SIFISC-18030] ce contrôle n'est effectif qu'à partir de la première année vraiment "unireg" des PM...)
		final Set<Integer> anneesBouclements = new HashSet<>(nouvellesDatesBouclement.size());
		for (RegDate dateBouclement : nouvellesDatesBouclement) {
			anneesBouclements.add(dateBouclement.year());
		}
		final int premiereAnnee = nouvellesDatesBouclement.first().year();
		final int derniereAnnee = nouvellesDatesBouclement.last().year();
		final int debutPremierExercice = anciensExercicesCommerciaux.get(0).getDateDebut().year();
		final int premierePeriodeFiscaleDeclarationsPersonnesMorales = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		if (entreprise.hasDateDebutPremierExercice() && premiereAnnee - debutPremierExercice > 1 && premiereAnnee >= premierePeriodeFiscaleDeclarationsPersonnesMorales) {
			throw new BouclementException(String.format("Impossible de déplacer la date de bouclement du %s au %s car alors le premier exercice commercial s'étendrait sur plus de deux années civiles.",
			                                            RegDateHelper.dateToDisplayString(ancienneDate),
			                                            RegDateHelper.dateToDisplayString(nouvelleDate)));
		}
		for (int annee = Math.max(premiereAnnee + 1, premierePeriodeFiscaleDeclarationsPersonnesMorales); annee < derniereAnnee; ++annee) {
			if (!anneesBouclements.contains(annee)) {
				throw new BouclementException(String.format("Impossible de déplacer la date de bouclement du %s au %s car alors il n'y aurait plus de bouclement sur l'année civile %d.",
				                                            RegDateHelper.dateToDisplayString(ancienneDate),
				                                            RegDateHelper.dateToDisplayString(nouvelleDate),
				                                            annee));
			}
		}
	}

	/**
	 * Renvoie la première date de bouclement fixée (= veille de la date de début de la première DI valide) postérieure à la date de référence passée en paramètre (qui ne
	 * doit pas être dans une période couverte par une DI
	 * @param dateReference date de référence
	 * @param dis liste triée des DI non-annulées existante (= c'est ce qui fixe les exercices commerciaux...)
	 * @return la première date de bouclement fixée après la date de référence, ou <code>null</code> si une telle date n'existe pas
	 * @throws IllegalStateException si la date de référence est dans une période couverte par une DI
	 */
	@Nullable
	private static RegDate getPremierBouclementFixeApres(@NotNull RegDate dateReference, List<DeclarationImpotOrdinairePM> dis) {
		if (DateRangeHelper.rangeAt(dis, dateReference) != null) {
			throw new IllegalStateException("Erreur dans l'algorithme : la date de référence est située dans une période couverte par une DI non-annulée.");
		}

		for (DeclarationImpotOrdinairePM di : dis) {
			if (dateReference.compareTo(di.getDateDebut()) < 0) {
				return di.getDateDebut().getOneDayBefore();
			}
		}
		return null;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}
}

