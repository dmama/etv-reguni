package ch.vd.uniregctb.webservices.common;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.error.v1.Error;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.type.Niveau;

public abstract class WebServiceHelper {

	private static final Logger LOGGER = Logger.getLogger(WebServiceHelper.class);

	/**
	 * @return le nom de l'utilisateur utilisé pour se connecter au web-service en mode <i>basic authentication</i>; ou "n/a" si cette information n'existe pas.
	 */
	public static String getBasicAuthenticationUser(HttpServletRequest request) {
		final Principal userPrincipal = (request == null ? null : request.getUserPrincipal());
		return (userPrincipal == null ? "n/a" : userPrincipal.getName());
	}

	/**
	 * Utilisé pour le log d'un appel web-service
	 * @param accessLogger le logger dans lequel (au niveau {@link org.apache.log4j.Level#INFO}) on enverra les informations de l'appel
	 * @param request requête HTTP entrante (pour y récupérer l'éventuelle donnée d'authentification)
	 * @param params un objet dont la méthode {@link #toString()} sera appelée pour la documentation de l'appel
	 * @param duration durée de l'appel, en nano-secondes
	 * @param load nombre d'appel en cours (y compris celui-ci)
	 * @param status le statut HTTP de la réponse
	 * @param t éventuelle exception lancée pendant l'appel
	 */
	public static void logAccessInfo(Logger accessLogger, HttpServletRequest request, Object params, long duration, int load, @Nullable Response.Status status, @Nullable Throwable t) {
		if (accessLogger.isInfoEnabled()) {
			final String user = getBasicAuthenticationUser(request);
			final String exceptionString = (t == null ? StringUtils.EMPTY : String.format(", %s thrown", t.getClass()));
			final String statusString = (status == null ? StringUtils.EMPTY : String.format(" status='%d %s'", status.getStatusCode(), status.getReasonPhrase()));
			accessLogger.info(String.format("[%s] (%d ms) %s load=%d%s%s", user, TimeUnit.NANOSECONDS.toMillis(duration), params.toString(), load, statusString, exceptionString));
		}
	}

	/**
	 * Parse la chaîne de caractères donnée en entrée pour en faire un {@link UserLogin}
	 * @param login la chaîne de caractères à parser
	 * @return le résultat du parsing ou <code>null</code> si la valeur ne correspondait à rien
	 */
	@Nullable
	public static UserLogin parseLoginParameter(String login) {
		if (StringUtils.isNotBlank(login)) {
			try {
				return UserLogin.fromString(login);
			}
			catch (IllegalArgumentException e) {
				LOGGER.error(e, e);
			}
		}
		return null;
	}

	/**
	 * Initialise les données d'authentification du thread courant avec les données présente dans l'objet {@link UserLogin} donné.
	 * Ces données doivent ensuite être effacées une fois le travail terminé par un appel à {@link #logout()}
	 * @param userLogin les nouvelles données d'authentification
	 */
	public static void login(UserLogin userLogin) {
		AuthenticationHelper.pushPrincipal(userLogin.userId, userLogin.oid);
	}

	/**
	 * Efface les données d'authentification du thread courrant précédemment enregistrées par un appel à {@link #login(UserLogin)}
	 */
	public static void logout() {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Vérifie que le login donné possède au moins l'un des rôles donnés
	 * @param securityProvider le service de vérification des droits d'accès
	 * @param userLogin les données d'authentification
	 * @param roles les rôles attendus
	 * @throws AccessDeniedException si aucun rôle n'est fourni ou aucun de ceux fournis ne sont associés à l'utilisateur authentifié
	 */
	public static void checkAnyAccess(SecurityProviderInterface securityProvider, UserLogin userLogin, Role... roles) throws AccessDeniedException {
		boolean foundOne = false;
		for (Role role : roles) {
			if (securityProvider.isGranted(role, userLogin.userId, userLogin.oid)) {
				foundOne = true;
				break;
			}
		}
		if (!foundOne) {
			throw new AccessDeniedException(String.format("L'utilisateur %s ne possède aucun des droits %s", userLogin, Arrays.toString(roles)));
		}
	}

	/**
	 * Vérifie que le login donné possède le rôle donné
	 * @param securityProvider le service de vérification des droits d'accès
	 * @param userLogin les données d'authentification
	 * @param role le rôle attendu
	 * @throws AccessDeniedException si le rôle demandé n'est pas associé à l'utilisateur authentifié
	 */
	public static void checkAccess(SecurityProviderInterface securityProvider, UserLogin userLogin, Role role) throws AccessDeniedException {
		if (!securityProvider.isGranted(role, userLogin.userId, userLogin.oid)) {
			throw new AccessDeniedException(String.format("L'utilisateur %s ne possède pas le droit %s", userLogin, role));
		}
	}

	/**
	 * Vérifie que l'utilisateur donné a bien au moins un droit de visualisation sur le dossier donné
	 * @param securityProvider le service de vérification des droits d'accès
	 * @param userLogin les données d'authentification
	 * @param partyNo le numéro du dossier concerné
	 * @throws AccessDeniedException si le droit de visualisation n'est pas attribué à l'utilisateur authentifié
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException si le dossier n'existe pas
	 */
	public static void checkPartyReadAccess(SecurityProviderInterface securityProvider, UserLogin userLogin, int partyNo) throws AccessDeniedException {
		if (securityProvider.getDroitAcces(userLogin.userId, partyNo) == null) {
			throw new AccessDeniedException(String.format("L'utilisateur %s ne possède aucun droit de lecture sur le dossier %d", userLogin, partyNo));
		}
	}

	/**
	 * Vérifie que l'utilisateur donné a bien un droit de modification sur le dossier donné
	 * @param securityProvider le service de vérification des droits d'accès
	 * @param userLogin les données d'authentification
	 * @param partyNo le numéro du dossier concerné
	 * @throws AccessDeniedException si le droit de modification n'est pas attribué à l'utilisateur authentifié
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException si le dossier n'existe pas
	 */
	public static void checkPartyReadWriteAccess(SecurityProviderInterface securityProvider, UserLogin userLogin, int partyNo) throws AccessDeniedException {
		if (securityProvider.getDroitAcces(userLogin.userId, partyNo) != Niveau.ECRITURE) {
			throw new AccessDeniedException(String.format("L'utilisateur %s ne possède aucun droit d'écriture sur le dossier %d", userLogin, partyNo));
		}
	}

	/**
	 * Construit une réponse (en erreur) avec le message donné
	 * @param status le statut d'erreur à mettre dans la réponse
	 * @param errorMessage le message d'erreur à envoyer dans le corps de la réponse
	 * @return la réponse elle même
	 */
	public static Response buildErrorResponse(Response.Status status, String errorMessage) {
		final ch.vd.unireg.ws.error.v1.ObjectFactory factory = new ch.vd.unireg.ws.error.v1.ObjectFactory();
		return Response.status(status).entity(factory.createError(new Error(errorMessage))).build();
	}

	/**
	 * Construit une réponse (en erreur) avec le message extrait de l'exception donnée
	 * @param status le statut d'erreur à mettre dans la réponse
	 * @param t une exception dont on va extraire le message d'erreur
	 * @return la réponse elle même
	 */
	public static Response buildErrorResponse(Response.Status status, Throwable t) {
		return buildErrorResponse(status, buildExceptionMessage(t));
	}

	/**
	 * Construit une chaîne de caractères à partir du message de l'exception (en cas d'absence de message, c'est le nom de la classe de l'exception qui est retourné)
	 * @param t throwable
	 * @return chaîne de caractères
	 */
	public static String buildExceptionMessage(Throwable t) {
		final String msg = t.getMessage();
		if (StringUtils.isNotBlank(msg)) {
			return msg;
		}
		else {
			return t.getClass().getName();
		}
	}

	/**
	 * Renvoie le type préféré pour une réponse pour laquelle le client a déclaré accepter certains types et le server propose une liste
	 * @param sortedAcceptHeaders les types acceptés par le client, par ordre de préférence décroissante
	 * @param producedTypes les types proposés par le serveur, par ordre de préférence décroissante (= la valeur par défaut en premier)
	 * @return le type choisi, ou <code>null</code> si aucun type n'est compatible
	 */
	public static MediaType getPreferedMediaType(List<MediaType> sortedAcceptHeaders, MediaType[] producedTypes) {
		if (sortedAcceptHeaders == null || sortedAcceptHeaders.isEmpty()) {
			return producedTypes[0];
		}
		for (MediaType acceptedMediaType : sortedAcceptHeaders) {
			for (MediaType producedMediaType : producedTypes) {
				if (producedMediaType.isCompatible(acceptedMediaType)) {
					return producedMediaType;
				}
			}
		}
		return null;
	}
}
