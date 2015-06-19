package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;

public class CommercialRegisterStatusConverter extends BaseEnumConverter<CommercialRegisterStatus, StatusRC> {

	@Override
	protected StatusRC convert(@NotNull CommercialRegisterStatus value) {
		switch (value) {
		case INCONNU:
			return StatusRC.INCONNU;
		case INSCRIT:
			return StatusRC.INSCRIT;
		case NON_INSCRIT:
			return StatusRC.NON_INSCRIT;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
