package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.Capital;

public class CapitalConverter extends RangedToRangeBaseConverter<ch.vd.evd0022.v3.Capital, Capital> {

	private static final TypeOfCapitalConverter TYPE_OF_CAPITAL_CONVERTER = new TypeOfCapitalConverter();

	@NotNull
	@Override
	protected Capital convert(@NotNull DateRangeHelper.Ranged<ch.vd.evd0022.v3.Capital> range) {
		final ch.vd.evd0022.v3.Capital capital = range.getPayload();
		return new Capital(range.getDateDebut(),
		                   range.getDateFin(),
		                   TYPE_OF_CAPITAL_CONVERTER.apply(capital.getTypeOfCapital()),
		                   capital.getCurrency(),
		                   capital.getCashedInAmount(),   // c'est le capital libéré qui passe dans Unireg...
		                   capital.getDivision());
	}
}
