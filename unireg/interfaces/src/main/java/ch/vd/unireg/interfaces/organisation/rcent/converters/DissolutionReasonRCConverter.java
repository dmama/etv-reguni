package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;

public class DissolutionReasonRCConverter extends BaseEnumConverter<DissolutionReason, RaisonDeDissolutionRC> {

	@Override
	@NotNull
	protected RaisonDeDissolutionRC convert(@NotNull DissolutionReason value) {
		switch (value) {
		case FUSION:
			return RaisonDeDissolutionRC.FUSION;
		case LIQUIDATION:
			return RaisonDeDissolutionRC.LIQUIDATION;
		case FAILLITE:
			return RaisonDeDissolutionRC.FAILLITE;
		case TRANSFORMATION:
			return RaisonDeDissolutionRC.TRANSFORMATION;
		case CARENCE_DANS_ORGANISATION:
			return RaisonDeDissolutionRC.CARENCE_DANS_ORGANISATION;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
