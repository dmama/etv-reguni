package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class TypeDeSiteConverter extends BaseEnumConverter<TypeDeSite, TypeOfLocation> {

	@Override
	@NotNull
	public TypeOfLocation convert(@NotNull TypeDeSite value) {
		switch (value) {
		case ETABLISSEMENT_PRINCIPAL:
			return TypeOfLocation.ETABLISSEMENT_PRINCIPAL;
		case ETABLISSEMENT_SECONDAIRE:
			return TypeOfLocation.ETABLISSEMENT_SECONDAIRE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
