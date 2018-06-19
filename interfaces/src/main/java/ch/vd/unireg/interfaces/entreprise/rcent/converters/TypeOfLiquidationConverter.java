package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfLiquidation;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeLiquidation;

public class TypeOfLiquidationConverter extends BaseEnumConverter<TypeOfLiquidation, TypeDeLiquidation> {

	@Override
	@NotNull
	protected TypeDeLiquidation convert(@NotNull TypeOfLiquidation value) {
		switch (value) {
		case SOCIETE_ANONYME:
			return TypeDeLiquidation.SOCIETE_ANONYME;
		case SOCIETE_RESPONSABILITE_LIMITE:
			return TypeDeLiquidation.SOCIETE_RESPONSABILITE_LIMITE;
		case SOCIETE_COOPERATIVE:
			return TypeDeLiquidation.SOCIETE_COOPERATIVE;
		case ASSOCIATION:
			return TypeDeLiquidation.ASSOCIATION;
		case FONDATION:
			return TypeDeLiquidation.FONDATION;
		case SOCIETE_NOM_COLLECTIF:
			return TypeDeLiquidation.SOCIETE_NOM_COLLECTIF;
		case SOCIETE_COMMANDITE:
			return TypeDeLiquidation.SOCIETE_COMMANDITE;
		case SOCIETE_COMMANDITE_PAR_ACTION:
			return TypeDeLiquidation.SOCIETE_COMMANDITE_PAR_ACTION;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
