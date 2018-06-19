package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionREE;
import ch.vd.unireg.interfaces.entreprise.data.StatusREE;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.BurRegistrationData;

public class BurRegistrationDataConverter extends BaseConverter<BurRegistrationData, InscriptionREE> {

	@NotNull
	@Override
	protected InscriptionREE convert(@NotNull BurRegistrationData burRegistrationData) {
		return new InscriptionREE(mapStatus(burRegistrationData.getStatus()),
		                          burRegistrationData.getRegistrationDate());
	}

	static StatusREE mapStatus(BurLocalUnitStatus status) {
		if (status == null) {
			return null;
		}

		switch (status) {
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
			throw new IllegalArgumentException(BaseEnumConverter.genericUnsupportedValueMessage(status));
		}
	}
}
