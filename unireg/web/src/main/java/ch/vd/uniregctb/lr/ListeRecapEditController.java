package ch.vd.uniregctb.lr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.lr.manager.ListeRecapEditManager;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.print.PrintPCLManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class ListeRecapEditController extends AbstractListeRecapController {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapListController.class);

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public final static String LR_ID_PARAMETER_NAME = "id";
	public final static String DELAI_ID_PARAMETER_NAME = "idDelai";
	public final static String DEBITEUR_ID_PARAMETER_NAME = "numero";

	public final static String BUTTON_SAUVER = "__confirmed_save";
	public final static String BUTTON_IMPRIMER_LR = "imprimerLR";
	public final static String BUTTON_SOMMER_LR = "sommerLR";
	public final static String BUTTON_DUPLICATA_LR = "duplicataLR";
	public final static String BUTTON_ANNULER_LR = "annulerLR";
	public final static String TARGET_ANNULER_DELAI = "annulerDelai";

	private ListeRecapEditManager lrEditManager;
	private PrintPCLManager printPCLManager;

	public ListeRecapEditController() {
		super();
		/*
		 * [UNIREG-486] Workaround pour un bug de IE6 qui empêche d'ouvrir correctement les fichier attachés PDFs.
		 *
		 * Voir aussi:
		 *  - http://drupal.org/node/93787
		 *  - http://support.microsoft.com/default.aspx?scid=kb;en-us;316431
		 *  - http://bugs.php.net/bug.php?id=16173
		 *  - http://pkp.sfu.ca/support/forum/viewtopic.php?p=359&sid=4516e6d325c613c7875f67e1b9194c57
		 *  - http://forum.springframework.org/showthread.php?t=24466
		 */
		setCacheSeconds(-1);
	}


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		String idLrParam = request.getParameter(LR_ID_PARAMETER_NAME);
		String idDpiParam = request.getParameter(DEBITEUR_ID_PARAMETER_NAME);

		ListeRecapDetailView lrEditView = null;

		if(!SecurityProvider.isGranted(Role.LR)){
			throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
		}

		if (idLrParam != null) {
			Long id = Long.parseLong(idLrParam);
			if (idLrParam != null && !"".equals(idLrParam)) {
				lrEditView = lrEditManager.get(id);
			}
		}
		else {
			if (idDpiParam != null) {
				Long idDpi = Long.parseLong(idDpiParam);
				lrEditView = lrEditManager.creerLr(idDpi);
			}
		}

		return lrEditView;
	}

	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command, BindException errors) {
		if (getTarget() != null || request.getParameter(BUTTON_IMPRIMER_LR) != null || request.getParameter(BUTTON_DUPLICATA_LR) != null) {
			return true;
		}
		return super.suppressValidation(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		ListeRecapDetailView lrDetailView = (ListeRecapDetailView) command;
		if (BUTTON_IMPRIMER_LR.equals(getTarget())) {
			if (lrDetailView.getDelaiAccorde() == null) {
				ValidationUtils.rejectIfEmpty(errors, "delaiAccorde", "error.delai.accorde.vide");
			}
			else
			{
				if (lrDetailView.getRegDelaiAccorde().isBefore(RegDate.get())) {
					ValidationUtils.rejectIfEmpty(errors, "delaiAccorde", "error.delai.accorde.anterieur.date.jour");
				}
			}
		}
		super.onBindAndValidate(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ListeRecapDetailView bean = (ListeRecapDetailView) command;

		if (getTarget() != null) {
			if (TARGET_ANNULER_DELAI.equals(getTarget())) {
				String delai = getEventArgument();
				Long idDelai = Long.parseLong(delai);
				lrEditManager.annulerDelai(bean, idDelai);
			}
			else if (BUTTON_IMPRIMER_LR.equals(getTarget())) {
				final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalLR(bean);
				if (resultat != null && resultat.getDocument() != null) {
					printPCLManager.openPclStream(response, resultat.getDocument());
				}
				else {
					final String message = String.format("%s Veuillez imprimer un duplicata de la liste récapitulative.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
					throw new EditiqueCommunicationException(message);
				}
			}
		}
		else {
			if (request.getParameter(BUTTON_SAUVER) != null) {
				lrEditManager.save(bean);
				this.setModified(false);
				return new ModelAndView("redirect:edit-debiteur.do?numero=" + bean.getDpi().getNumero().toString());
			}
			else if (request.getParameter(BUTTON_DUPLICATA_LR) != null) {
				final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalDuplicataLR(bean);
				if (resultat != null && resultat.getDocument() != null) {
					printPCLManager.openPclStream(response, resultat.getDocument());
				}
				else {
					final String message = String.format("%s Veuillez ré-essayer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
					throw new EditiqueCommunicationException(message);
				}
			}
			//TODO [FDE] A supprimer - Mis ici pour tester les Sommations LR
			else if (request.getParameter(BUTTON_SOMMER_LR) != null) {
				final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalSommationLR(bean);
				if (resultat != null && resultat.getDocument() != null) {
					printPCLManager.openPclStream(response, resultat.getDocument());
				}
				else {
					final String message = String.format("%s Veuillez ré-essayer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
					throw new EditiqueCommunicationException(message);
				}
			}
			else if (request.getParameter(BUTTON_ANNULER_LR) != null) {
				lrEditManager.annulerLR(bean);
				return new ModelAndView("redirect:edit-debiteur.do?numero=" + bean.getDpi().getNumero().toString());
			}
		}
		lrEditManager.refresh(bean);
		return showForm(request, response, errors);
	}

	public void setLrEditManager(ListeRecapEditManager lrEditManager) {
		this.lrEditManager = lrEditManager;
	}


	public void setPrintPCLManager(PrintPCLManager printPCLManager) {
		this.printPCLManager = printPCLManager;
	}

}
