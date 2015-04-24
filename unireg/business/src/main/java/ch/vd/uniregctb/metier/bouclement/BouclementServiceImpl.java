package ch.vd.uniregctb.metier.bouclement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.type.DayMonth;

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
			candidate = candidate.addMonths(periodeMois);

			// apparemment, dans les RegDate, 28.02.2015 + 12M -> 28.2.2016 même si 2016 est bissextile
			if (ancrageOnEndOfMonth) {
				candidate = candidate.getLastDayOfTheMonth();
			}
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
		final RegDate premiereDate = active.getValue().getAncrage().nextAfter(active.getKey());
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
	public List<ExerciceCommercial> getExercicesCommerciaux(Collection<Bouclement> bouclements, @NotNull DateRange range) {
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
			liste.add(new ExerciceCommercial(debut, fin));
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

	@Override
	public List<Bouclement> extractBouclementsDepuisDates(Collection<RegDate> datesBouclements, int periodeMoisFinale) {

		// blindage simple contre les collections vides/nulles en entrée
		final NavigableSet<RegDate> sortedDates = sortAndRemoveNull(datesBouclements);
		if (sortedDates.isEmpty()) {
			return Collections.emptyList();
		}

		// la liste finale
		final List<Bouclement> liste = new LinkedList<>();

		// une sorte de buffer temporaire pour construire la liste finale
		Bouclement tmp = null;

		// on boucle sur les dates, en ayant toujours en tête la suivante
		final MovingWindow<RegDate> wnd = new MovingWindow<>(new ArrayList<>(sortedDates));
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<RegDate> snap = wnd.next();
			final RegDate dateCourante = snap.getCurrent();
			final RegDate dateSuivante = snap.getNext();

			final DayMonth ancrageCourant = DayMonth.get(dateCourante);
			final DayMonth nouvelAncrage = dateSuivante == null ? ancrageCourant : DayMonth.get(dateSuivante);

			final boolean ancragesMemeFinDeMois = ancrageCourant.isEndOfMonth() && nouvelAncrage.isEndOfMonth() && ancrageCourant.month() == nouvelAncrage.month();
			final boolean ancragesMemeJourDuMois = DayMonth.isSameDayOfMonth(ancrageCourant, nouvelAncrage);

			final int periodeMois;
			if (ancrageCourant == nouvelAncrage || ancragesMemeFinDeMois) {
				// même ancrage, il faut juste vérifier la différence en années (et donc en mois) entre les deux dates
				periodeMois = dateSuivante == null ? periodeMoisFinale : 12 * (dateSuivante.year() - dateCourante.year());
			}
			else if (ancragesMemeJourDuMois) {
				// même jour dans le mois, il y a donc un nombre de mois entier entre les deux dates
				periodeMois = 12 * (dateSuivante.year() - dateCourante.year()) + dateSuivante.month() - dateCourante.month();
			}
			else {
				// on construit une période qui passe de manière certaine au dessus du prochain seuil
				// TODO doit-on arrondir en années entières ?
				final int pm =  12 * (dateSuivante.year() - dateCourante.year()) + dateSuivante.month() - dateCourante.month();
				periodeMois = dateCourante.addMonths(pm).isBefore(dateSuivante) ? pm + 1 : pm;
			}

			final boolean mustChange = tmp != null && (tmp.getPeriodeMois() != periodeMois || !DayMonth.isSameDayOfMonth(tmp.getAncrage(), ancrageCourant));
			if (tmp == null || mustChange) {
				tmp = new Bouclement();
				tmp.setDateDebut(dateCourante);
				tmp.setAncrage(ancrageCourant);
				tmp.setPeriodeMois(periodeMois);
				liste.add(tmp);
			}
		}

		// repassage en ArrayList une fois la taille connue
		return new ArrayList<>(liste);
	}
}

