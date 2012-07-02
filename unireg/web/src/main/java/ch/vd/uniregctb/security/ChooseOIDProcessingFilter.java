package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.URLHelper;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

/**
 * Ce filtre permet de renseigner l'office d'impôt d'un utilisateur. Si l'utilisateur ne possède qu'un seul office d'impôt, ce dernier est sélectionné d'office et le processus est transparent. Si
 * l'utilisateur possède plusieurs offices d'impôt, une page de sélection est affichée et l'utilisateur doit sélectionner l'office d'impôt voulu.
 */
public class ChooseOIDProcessingFilter extends GenericFilterBean {

	private final static Logger LOGGER = Logger.getLogger(ChooseOIDProcessingFilter.class);

	public static final String IFOSEC_OID_PARAM = "ifosec-oid";

	private ServiceSecuriteService serviceSecurite;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		if (AuthenticationHelper.getCurrentOID() == null) {

			final HttpServletRequest request = (HttpServletRequest) servletRequest;
			final HttpServletResponse response = (HttpServletResponse) servletResponse;

			final String username = AuthenticationHelper.getCurrentPrincipal();
			if (StringUtils.isBlank(username)) {
				throw new IllegalArgumentException();
			}

			final List<CollectiviteAdministrative> collectivites;
			try {
				collectivites = serviceSecurite.getCollectivitesUtilisateur(username);
			}
			catch (ServiceSecuriteException e) {
				LOGGER.error(e, e);
				throw e;
			}
			if (collectivites == null || collectivites.isEmpty()) {
				throw new AuthenticationFailedException("L'utilisateur " + username + " ne possède aucune collectivité.");
			}

			CollectiviteAdministrative oi = null;

			if (collectivites.size() == 1) {
				// si l'utilisateur ne possède qu'un seul OID, on le choisit d'office.
				oi = collectivites.get(0);
			}
			else {
				final Integer oid = getIntegerParam(request, IFOSEC_OID_PARAM);
				if (oid == null) {
					// plusieurs OIDs => on redirige vers l'écran de choix
					new SimpleUrlAuthenticationFailureHandler("/chooseOID.do").onAuthenticationFailure(request, response,
							new MultipleOIDFoundException("Vous avez plusieurs OIDs, merci d'en choisir un.", URLHelper.getTargetUrl(request)));
					return;
				}

				for (CollectiviteAdministrative ca : collectivites) {
					if (ca.getNoColAdm() == oid) {
						oi = ca;
						break;
					}
				}
				if (oi == null) {
					// l'OID choisi (ou prérempli) n'est pas autorisé => on redirige vers l'écran de choix
					final String targetUrl = URLHelper.getTargetUrl(request);
					LOGGER.warn(String.format("Tentative de login sur l'OID %d non-autorisé pour l'utilisateur %s %s (%s) !",
							oid, AuthenticationHelper.getFirstName(), AuthenticationHelper.getLastName(), targetUrl));
					new SimpleUrlAuthenticationFailureHandler("/chooseOID.do").onAuthenticationFailure(request, response,
							new UnauthorizedOIDException("Vous n'avez pas l'autorisation de vous connecter sur l'OID numéro " + oid + " !", targetUrl));
					return;
				}
			}

			LOGGER.info(String.format("Choix de l'OID %d (%s) pour l'utilisateur %s %s", oi.getNoColAdm(), oi.getSigle(), AuthenticationHelper.getFirstName(), AuthenticationHelper.getLastName()));

			// On peut maintenant renseigner l'oid
			AuthenticationHelper.setCurrentOID(oi.getNoColAdm(), oi.getSigle());
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Nullable
	private static Integer getIntegerParam(HttpServletRequest request, String paramName) {

		final String val = request.getParameter(paramName);
		if ("{OID}".equals(val)) {
			// [SIFISC-1035] l'autre application fiscale n'a pas renseigné l'OID et Fidor n'a pas résolu le paramètre => inutile de chercher plus loin
			return null;
		}
		
		try {
			return Integer.parseInt(val);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}
}
