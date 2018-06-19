package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;

public class TypeOfLocationConverter extends BaseEnumConverter<TypeOfLocation, TypeEtablissementCivil> {

	@Override
	@NotNull
	protected TypeEtablissementCivil convert(@NotNull TypeOfLocation value) {
		switch (value) {
		case ETABLISSEMENT_PRINCIPAL:
			return TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL;
		case ETABLISSEMENT_SECONDAIRE:
			return TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
