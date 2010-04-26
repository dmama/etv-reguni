package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;

public abstract class ControllerUtils {

	private static final Logger LOGGER = Logger.getLogger(ControllerUtils.class);

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture et écriture sur le <b>dossier</b> du tiers spécifié.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits IFOSec. Un ou plusieurs appels à la méthode {@link ch.vd.uniregctb.security.SecurityProvider#isGranted(ch.vd.uniregctb.security.Role)}
	 * sont nécessaires en complément.
	 *
	 * @param tiersId le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ObjectNotFoundException si le tiers spécifié n'existe pas
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *                                 si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	public static void checkAccesDossierEnLecture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		if (tiersId != null) {
			Niveau acces = SecurityProvider.getDroitAcces(tiersId);
			if (acces == null) {
				final String message = String.format("L'opérateur [%s] s'est vu refusé l'accès en lecture sur le tiers n°%d",
						AuthenticationHelper.getCurrentPrincipal(), tiersId);
				LOGGER.warn(message);
				throw new AccessDeniedException("Vous ne possédez pas les droits de visualisation sur ce contribuable");
			}
		}
	}

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture et écriture sur le <b>dossier</b> du tiers spécifié.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits IFOSec. Un ou plusieurs appels à la méthode {@link SecurityProvider#isGranted(ch.vd.uniregctb.security.Role)} sont nécessaires en
	 * complément.
	 *
	 * @param tiersId le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ObjectNotFoundException si le tiers spécifié n'existe pas
	 * @throws AccessDeniedException   si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	public static void checkAccesDossierEnEcriture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		if (tiersId != null) {
			Niveau acces = SecurityProvider.getDroitAcces(tiersId);
			if (acces == null || acces.equals(Niveau.LECTURE)) {
				final String message = String.format(
						"L'opérateur [%s] s'est vu refusé l'accès en écriture sur le tiers n°%d (acces autorisé=%s)", AuthenticationHelper
								.getCurrentPrincipal(), tiersId, (acces == null ? "null" : acces.toString()));
				LOGGER.warn(message);
				throw new AccessDeniedException("Vous ne possédez pas les droits d'édition sur ce contribuable");
			}
		}
	}

}
