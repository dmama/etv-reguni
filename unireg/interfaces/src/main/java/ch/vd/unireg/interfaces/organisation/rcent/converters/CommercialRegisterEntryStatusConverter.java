package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;

public class CommercialRegisterEntryStatusConverter extends BaseEnumConverter<CommercialRegisterEntryStatus, StatusInscriptionRC> {

	@Override
	@NotNull
	protected StatusInscriptionRC convert(@NotNull CommercialRegisterEntryStatus value) {
		switch (value) {
		case AUTRE:
			return StatusInscriptionRC.AUTRE;
		case ACTIF:
			return StatusInscriptionRC.ACTIF;
		case EN_LIQUIDATION:
			return StatusInscriptionRC.EN_LIQUIDATION;
		case RADIE:
			return StatusInscriptionRC.RADIE;
		case PROVISOIRE:
			return StatusInscriptionRC.PROVISOIRE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
