package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.springframework.web.util.HtmlUtils;

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
			final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
			if (acces == null) {
				final String message = String.format("L'opérateur [%s] s'est vu refusé l'accès en lecture sur le tiers n°%d",
						AuthenticationHelper.getCurrentPrincipal(), tiersId);
				LOGGER.warn(message);
				throw new AccessDeniedException(String.format("Vous ne possédez pas les droits de visualisation sur le contribuable %s.", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
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
			final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
			if (acces == null || acces == Niveau.LECTURE) {
				final String message = String.format(
						"L'opérateur [%s] s'est vu refusé l'accès en écriture sur le tiers n°%d (acces autorisé=%s)", AuthenticationHelper
								.getCurrentPrincipal(), tiersId, (acces == null ? "null" : acces.toString()));
				LOGGER.warn(message);
				throw new AccessDeniedException(String.format("Vous ne possédez pas les droits d'édition sur le contribuable %s.", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
			}
		}
	}

	/**
	 * A partir d'une requete, cette méthode construit une String
	 * contenant les paramètres de requetes nécessaire au DisplayTag
	 * pour répercuter ces informations de navigation (no page, champs de tri, ordre de tri)
	 *
	 * @param request request avec laquelle la table a déjà été affichée
	 * @param tableName le nom de la table
	 * @return une chaîne de caractères (<code>null</code> si vide) dans laquelle ont été extraits les paramètres de pagination pour la table donnée, depuis la requête donnée
	 */
	public static String getDisplayTagRequestParametersForPagination(HttpServletRequest request, String tableName) {
		final ParamEncoder encoder = new ParamEncoder(tableName);
		final String pageParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
		final String sortUsingNameParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORTUSINGNAME);
		final String sortParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
		final String orderParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
		return getDisplayTagRequestParametersForPagination(
				tableName,
				StringUtils.trimToNull(request.getParameter(pageParamName)),
				StringUtils.trimToNull(request.getParameter(sortUsingNameParamName)),
				StringUtils.trimToNull(request.getParameter(sortParamName)),
				StringUtils.trimToNull(request.getParameter(orderParamName)),
				true
		);

	}

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
	public static String getDisplayTagRequestParametersForPagination(String tableName, ParamPagination pagination) {
		return getDisplayTagRequestParametersForPagination(
				tableName,
				Integer.toString(pagination.getNumeroPage()),
				"1",
				pagination.getSorting().getField(),
				pagination.getSorting().isAscending() ? "1" : "2",
				false);
	}
	
	public static String getDisplayTagRequestParametersForPagination(String tableName, String pageParamValue, String sortUsingNameParamValue, String sortParamValue, String orderParamValue, boolean htmlEscape) {

		final ParamEncoder encoder = new ParamEncoder(tableName);
		final String pageParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
		final String sortUsingNameParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORTUSINGNAME);
		final String sortParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
		final String orderParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
		
		final StringBuilder b = new StringBuilder();
		if (pageParamValue != null) {
			b.append(String.format("%s=%s", pageParamName, pageParamValue));
		}
		if (sortUsingNameParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", sortUsingNameParamName, sortUsingNameParamValue));
		}
		if (sortParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", sortParamName, sortParamValue));
		}
		if (orderParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", orderParamName, orderParamValue));
		}

		String ret;
		if(htmlEscape) {
			ret = HtmlUtils.htmlEscape(b.toString());	
		} else {
			ret = b.toString();
		}
		return StringUtils.trimToNull(ret);
	}	
}
