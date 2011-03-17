package ch.vd.uniregctb.security;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CommonSimpleFormController;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

/**
 * Ce contrôleur récupère la liste des offices d'impôts à choix pour un utilisateur.<p> <b>Note:</b> Lorsque l'utilisateur choisit un office d'impôt et valide le formulaire (voir
 * <i>chooseOID.jsp</i>), il est automatiquement redirigé sur la page <i>index.do</i>. Avant d'y parvenir, le filtre <i>IFOSecAuthenticationProcessingFilter</i> est appelé, et celui va lire l'OID
 * choisi par l'utilisateur et renseigner les valeurs qui vont bien dans le profil de sécurité courant. C'est pourquoi ce contrôleur ne fait rien dans le <i>onSubmit</i> : tout est fait par le filtre
 * <i>IFOSecAuthenticationProcessingFilter</i>.
 */
public class ChooseOIDController extends CommonSimpleFormController {

	private ServiceSecuriteService serviceSecurite;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final ChooseOIDView view = (ChooseOIDView) super.formBackingObject(request);

		final UniregSecurityDetails details = getProfilSecuriteCourant();
		if (details != null) {
			// [UNIREG-1242] On reset un éventuel profil de sécurité déjà mémorisé, de manière à ce que le filtre IFOSecAuthenticationProcessingFilter (re)sélectionne bien l'OID choisi.
			details.setIfoSecOID(null);
			details.setIfoSecOIDSigle(null);
			details.setIfoSecProfil(null);
		}

		final List<CollectiviteAdministrative> list = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
		view.setOfficesImpot(list);

		return view;
	}

	/**
	 * @return le profil de sécurité de l'opérateur courant.
	 */
	private UniregSecurityDetails getProfilSecuriteCourant() {
		// Récupération du contexte Acegi
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
