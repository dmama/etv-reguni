package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.Sex;
import ch.vd.uniregctb.type.Sexe;

public class SexConverter extends BaseEnumConverter<Sex, Sexe> {

	@Override
	protected Sexe convert(@NotNull Sex value) {
		switch (value) {
		case FEMININ:
			return Sexe.FEMININ;
		case MASCULIN:
			return Sexe.MASCULIN;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}

}
