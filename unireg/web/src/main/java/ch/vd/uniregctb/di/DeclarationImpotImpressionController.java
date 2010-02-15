package ch.vd.uniregctb.di;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotImpressionView;
import ch.vd.uniregctb.print.PrintPCLManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class DeclarationImpotImpressionController  extends AbstractDeclarationImpotController {

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditController.class);

	public final static String DI_ID_PARAMETER_NAME = "id";

	public final static String TYPE_DOCUMENT_PARAMETER_NAME = "typeDocument";

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String IMPRESSION_DI_CRITERIA_NAME = "impressionDICriteria";

	public final static String DUPLICATA_VALUE = "duplicataDI";

	private DeclarationImpotEditManager diEditManager;
	private PrintPCLManager printPCLManager;

	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}

	public void setPrintPCLManager(PrintPCLManager printPCLManager) {
		this.printPCLManager = printPCLManager;
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
			DeclarationImpotOrdinaire di = getDiDAO().get(diImpressionView.getIdDI());
			checkAccesDossierEnEcriture(di.getTiers().getNumero());
		}

		return diImpressionView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);

		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		TracePoint tp = TracingManager.begin();

		LOGGER.debug("Traitement du formulaire...");
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		String action = request.getParameter(ACTION_PARAMETER_NAME);
		DeclarationImpotImpressionView bean = (DeclarationImpotImpressionView) command;

		DeclarationImpotOrdinaire di = getDiDAO().get(bean.getIdDI());
		checkAccesDossierEnEcriture(di.getTiers().getNumero());

		if(action.equals(EFFACER_PARAMETER_VALUE)) {

			HttpSession session = request.getSession();
			session.setAttribute(IMPRESSION_DI_CRITERIA_NAME, bean);
			mav.setView(new RedirectView("impression.do"));
		}
		else if (action.equals(DUPLICATA_VALUE)) {

			SimpleDateFormat formater = new java.text.SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
			Date dateEnvoi = new Date();
			String message = "Impression duplicata DI Début communication avec l'éditique - Timestamp:" + formater.format(dateEnvoi);
			LOGGER.debug(message);
			String docID = diEditManager.envoieImpressionLocalDuplicataDI(bean);
			LOGGER.debug("docID:" + docID + "--");
			byte[] pcl = diEditManager.recoitImpressionLocal(docID);
			if (pcl == null) {
				throw new EditiqueCommunicationException("La communication avec l'éditique à échoué. Veuillez recommencer plus tard.");
			}
			printPCLManager.openPclStream(request, response, pcl);

		}

		TracingManager.end(tp);

		TracingManager.outputMeasures(LOGGER);

		return mav;
	}

}
