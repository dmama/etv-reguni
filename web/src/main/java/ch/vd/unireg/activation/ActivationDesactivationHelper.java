package ch.vd.unireg.activation;

import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.NatureTiers;

/**
 * Quelques méthodes utilitaires autour de l'activation et la désactivation de tiers
 */
public abstract class ActivationDesactivationHelper {

	/**
	 * @param nature la nature du tiers que l'on veut modifier
	 * @param securityProvider le service d'accès aux droits
	 * @return <code>true</code> si la modification est autorisée, <code>false</code> sinon
	 */
	public static boolean isActivationDesactivationAllowed(NatureTiers nature, SecurityProviderInterface securityProvider) {
		if (nature == NatureTiers.Etablissement || nature == NatureTiers.Entreprise) {
			return SecurityHelper.isGranted(securityProvider, Role.MODIF_PM);
		}
		else {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}
	}
}
