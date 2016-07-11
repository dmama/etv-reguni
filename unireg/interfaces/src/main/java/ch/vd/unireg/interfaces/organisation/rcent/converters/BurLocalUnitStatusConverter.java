package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusREE;

public class BurLocalUnitStatusConverter extends BaseEnumConverter<BurLocalUnitStatus, StatusREE> {

	@Override
	@NotNull
	protected StatusREE convert(@NotNull BurLocalUnitStatus value) {
		switch (value) {
		case ACTIF:
			return StatusREE.ACTIF;
		case INACTIF:
			return StatusREE.INACTIF;
		case RADIE:
			return StatusREE.RADIE;
		case INCONNU:
			return StatusREE.INCONNU;
		case TRANSFERE:
			return StatusREE.TRANSFERE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
