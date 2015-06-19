package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Capital;

public class CapitalConverter extends BaseConverter<ch.vd.evd0022.v1.Capital, Capital> {

	private static final TypeOfCapitalConverter TYPE_OF_CAPITAL_CONVERTER = new TypeOfCapitalConverter();

	@Override
	@NotNull
	protected Capital convert(@NotNull ch.vd.evd0022.v1.Capital capital) {
		return new Capital(
				TYPE_OF_CAPITAL_CONVERTER.apply(capital.getTypeOfCapital()),
				capital.getCurrency(),
				capital.getCapitalAmount(),
				capital.getCashedInAmount(),
				capital.getDivision()
		);
	}
}
