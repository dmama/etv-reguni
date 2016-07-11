package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;

public class UidRegisterStatusConverter extends BaseEnumConverter<UidRegisterStatus, StatusRegistreIDE> {

	@Override
	@NotNull
	protected StatusRegistreIDE convert(@NotNull UidRegisterStatus value) {
		switch (value) {
		case AUTRE:
			return StatusRegistreIDE.AUTRE;
		case PROVISOIRE:
			return StatusRegistreIDE.PROVISOIRE;
		case EN_REACTIVATION:
			return StatusRegistreIDE.EN_REACTIVATION;
		case DEFINITIF:
			return StatusRegistreIDE.DEFINITIF;
		case EN_MUTATION:
			return StatusRegistreIDE.EN_MUTATION;
		case RADIE:
			return StatusRegistreIDE.RADIE;
		case DEFINITIVEMENT_RADIE:
			return StatusRegistreIDE.DEFINITIVEMENT_RADIE;
		case ANNULE:
			return StatusRegistreIDE.ANNULE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
