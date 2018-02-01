package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.infra.data.Pays;

/**
 * Classe utilitaire de méthodes pratiques autour de la gestion des nationalités
 */
public abstract class NationaliteHelper {

	/**
	 * Extraction, d'une liste de nationalités historiques, celle(s) qui est/sont valid(e) à la date donnée
	 * @param nationalites toutes les nationalités connues d'un individu
	 * @param date date de référence
	 * @return une liste (potentiellement vide) des nationalités valides à la date demandée
	 */
	@NotNull
	public static List<Nationalite> validAt(Collection<Nationalite> nationalites, final RegDate date) {
		return filter(nationalites, nationalite -> nationalite.isValidAt(date) ? FilteringResult.TAKE_IT_AND_CONTINUE : FilteringResult.DONT_TAKE_IT);
	}

	/**
	 * Identification de la nationalité de référence d'un individu à une date donnée
	 * @param nationalites toutes les nationalités historiques connues d'un individu
	 * @param date date de référence
	 * @return la nationalité (peut-être nulle) à considérer fiscalement pour un individu à la date demandée (Suisse prépondérant, sinon, l'une des nationalités étrangères trouvées)
	 */
	@Nullable
	public static Nationalite refAt(Collection<Nationalite> nationalites, RegDate date) {
		final List<Nationalite> allAt = validAt(nationalites, date);
		if (allAt.size() == 0) {
			return null;
		}
		else if (allAt.size() == 1) {
			return allAt.get(0);
		}
		else {
			// recherche d'une nationalité suisse
			for (Nationalite candidate : allAt) {
				final Pays pays = candidate.getPays();
				if (pays != null && pays.isSuisse()) {
					return candidate;
				}
			}

			// aucune nationalité suisse... on en renvoie une un peu au hasard...
			return allAt.get(0);
		}
	}

	/**
	 * @param nationalites ensemble des nationalités historiques connues d'un individu
	 * @param date date de référence
	 * @return <code>true</code> si l'individu peut être considéré comme Suisse à la date demandée compte tenu des nationalités présentées, <code>false</code> sinon
	 */
	public static boolean isSuisseAt(Collection<Nationalite> nationalites, RegDate date) {
		final Nationalite ref = refAt(nationalites, date);
		final Pays pays = ref == null ? null : ref.getPays();
		return pays != null && pays.isSuisse();
	}

	/**
	 * @param nationalites ensemble des nationalités historiques connues d'un individu
	 * @param date date de référence
	 * @return la liste des nationalités dont la date de début correspond à la date demandée
	 */
	public static List<Nationalite> startingAt(Collection<Nationalite> nationalites, final RegDate date) {
		return filter(nationalites, nationalite -> nationalite.getDateDebut() == date ? FilteringResult.TAKE_IT_AND_CONTINUE : FilteringResult.DONT_TAKE_IT);
	}

	/**
	 * @param nationalites ensemble des nationalités historiques connues d'un individu
	 * @param date date de référence
	 * @return la liste des nationalités dont la date de fin correspond à la date demandée
	 */
	public static List<Nationalite> endingAt(Collection<Nationalite> nationalites, final RegDate date) {
		return filter(nationalites, nationalite -> nationalite.getDateFin() == date ? FilteringResult.TAKE_IT_AND_CONTINUE : FilteringResult.DONT_TAKE_IT);
	}

	private enum FilteringResult {
		TAKE_IT_AND_STOP(true, true),
		TAKE_IT_AND_CONTINUE(true, false),
		DONT_TAKE_IT(false, false);

		private final boolean takeIt;
		private final boolean stop;

		FilteringResult(boolean takeIt, boolean stop) {
			this.takeIt = takeIt;
			this.stop = stop;
		}
	}

	private interface Filter {
		@NotNull
		FilteringResult filter(Nationalite nationalite);
	}

	@NotNull
	private static List<Nationalite> filter(Collection<Nationalite> src, Filter filter) {
		if (src == null || src.size() == 0) {
			return Collections.emptyList();
		}

		final List<Nationalite> res = new ArrayList<>(src.size());
		for (Nationalite candidate : src) {
			if (candidate != null) {
				final FilteringResult filteringResult = filter.filter(candidate);
				if (filteringResult.takeIt) {
					res.add(candidate);
				}
				if (filteringResult.stop) {
					break;
				}
			}
		}

		return res;
	}
}
