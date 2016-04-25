package ch.vd.uniregctb.tiers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;

/**
 * @author Raphaël Marmier, 2016-04-22, <raphael.marmier@vd.ch>
 */
public class SurchargeDonneesCivilesHelper {

	public static <T extends HibernateDateRangeEntity & Duplicable<T>> Set<T> tronconneSurchargeFiscale(DateRange range, RegDate dateValeur, List<T> entites, String descriptionType) throws TiersException {
		if (!entites.isEmpty()) {
			/*
			   Condition pour fonctionner: à la date, il n'y a pas d'override. On s'attend à en avoir jusqu'à la veille pour
			   les entreprises déjà connues d'Unireg.
			 */
			final T domicileFiscal = DateRangeHelper.rangeAt(entites, dateValeur);
			if (domicileFiscal != null) {
				throw new TiersException(
						String.format("Impossible d'appliquer les données civiles car une surcharge fiscale de %s présente en date du %s, après la plage demandée.",
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

	@NotNull
	private static <T extends HibernateDateRangeEntity & Duplicable<T>> Set<T> creeUnEspace(DateRange range, List<T> entites) {
		final Set<T> aSauver = new HashSet<>();
		final List<DateRange> intersectionsDomiciles = DateRangeHelper.intersections(range, entites);
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
