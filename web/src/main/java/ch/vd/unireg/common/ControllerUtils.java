package ch.vd.unireg.common;

import javax.servlet.http.HttpServletRequest;

import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.security.AccessDeniedException;

/**
 * Collection de méthodes utilitaires <b>générales</b> en relation avec les contrôleurs Spring MVC 3.
 */
public interface ControllerUtils {

	String PARAMETER_MODIFIER = "__MODIFIER__";

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture sur le <b>dossier</b> du tiers spécifié.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits. Un ou plusieurs appels à la méthode {@link ch.vd.unireg.security.SecurityHelper#isGranted(ch.vd.unireg.security.Role)}
	 * sont nécessaires en complément.
	 *
	 * @param tiersId le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ch.vd.unireg.common.ObjectNotFoundException si le tiers spécifié n'existe pas
	 * @throws ch.vd.unireg.security.AccessDeniedException
	 *                                 si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	void checkAccesDossierEnLecture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException;

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture et écriture sur le <b>dossier</b> du tiers spécifié.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits. Un ou plusieurs appels à la méthode {@link ch.vd.unireg.security.SecurityHelper#isGranted(ch.vd.unireg.security.Role)} sont nécessaires en
	 * complément.
	 *
	 * @param tiersId le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ch.vd.unireg.common.ObjectNotFoundException si le tiers spécifié n'existe pas
	 * @throws ch.vd.unireg.security.AccessDeniedException   si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	void checkAccesDossierEnEcriture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException;

	/**
	 * A partir d'une requete, cette méthode construit une String
	 * contenant les paramètres de requetes nécessaire au DisplayTag
	 * pour répercuter ces informations de navigation (no page, champs de tri, ordre de tri)
	 *
	 * @param request request avec laquelle la table a déjà été affichée
	 * @param tableName le nom de la table
	 * @return une chaîne de caractères (<code>null</code> si vide) dans laquelle ont été extraits les paramètres de pagination pour la table donnée, depuis la requête donnée
	 */
	String getDisplayTagRequestParametersForPagination(HttpServletRequest request, String tableName);

	/**
	 * A partir d'un objet ParamPagination, cette méthode construit une String
	 * contenant les paramètres de requetes nécessaire au DisplayTag
	 * pour répercuter ces informations de navigation (no page, champs de tri, ordre de tri)
	 *
	 * @param tableName le nom de la table display-tag
	 * @param pagination l'objet source
	 *
	 * @return une chaine contenant les parametres de requetes utiles au displaytag
	 */
	String getDisplayTagRequestParametersForPagination(String tableName, ParamPagination pagination);

	/**
	 * Verifie que l'opérateur courant possède les droits d'accès pour un dossier si celui ci possède une décision ACI.
	 * @param tiersId numéro du tiers à vérifier
	 * @throws ObjectNotFoundException si le tiers n'existe pas
	 * @throws AccessDeniedException si l'utilisatuer n'as pas les droits necessaires
	 */
	void checkTraitementContribuableAvecDecisionAci(Long tiersId) throws ObjectNotFoundException, AccessDeniedException;
}
