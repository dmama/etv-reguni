package ch.vd.unireg.interfaces;

import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.unireg.interfaces.efacture.data.TypeDemande;

public abstract class EvdHelper {

	public static TypeDemande getTypeDemandeFromEvd25(RegistrationMode mode) {
		final TypeDemande type;
		if (mode == null) {
			type = null;
		}
		else {
			switch (mode) {
				case DIRECT:
				case STANDARD:
					type = TypeDemande.INSCRIPTION;
					break;
				case UNREGISTER:
					type = TypeDemande.DESINSCRIPTION;
					break;
				default:
				    throw new IllegalArgumentException("Invalid mode : " + mode);
			}
		}
		return type;
	}
}
