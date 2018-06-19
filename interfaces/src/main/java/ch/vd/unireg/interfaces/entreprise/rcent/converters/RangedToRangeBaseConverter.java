package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;

public abstract class RangedToRangeBaseConverter<S, D extends DateRange> extends BaseConverter<DateRangeHelper.Ranged<S>, D> {

	/**
	 * @param range un élément associé à des dates de validité (ni l'élément ni sa {@link DateRangeHelper.Ranged#getPayload() payload} ne sont <code>null</code>)
	 * @return la structure de données complèẗe
	 */
	@NotNull
	protected abstract D convert(@NotNull DateRangeHelper.Ranged<S> range);
}
