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
import java.util.TreeMap;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.MovingWindow;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.type.DayMonth;

/**
 * Implémentation du service de fourniture d'information autour des bouclements et exercices comptables
 */
public class BouclementServiceImpl implements BouclementService {

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
}

