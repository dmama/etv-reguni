package ch.vd.uniregctb.security;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.UrlEncodedQueryString;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

/**
 * Ce contrôleur gère l'écran de sélection de l'office d'impôt de district (OID). Ce contrôleur est automatiquement appelé par le filtre {@link ChooseOIDProcessingFilter} lorsqu'un utilisateur doit
 * choisir son OID de travail.
 */
@Controller
public class ChooseOIDController {

	private ServiceSecuriteService serviceSecurite;

	@RequestMapping(value = "/chooseOID.do", method = RequestMethod.GET)
	public String chooseOID(Model mav, HttpSession session) {

		final UniregSecurityDetails details = getProfilSecuriteCourant();
		if (details != null) {
			// [UNIREG-1242] On reset un éventuel profil de sécurité déjà mémorisé, de manière à ce que le filtre IFOSecAuthenticationProcessingFilter (re)sélectionne bien l'OID choisi.
			details.setIfoSecOID(null);
			details.setIfoSecOIDSigle(null);
			details.setIfoSecProfil(null);
		}

		final ChooseOIDView view = new ChooseOIDView();
		view.setInitialUrl(getInitialUrl(session));
		view.setOfficesImpot(getOfficesImpot());
		mav.addAttribute("command", view);

		return "security/chooseOID";
	}

	@RequestMapping(value = "/chooseOID.do", method = RequestMethod.POST)
	public String chooseOID(@Valid @ModelAttribute("command") final ChooseOIDView view, HttpSession session) throws URISyntaxException {
		session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		return "redirect:" + setParam(view.getInitialUrl(), ChooseOIDProcessingFilter.IFOSEC_OID_PARAM, String.valueOf(view.getSelectedOID()));
	}

	/**
	 * @return les offices d'impôt autorisés pour l'utilisateur courant
	 */
	private List<CollectiviteAdministrative> getOfficesImpot() {
		// [SIFISC-2078] Tri des collectivités administratives affichées par ordre alphabétique du nom
		List<CollectiviteAdministrative> list = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
		if (list != null && list.size() > 1) {
			// recopie dans une autre liste, au cas où host-interface choisit un jour de nous renvoyer une collection immutable
			list = new ArrayList<CollectiviteAdministrative>(list);
			Collections.sort(list, new Comparator<CollectiviteAdministrative>() {
				@Override
				public int compare(CollectiviteAdministrative o1, CollectiviteAdministrative o2) {
					return o1.getNomCourt().compareTo(o2.getNomCourt());
				}
			});
		}
		return list;
	}

	/**
	 * Récupère la destination initiale de l'utilisateur, avant que le filtre n'intercepte l'appel
	 *
	 * @param session la session http
	 * @return l'url de destination
	 */
	private static String getInitialUrl(HttpSession session) {
		final Object e = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		if (e instanceof MultipleOIDFoundException) {
			final MultipleOIDFoundException exception = (MultipleOIDFoundException) e;
			Flash.message(exception.getMessage());
			return exception.getInitialUrl();
		}
		else if (e instanceof UnauthorizedOIDException) {
			final UnauthorizedOIDException exception = (UnauthorizedOIDException) e;
			Flash.warning(exception.getMessage());
			return exception.getInitialUrl();
		}
		else {
			return "index.do";
		}
	}

	/**
	 * Ajoute ou met-à-jour un paramètre sur une URL.
	 *
	 * @param url        une url
	 * @param paramName  le nom du paramètre
	 * @param paramValue la valeur du paramètre
	 * @return l'url avec le paramètre ajouté ou mis-à-jour
	 * @throws URISyntaxException si l'url d'entrée n'est pas valide
	 */
	private static String setParam(String url, String paramName, String paramValue) throws URISyntaxException {
		URI uri = new URI(url);
		final UrlEncodedQueryString queryString = UrlEncodedQueryString.parse(uri);
		queryString.set(paramName, paramValue);
		uri = queryString.apply(uri);
		return uri.toString();
	}

	/**
	 * @return le profil de sécurité de l'opérateur courant.
	 */
	private UniregSecurityDetails getProfilSecuriteCourant() {
		// Récupération du contexte de sécurité
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null) {
			return null;
		}

		// Récupération des données d'authentification
		Authentication auth = context.getAuthentication();
		if (auth == null) {
			return null;
		}

		// Récupération du profil dans l'objet Authentication
		return (UniregSecurityDetails) auth.getDetails();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}
}
