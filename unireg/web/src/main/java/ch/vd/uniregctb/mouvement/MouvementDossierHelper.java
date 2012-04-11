package ch.vd.uniregctb.mouvement;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

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
	 * Renvoie une exception si le rôle {@link ch.vd.uniregctb.security.Role#MVT_DOSSIER_MASSE} n'est pas associé au principal
	 * @throws AccessDeniedException en cas d'accès interdit
	 */
	public static void checkAccess() throws AccessDeniedException {
		if (!SecurityProvider.isGranted(Role.MVT_DOSSIER_MASSE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits de gestion des mouvements de dossiers en masse pour l'application Unireg.");
		}
	}
}
