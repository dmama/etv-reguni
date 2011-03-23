package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotImpressionView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class DeclarationImpotImpressionController extends AbstractDeclarationImpotController {

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditController.class);

	public final static String DI_ID_PARAMETER_NAME = "id";

	public final static String TYPE_DOCUMENT_PARAMETER_NAME = "typeDocument";

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String IMPRESSION_DI_CRITERIA_NAME = "impressionDICriteria";

	public final static String DUPLICATA_VALUE = "duplicataDI";

	private DeclarationImpotEditManager diEditManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}

	public DeclarationImpotImpressionController() {
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

		if(!SecurityProvider.isGranted(Role.DI_DUPLIC_PP)){
			throw new AccessDeniedException("vous n'avez pas le droit de dupliquer une DI");
		}
		String idDiParam = request.getParameter(DI_ID_PARAMETER_NAME);
		String typeDocument = request.getParameter(TYPE_DOCUMENT_PARAMETER_NAME);

		DeclarationImpotImpressionView diImpressionView = null;

		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);

		HttpSession session = request.getSession(true);
		if ((buttonEffacer != null) && (buttonEffacer.equals(EFFACER_PARAMETER_VALUE) )) {
			session.removeAttribute(IMPRESSION_DI_CRITERIA_NAME);
			if (diImpressionView == null) {
				if (idDiParam != null) {
					Long id = Long.parseLong(idDiParam);
					diEditManager.controleDI(id);
					diImpressionView = diEditManager.getView(id, null);
				}
			}
		} else {
			diImpressionView = (DeclarationImpotImpressionView) session.getAttribute(IMPRESSION_DI_CRITERIA_NAME);
		 	if (diImpressionView == null) {
				if (idDiParam != null) {
					Long id = Long.parseLong(idDiParam);
					diEditManager.controleDI(id);
					diImpressionView = diEditManager.getView(id, typeDocument);
				}
		 	}
		}

		// vérification des droits d'accès au dossier du contribuable
		if (diImpressionView != null) {
			final Long idDi = diImpressionView.getIdDI();
			final Long tiersId = diEditManager.getTiersId(idDi);
			checkAccesDossierEnEcriture(tiersId);
		}

		return diImpressionView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		final TracePoint tp = TracingManager.begin();
		try {

			final ModelAndView mav = super.onSubmit(request, response, command, errors);

			final String action = request.getParameter(ACTION_PARAMETER_NAME);
			final DeclarationImpotImpressionView bean = (DeclarationImpotImpressionView) command;

			final Long idDi = bean.getIdDI();
			final Long tiersId = diEditManager.getTiersId(idDi);
			checkAccesDossierEnEcriture(tiersId);

			if(action.equals(EFFACER_PARAMETER_VALUE)) {
				final HttpSession session = request.getSession();
				session.setAttribute(IMPRESSION_DI_CRITERIA_NAME, bean);
				mav.setView(new RedirectView("impression.do"));
			}
			else if (action.equals(DUPLICATA_VALUE)) {

				final TraitementRetourEditique inbox = new TraitementRetourEditique() {
					@Override
					public ModelAndView doJob(EditiqueResultat resultat) {
						mav.setView(new RedirectView(String.format("edit.do?action=editdi&id=%d", idDi)));
						return null;
					}
				};

				final TraitementRetourEditique erreur = new TraitementRetourEditique() {
					@Override
					public ModelAndView doJob(EditiqueResultat resultat) {
						final String message = String.format("%s Veuillez recommencer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
						throw new EditiqueCommunicationException(message);
					}
				};

				final EditiqueResultat result = diEditManager.envoieImpressionLocalDuplicataDI(bean);
				traiteRetourEditique(result, response, "di", inbox, erreur, erreur);
			}

			return mav;
		}
		finally {
			TracingManager.end(tp);
			TracingManager.outputMeasures(LOGGER);
		}
	}

}
