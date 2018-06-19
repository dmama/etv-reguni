package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;

public class TypeEtablissementCivilConverter extends BaseEnumConverter<TypeEtablissementCivil, TypeOfLocation> {

	@Override
	@NotNull
	public TypeOfLocation convert(@NotNull TypeEtablissementCivil value) {
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
