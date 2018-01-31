package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.RCRegistrationData;

public class CommercialRegisterRegistrationDataConverter extends BaseConverter<RCRegistrationData, InscriptionRC> {

	@NotNull
	@Override
	protected InscriptionRC convert(@NotNull RCRegistrationData rcRegistrationData) {
		return new InscriptionRC(mapStatus(rcRegistrationData.getRegistrationStatus()),
		                         mapRaisonDissolution(rcRegistrationData.getVdDissolutionReason()),
		                         rcRegistrationData.getVdRegistrationDate(),
		                         rcRegistrationData.getVdDeregistrationDate(),
		                         rcRegistrationData.getChRegistrationDate(),
		                         rcRegistrationData.getChDeregistrationDate());
	}

	@Nullable
	static StatusInscriptionRC mapStatus(CommercialRegisterStatus status) {
		if (status == null) {
			return null;
		}

		switch (status) {
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
			throw new IllegalArgumentException(BaseEnumConverter.genericUnsupportedValueMessage(status));
		}
	}

	@Nullable
	static RaisonDeDissolutionRC mapRaisonDissolution(DissolutionReason reason) {
		if (reason == null) {
			return null;
		}

		switch (reason) {
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
			throw new IllegalArgumentException(BaseEnumConverter.genericUnsupportedValueMessage(reason));
		}
	}
}
