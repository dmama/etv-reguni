package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

/**
 * Un adapter permettant de tirer partie des DateRange implémentant les fonctions de duplication [duplicate()] et de rebornage [rerange()]
 * nécessaires à DateRangeHelper.override(), afin d'éviter de fournir une implémentation à chaque appel et de garantir de ce fait
 * un comportement homogène.
 *
 * Exemple d'utilisation: DateRangeHelper.override() pour combiner deux historiques.
 *
 * @author Raphaël Marmier, 2016-01-06, <raphael.marmier@vd.ch>
 */
public class GentilDateRangeExtendedAdapterCallback<T extends DateRange & Duplicable<T> & Rerangeable<T>> implements DateRangeHelper.AdapterCallbackExtended<T> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T duplicate(T range) {
		return range.duplicate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T adapt(T range, RegDate debut, T sourceSurchargeDebut, RegDate fin, T sourceSurchargeFin) {
		final DateRange cropRange = new DateRangeHelper.Range(sourceSurchargeDebut != null ? debut : range.getDateDebut(),
		                                                      sourceSurchargeFin != null ? fin : range.getDateFin());
		return range.rerange(cropRange);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T adapt(T range, RegDate debut, RegDate fin) {
		final DateRange cropRange = new DateRangeHelper.Range(debut, fin);
		return range.rerange(cropRange);
	}
}
