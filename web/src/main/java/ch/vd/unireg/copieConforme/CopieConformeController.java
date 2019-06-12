package ch.vd.unireg.copieConforme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.HttpHelper;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.TypeDocumentEditique;

@Controller
public class CopieConformeController {

	private static final String ID_DELAI = "idDelai";
	private static final String ID_ETAT = "idEtat";
	private static final String ID_DOC = "idDoc";

	private static final String NOCTB = "noCtb";
	private static final String TYPE_DOC = "typeDoc";
	private static final String KEY = "key";

	/**
	 * Temps (ms) après lequel un message d'erreur doit être effacé automatiquement
	 */
	private static final long errorFadingTimeout = 5000L;

	private CopieConformeManager copieConformeManager;

	private RetourEditiqueControllerHelper helper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHelper(RetourEditiqueControllerHelper helper) {
		this.helper = helper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCopieConformeManager(CopieConformeManager copieConformeManager) {
		this.copieConformeManager = copieConformeManager;
	}

	private interface CopieConformeGetter {
		/**
		 * @return un flux duquel on peut extraire le contenu du document, où <code>null</code> si aucun document n'a pu être trouvé
		 * @throws EditiqueException en cas d'erreur lors de la récupération du contenu du document
		 */
		EditiqueResultat getCopieConforme() throws EditiqueException;
	}

	/**
	 * Traitement d'une demande de copie conforme
	 * @param request HTTP request de la demande de copie conforme
	 * @param response HTTP response (dans laquelle le document sera renvoyé)
	 * @param filenameRadical radical du nom du fichier du document à renvoyer
	 * @param errorMessageIfNoSuchDocument message d'erreur au cas où le document demandé n'existe pas dans l'archivage
	 * @param getter l'implémentation spécifique de récupération du document
	 * @return <code>null</code> si le document a bien été renvoyé dans la réponse HTTP, "redirect:..." en cas d'erreur
	 * @throws EditiqueException en cas d'erreur lors de la récupération du document depuis les archives
	 * @throws IOException en cas d'erreurs lors du streaming du document
	 */
	private String getDocumentCopieConforme(final HttpServletRequest request, HttpServletResponse response, String filenameRadical, final String errorMessageIfNoSuchDocument, CopieConformeGetter getter) throws EditiqueException, IOException {
		final EditiqueResultat reponseEditique = getter.getCopieConforme();
		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultat> redirect = resultat -> HttpHelper.getRedirectPagePrecedente(request);
		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
			final Integer errorCode = resultat.getErrorCode();
			final String errorMessage;
			if (errorCode != null && errorCode == 404 && StringUtils.isNotBlank(errorMessageIfNoSuchDocument)) {
				errorMessage = errorMessageIfNoSuchDocument;
			}
			else if (StringUtils.isNotBlank(resultat.getErrorMessage())) {
				errorMessage = resultat.getErrorMessage();
			}
			else {
				errorMessage = "Erreur inattendue.";
			}
			Flash.error(errorMessage, errorFadingTimeout);
			return HttpHelper.getRedirectPagePrecedente(request);
		};

		return helper.traiteRetourEditique(reponseEditique, response, filenameRadical, redirect, null, erreur);
	}

	@RequestMapping(value = "/declaration/copie-conforme-delai.do", method = RequestMethod.GET)
	public String getDocumentDelai(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_DELAI, required = true) final Long idDelai) throws Exception {
		return getDocumentCopieConforme(request, response, "copieDelai", "Aucun archivage trouvé pour la confirmation de délai demandée !", () -> copieConformeManager.getPdfCopieConformeDelai(idDelai));
	}

	@RequestMapping(value = "/declaration/copie-conforme-sommation.do", method = RequestMethod.GET)
	public String getDocumentSommation(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_ETAT, required = true) final Long idEtat) throws Exception {
		return getDocumentCopieConforme(request, response, "copieSommation", "Aucun archivage trouvé pour la sommation de déclaration demandée !", () -> copieConformeManager.getPdfCopieConformeSommation(idEtat));
	}

	@RequestMapping(value = "/declaration/copie-conforme-rappel.do", method = RequestMethod.GET)
	public String getDocumentRappel(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_ETAT, required = true) final Long idEtat) throws Exception {
		return getDocumentCopieConforme(request, response, "copieRappel", "Aucun archivage trouvé pour le rappel demandé !", () -> copieConformeManager.getPdfCopieConformeRappel(idEtat));
	}

	@RequestMapping(value = "/autresdocs/copie-conforme-envoi.do", method = RequestMethod.GET)
	public String getAutreDocumentFiscalInitial(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_DOC, required = true) final Long idDocument) throws Exception {
		return getDocumentCopieConforme(request, response, "copieEnvoi", "Aucun archivage trouvé pour le document demandé !", () -> copieConformeManager.getPdfCopieConformeEnvoiAutreDocumentFiscal(idDocument));
	}

	@RequestMapping(value = "/autresdocs/copie-conforme-rappel.do", method = RequestMethod.GET)
	public String getAutreDocumentFiscalRappel(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_DOC, required = true) final Long idDocument) throws Exception {
		return getDocumentCopieConforme(request, response, "copieRappel", "Aucun archivage trouvé pour le document demandé !", () -> copieConformeManager.getPdfCopieConformeRappelAutreDocumentFiscal(idDocument));
	}

	@RequestMapping(value = "/copie-conforme.do", method = RequestMethod.GET)
	public String getDocument(HttpServletRequest request, HttpServletResponse response,
	                          @RequestParam(value = NOCTB, required = true) final long noCtb,
	                          @RequestParam(value = TYPE_DOC, required = true) final TypeDocumentEditique typeDoc,
	                          @RequestParam(value = KEY, required = true) final String key) throws Exception {
		return getDocumentCopieConforme(request, response, "document", "Aucun archivage trouvé pour le document demandé !", () -> copieConformeManager.getPdfCopieConforme(noCtb, typeDoc, key));
	}
}
