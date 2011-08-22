package ch.vd.uniregctb.security;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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

		// [SIFISC-2078] Tri des collectivités administratives affichées par ordre alphabétique du nom
		List<CollectiviteAdministrative> list = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
		if (list.size() > 1) {
			// recopie dans une autre liste, au cas où host-interface choisi un jour de nous renvoyer une collection immutable
			list = new ArrayList<CollectiviteAdministrative>(list);
			Collections.sort(list, new Comparator<CollectiviteAdministrative>() {
				@Override
				public int compare(CollectiviteAdministrative o1, CollectiviteAdministrative o2) {
					return o1.getNomCourt().compareTo(o2.getNomCourt());
				}
			});
		}
		view.setOfficesImpot(list);

		return view;
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
