package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class TypeOfLocationConverter extends BaseEnumConverter<TypeOfLocation, TypeDeSite> {

	@Override
	@NotNull
	protected TypeDeSite convert(@NotNull TypeOfLocation value) {
		switch (value) {
		case ETABLISSEMENT_PRINCIPAL:
			return TypeDeSite.ETABLISSEMENT_PRINCIPAL;
		case ETABLISSEMENT_SECONDAIRE:
			return TypeDeSite.ETABLISSEMENT_SECONDAIRE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
