package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;

public class UidRegisterDeregistrationReasonConverter extends BaseEnumConverter<UidDeregistrationReason, RaisonDeRadiationRegistreIDE> {
	@Override
	@NotNull
	protected RaisonDeRadiationRegistreIDE convert(@NotNull UidDeregistrationReason value) {
		switch (value) {
		case AUTRE:
			return RaisonDeRadiationRegistreIDE.AUTRE;
		case FUSION_ABSORPTION:
			return RaisonDeRadiationRegistreIDE.FUSION_ABSORPTION;
		case FUSION_COMBINAISON:
			return RaisonDeRadiationRegistreIDE.FUSION_COMBINAISON;
		case CESSATION_SCISSION_RETRAITE_SALARIE:
			return RaisonDeRadiationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE;
		case ABSENCE_AUTORISATION:
			return RaisonDeRadiationRegistreIDE.ABSENCE_AUTORISATION;
		case REMISE_DE_COMMERCE_OU_MODIFICATION_DE_LA_RAISON_DE_COMMERCE:
			return RaisonDeRadiationRegistreIDE.REMISE_DE_COMMERCE_OU_MODIFICATION_DE_LA_RAISON_DE_COMMERCE;
		case FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE:
			return RaisonDeRadiationRegistreIDE.FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE;
		case DECES:
			return RaisonDeRadiationRegistreIDE.DECES;
		case DOUBLON_OU_ERREUR:
			return RaisonDeRadiationRegistreIDE.DOUBLON_OU_ERREUR;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}