package ch.vd.uniregctb.tache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tache.manager.TacheListManager;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * @author xcifde
 *
 */
public class NouveauDossierListController extends AbstractTacheController {

	protected static final Logger LOGGER = Logger.getLogger(NouveauDossierListController.class);

	public final static String BOUTON_IMPRIMER = "imprimer";

	private static final String TABLE_NOUVEAU_DOSSIER_ID = "nouveauDossier";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final String NOUVEAU_DOSSIER_CRITERIA_NAME = "nouveauDossierCriteria";
	public static final String NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME = "nouveauxDossiers";
	public static final int PAGE_SIZE = 25;

	private TacheListManager tacheListManager;

	public TacheListManager getTacheListManager() {
		return tacheListManager;
	}

	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	public NouveauDossierListController() {
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

		final TracePoint tp = TracingManager.begin();

		final HttpSession session = request.getSession();

		NouveauDossierCriteriaView bean = (NouveauDossierCriteriaView) session.getAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME);

		if (bean == null || isAppuiSurEffacer(request)) {
			bean = (NouveauDossierCriteriaView) super.formBackingObject(request);
			bean.setEtatTache(TypeEtatTache.EN_INSTANCE.toString());
			bean.setOfficeImpot(getDefaultOID());
			session.setAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME, bean);
		}

		TracingManager.end(tp);
		return bean;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		final TracePoint tp = TracingManager.begin();
		final ModelAndView mav = super.showForm(request, response, errors, model);

		LOGGER.debug("Affichage du formulaire de recherche...");
		final HttpSession session = request.getSession();

		final NouveauDossierCriteriaView bean = (NouveauDossierCriteriaView) session.getAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME);
		final boolean faireRecherche = bean != null && !bean.isEmpty() && !isAppuiSurEffacer(request);
		if (faireRecherche) {
			LOGGER.debug("Critères de recherche=" + bean);
			bean.setTabIdsDossiers(null);
			final WebParamPagination pagination = new WebParamPagination(request, TABLE_NOUVEAU_DOSSIER_ID, PAGE_SIZE);
			List<NouveauDossierListView> dossiersView = tacheListManager.find(bean, pagination);
			mav.addObject(NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME, dossiersView);
			mav.addObject(RESULT_SIZE_NAME, tacheListManager.count(bean));
		}
		else {
			mav.addObject(NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME, new ArrayList<NouveauDossierListView>());
			mav.addObject(RESULT_SIZE_NAME, 0);
		}

		TracingManager.end(tp);
		return mav;
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
			mav.setView(new RedirectView(getSuccessView()));

			final NouveauDossierCriteriaView bean = (NouveauDossierCriteriaView) command;
			final HttpSession session = request.getSession();
			session.setAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME, bean);

			if (request.getParameter(BOUTON_IMPRIMER) != null) {
				try {

					final TraitementRetourEditique erreur = new TraitementRetourEditique() {
						@Override
						public ModelAndView doJob(EditiqueResultat resultat) {
							final String message = String.format("%s Veuillez recommencer l'opération ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
							throw new EditiqueCommunicationException(message);
						}
					};

					final EditiqueResultat resultat = tacheListManager.envoieImpressionLocalDossier(bean);
					return traiteRetourEditique(resultat, response, "dossier", null, erreur, erreur);
				}
				catch (EditiqueException e) {
					LOGGER.error(e, e);
					// UNIREG-1218 : on affiche le message d'erreur de manière sympa
					throw new ActionException(e.getMessage());
				}
			}

			return mav;
		}
		finally {
			TracingManager.end(tp);
			TracingManager.outputMeasures(LOGGER);
		}
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);

		final NouveauDossierCriteriaView criteriaView = (NouveauDossierCriteriaView) command;

		if (request.getParameter(BOUTON_IMPRIMER) != null) {

			// avant d'imprimer, il faut au moins avoir sélectionné un dossier
			if (criteriaView.getTabIdsDossiers() == null) {
				errors.reject("error.aucun.nouveau.dossier.selectionne");
			}
		}
	}
}
