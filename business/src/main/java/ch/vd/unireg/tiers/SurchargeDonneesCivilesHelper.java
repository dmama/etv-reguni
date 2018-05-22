package ch.vd.unireg.tiers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;

/**
 * @author Raphaël Marmier, 2016-04-22, <raphael.marmier@vd.ch>
 */
public class SurchargeDonneesCivilesHelper {

	/**
	 * Tronque l'historique d'entités à la veille de la date de début du range. S'assure qu'il n'existe pas de données fiscales
	 * en base ni à la date de valeur ni au lendemain de la date de fin du range.
	 *
	 * Les périodes incluses dans le range sont annulées et la période à cheval sur la date de début est tronquée à la veille.
	 *
	 * @return La liste des périodes à sauver (une seule en réalité avec cette méthode)
	 */
	public static <T extends HibernateDateRangeEntity & Duplicable<T>> Set<T> tronqueSurchargeFiscale(DateRange range, RegDate dateValeur, List<T> entites, String descriptionType) throws TiersException {
		if (!entites.isEmpty()) {
			if (range.isValidAt(dateValeur)) {
				throw new IllegalArgumentException();
			}
			/*
			   Condition pour fonctionner: à la date de valeur, il n'y a pas d'override. On s'attend à en avoir jusqu'à la veille pour
			   les entreprises déjà connues d'Unireg.
			 */
			final T domicileFiscalDateValeur = DateRangeHelper.rangeAt(entites, dateValeur);
			final T domicileFiscalApres = DateRangeHelper.rangeAt(entites, range.getDateFin().getOneDayAfter());
			if (domicileFiscalDateValeur != null || domicileFiscalApres != null) {
				throw new TiersException(
						String.format("Impossible d'appliquer les données civiles car une surcharge fiscale de %s est présente en date du %s.",
						              descriptionType,
						              RegDateHelper.dateToDisplayString(dateValeur)));
			}
			/*
				Supprimer toute surcharge dans la période demandée, en prenant soin de fermer celle qui chevauche.
			 */
			return creeUnEspace(range, entites);
		}
		return Collections.emptySet();
	}

	/**
	 * Crée un espace  dans un historique de périodes. Les périodes à cheval sont recréées avant et après et les périodes incluses
	 * sont annulées.
	 */
	@NotNull
	private static <T extends HibernateDateRangeEntity & Duplicable<T>> Set<T> creeUnEspace(DateRange range, List<T> entites) {
		final Set<T> aSauver = new HashSet<>();
		final List<DateRange> intersectionsDomiciles = DateRangeHelper.intersections(range, entites);
		if (intersectionsDomiciles == null) {
			return Collections.emptySet();
		}
		for (DateRange intersection : intersectionsDomiciles) {
		/* Récupérer l'original */
			final T dom = DateRangeHelper.rangeAt(entites, intersection.getDateDebut());
			/* Le fermer à la veille s'il est à cheval sur le début de la période. */
			if (dom.isValidAt(range.getDateDebut().getOneDayBefore())) {
				final T clone = dom.duplicate();
				clone.setDateFin(range.getDateDebut().getOneDayBefore());
				aSauver.add(clone);
			}
			/* Le fermer au lendemain s'il est à cheval sur la fin de la période. */
			if (dom.isValidAt(range.getDateFin().getOneDayAfter())) {
				final T clone = dom.duplicate();
				clone.setDateDebut(range.getDateFin().getOneDayAfter());
				aSauver.add(clone);
			}
			/* L'annuler purement et simplement s'il se trouve dedans. */
			dom.setAnnule(true);
		}
		return aSauver;
	}

}
