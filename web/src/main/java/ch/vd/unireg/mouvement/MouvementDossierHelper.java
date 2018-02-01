package ch.vd.unireg.mouvement;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;

/**
 * Quelques méthodes bien pratiques quand on travaille avec les mouvements de dossier
 */
public abstract class MouvementDossierHelper {

	/**
	 * @return le numéro de collectivité administrative courant (celle avec laquelle l'utilisateur s'est autentifié dans l'application), ou <code>null</code> si c'est l'ACI
	 */
	public static Integer getNoCollAdmFiltree() {
		final Integer oid = AuthenticationHelper.getCurrentOID();
		if (oid != null && oid != ServiceInfrastructureService.noACI) {
			return oid;
		}
		else {
			return null;
		}
	}

	/**
	 * Renvoie une exception si le rôle {@link ch.vd.unireg.security.Role#MVT_DOSSIER_MASSE} n'est pas associé au principal
	 * @throws AccessDeniedException en cas d'accès interdit
	 * @param securityProvider le security provider
	 */
	public static void checkAccess(SecurityProviderInterface securityProvider) throws AccessDeniedException {
		if (!SecurityHelper.isGranted(securityProvider, Role.MVT_DOSSIER_MASSE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits de gestion des mouvements de dossiers en masse pour l'application Unireg.");
		}
	}
}
