package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;

public class CommercialRegisterStatusConverter extends BaseEnumConverter<CommercialRegisterStatus, StatusInscriptionRC> {

	@Override
	@NotNull
	protected StatusInscriptionRC convert(@NotNull CommercialRegisterStatus value) {
		switch (value) {
		case INCONNU:
			return StatusInscriptionRC.INCONNU;
		case NON_INSCRIT:
			return StatusInscriptionRC.NON_INSCRIT;
		case ACTIF:
			return StatusInscriptionRC.ACTIF;
		case EN_LIQUIDATION:
			return StatusInscriptionRC.EN_LIQUIDATION;
		case RADIE:
			return StatusInscriptionRC.RADIE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
