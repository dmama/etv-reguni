package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfCapitalReduction;
import ch.vd.unireg.interfaces.organisation.data.TypeDeReductionDuCapital;

public class TypeOfCapitalReductionConverter extends BaseEnumConverter<TypeOfCapitalReduction, TypeDeReductionDuCapital> {

	@Override
	@NotNull
	protected TypeDeReductionDuCapital convert(@NotNull TypeOfCapitalReduction value) {
		switch (value) {
		case CAPITAL_ACTIONS_SOCIETE_ANONYME:
			return TypeDeReductionDuCapital.CAPITAL_ACTIONS_SOCIETE_ANONYME;
		case CAPITAL_PARTICIPATION:
			return TypeDeReductionDuCapital.CAPITAL_PARTICIPATION;
		case CAPITAL_SOCIAL:
			return TypeDeReductionDuCapital.CAPITAL_SOCIAL;
		case VALEUR_NOMINALE_TITRES:
			return TypeDeReductionDuCapital.VALEUR_NOMINALE_TITRES;
		case SUPPRESSION_TITRES:
			return TypeDeReductionDuCapital.SUPPRESSION_TITRES;
		case CAPITAL_ACTION_SOCIETE_COMMANDITE_PAR_ACTION:
			return TypeDeReductionDuCapital.CAPITAL_ACTION_SOCIETE_COMMANDITE_PAR_ACTION;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
