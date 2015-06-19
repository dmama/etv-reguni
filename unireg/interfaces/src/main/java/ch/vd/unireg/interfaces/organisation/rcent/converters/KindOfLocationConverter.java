package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class KindOfLocationConverter extends BaseEnumConverter<KindOfLocation, TypeDeSite> {

	@Override
	@NotNull
	protected TypeDeSite convert(@NotNull KindOfLocation value) {
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
