package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;

public class UidRegisterLiquidationReasonConverter extends BaseEnumConverter<UidRegisterLiquidationReason, RaisonLiquidationRegistreIDE> {
	@Override
	protected RaisonLiquidationRegistreIDE convert(@NotNull UidRegisterLiquidationReason value) {
		switch (value) {
		case AUTRE:
			return RaisonLiquidationRegistreIDE.AUTRE;
		case FUSION_ABSORPTION:
			return RaisonLiquidationRegistreIDE.FUSION_ABSORPTION;
		case FUSION_COMBINAISON:
			return RaisonLiquidationRegistreIDE.FUSION_COMBINAISON;
		case CESSATION_SCISSION_RETRAITE_SALARIE:
			return RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE;
		case ABSENCE_AUTORISATION:
			return RaisonLiquidationRegistreIDE.ABSENCE_AUTORISATION;
		case REMISE_OU_MODIFICATION_DE_LA_RAISON:
			return RaisonLiquidationRegistreIDE.REMISE_OU_MODIFICATION_DE_LA_RAISON;
		case FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE:
			return RaisonLiquidationRegistreIDE.FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE;
		case DECES:
			return RaisonLiquidationRegistreIDE.DECES;
		case DOUBLON_OU_ERREUR:
			return RaisonLiquidationRegistreIDE.DOUBLON_OU_ERREUR;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value.name(), value.getClass().getSimpleName()));
		}
	}
}