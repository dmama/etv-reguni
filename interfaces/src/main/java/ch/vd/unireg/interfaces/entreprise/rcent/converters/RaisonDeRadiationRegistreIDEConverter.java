package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.unireg.interfaces.entreprise.data.RaisonDeRadiationRegistreIDE;

public class RaisonDeRadiationRegistreIDEConverter extends BaseEnumConverter<RaisonDeRadiationRegistreIDE, UidDeregistrationReason> {
	@Override
	@NotNull
	public UidDeregistrationReason convert(@NotNull RaisonDeRadiationRegistreIDE value) {
		switch (value) {
		case AUTRE:
			return UidDeregistrationReason.AUTRE;
		case FUSION_ABSORPTION:
			return UidDeregistrationReason.FUSION_ABSORPTION;
		case FUSION_COMBINAISON:
			return UidDeregistrationReason.FUSION_COMBINAISON;
		case CESSATION_SCISSION_RETRAITE_SALARIE:
			return UidDeregistrationReason.CESSATION_SCISSION_RETRAITE_SALARIE;
		case ABSENCE_AUTORISATION:
			return UidDeregistrationReason.ABSENCE_AUTORISATION;
		case REMISE_DE_COMMERCE_OU_MODIFICATION_DE_LA_RAISON_DE_COMMERCE:
			return UidDeregistrationReason.REMISE_DE_COMMERCE_OU_MODIFICATION_DE_LA_RAISON_DE_COMMERCE;
		case FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE:
			return UidDeregistrationReason.FIN_CONSORTIUM_PROJET_EVT_OU_ACTIVITE_EN_SUISSE;
		case DECES:
			return UidDeregistrationReason.DECES;
		case DOUBLON_OU_ERREUR:
			return UidDeregistrationReason.DOUBLON_OU_ERREUR;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}