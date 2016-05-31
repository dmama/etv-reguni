package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfCapital;
import ch.vd.unireg.interfaces.organisation.data.TypeDeCapital;

public class TypeOfCapitalConverter extends BaseEnumConverter<TypeOfCapital, TypeDeCapital> {

	@Override
	@NotNull
	protected TypeDeCapital convert(@NotNull TypeOfCapital value) {
		switch (value) {
		case CAPITAL_SOCIAL:
			return TypeDeCapital.CAPITAL_SOCIAL;
		case CAPITAL_ACTIONS:
			return TypeDeCapital.CAPITAL_ACTIONS;
		case CAPITAL_PARTICIPATION:
			return TypeDeCapital.CAPITAL_PARTICIPATION;
		case MONTANT_TOTAL_COMMANDITES:
			return TypeDeCapital.MONTANT_TOTAL_COMMANDITES;
		case CAPITAL_VARIABLE:
			return TypeDeCapital.CAPITAL_VARIABLE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
