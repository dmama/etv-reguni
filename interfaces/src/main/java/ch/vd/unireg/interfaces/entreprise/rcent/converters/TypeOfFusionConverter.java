package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfFusion;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeFusion;

public class TypeOfFusionConverter extends BaseEnumConverter<TypeOfFusion, TypeDeFusion> {

	@Override
	@NotNull
	protected TypeDeFusion convert(@NotNull TypeOfFusion value) {
		switch (value) {
		case FUSION_SOCIETES_COOPERATIVES:
			return TypeDeFusion.FUSION_SOCIETES_COOPERATIVES;
		case FUSION_SOCIETES_ANONYMES:
			return TypeDeFusion.FUSION_SOCIETES_ANONYMES;
		case FUSION_SOCIETES_ANONYMES_ET_COMMANDITE_PAR_ACTIONS:
			return TypeDeFusion.FUSION_SOCIETES_ANONYMES_ET_COMMANDITE_PAR_ACTIONS;
		case AUTRE_FUSION:
			return TypeDeFusion.AUTRE_FUSION;
		case FUSION_INTERNATIONALE:
			return TypeDeFusion.FUSION_INTERNATIONALE;
		case FUSION_ART_25_LFUS:
			return TypeDeFusion.FUSION_ART_25_LFUS;
		case FUSION_INSTITUTIONS_DE_PREVOYANCE:
			return TypeDeFusion.FUSION_INSTITUTIONS_DE_PREVOYANCE;
		case SCISSION_ART_45_LFUS:
			return TypeDeFusion.SCISSION_ART_45_LFUS;
		case FUSION_SUISSE_VERS_ETRANGER:
			return TypeDeFusion.FUSION_SUISSE_VERS_ETRANGER;
		case SCISSION_SUISSE_VERS_ETRANGER:
			return TypeDeFusion.SCISSION_SUISSE_VERS_ETRANGER;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
