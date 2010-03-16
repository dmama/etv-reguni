package ch.vd.uniregctb.couple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class ContribuableListController extends AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(ContribuableListController.class);

	public static final String CTB_CRITERIA_NAME = "ctbCriteria";

	private static final String NUMERO_PREMIER_PP_PARAMETER_NAME = "numeroPP1";
	private static final String NUMERO_SECOND_PP_PARAMETER_NAME = "numeroPP2";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		TracePoint tp = TracingManager.begin();
		LOGGER.debug("Start of ContribuableListController:formBackingObject");

		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		TiersCriteriaView criteriaView = (TiersCriteriaView) session.getAttribute(CTB_CRITERIA_NAME);

		if (criteriaView == null || ((action != null) && action.equals(EFFACER_PARAMETER_VALUE))) {
			criteriaView = new TiersCriteriaView();
			criteriaView.setTypeRechercheDuNom(TiersCriteriaView.TypeRecherche.EST_EXACTEMENT);
			criteriaView.setTypeTiers(TiersCriteriaView.TypeTiers.NON_HABITANT_OU_MENAGE_COMMUN);
			criteriaView.setForPrincipalActif(true);
			
			String numeroPP1Param = request.getParameter(NUMERO_PREMIER_PP_PARAMETER_NAME);
			if (numeroPP1Param != null) {
				Long numeroPP1 = Long.parseLong(numeroPP1Param);
				checkAccesDossierEnLecture(numeroPP1);
				criteriaView.setNumeroPremierePersonne(numeroPP1);
			}
			String numeroPP2Param = request.getParameter(NUMERO_SECOND_PP_PARAMETER_NAME);
			Long numeroPP2 = null;
			if (numeroPP2Param != null) {
				numeroPP2 = Long.parseLong(numeroPP2Param);
				checkAccesDossierEnLecture(numeroPP2);
				criteriaView.setNumeroSecondePersonne(numeroPP2);
			}
		}

		TracingManager.end(tp);
		return criteriaView;

	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		HttpSession session = request.getSession();
		TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(CTB_CRITERIA_NAME);
		ModelAndView mav = super.showForm(request, response, errors, model);

		if (errors.getErrorCount() == 0) {
			String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
			if(buttonEffacer == null) {
				LOGGER.debug("Affichage du formulaire de recherche...");
				if (bean != null && !bean.isEmpty()) {
					LOGGER.debug("Critères de recherche=" + bean);
					try {
						List<TiersIndexedData> results = service.search(bean);
						List<TiersIndexedData> filtredResults = new ArrayList<TiersIndexedData>();
						for (Iterator<TiersIndexedData> itResults = results.iterator(); itResults.hasNext(); ) {
							TiersIndexedData tiersIndexedData = itResults.next();
							Contribuable contribuable = (Contribuable) service.getTiers(tiersIndexedData.getNumero());
							if (contribuable instanceof PersonnePhysique) {
								PersonnePhysique nonHabitant = (PersonnePhysique) contribuable;
								// seulement les contribuables ouverts et indéterminés doivent être affichés
								if (nonHabitant.getSexe() == null && nonHabitant.getSituationFamilleActive() == null && nonHabitant.getForFiscalPrincipalAt(null) != null) {
									filtredResults.add(tiersIndexedData);
								}
							}
							else if (contribuable instanceof MenageCommun) {
								MenageCommun menage = (MenageCommun) contribuable;
								// seulement les ménages communs annulés du ou des contribuables doivent être affichés
								for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
									if (rapport.isAnnule() &&
									   (rapport.getSujet().getNumero().equals(bean.getNumeroPremierePersonne()) ||
										rapport.getSujet().getNumero().equals(bean.getNumeroSecondePersonne())) &&
									   /* pour pas repeter 2 fois le même ménage */
									   !filtredResults.contains(tiersIndexedData))  {
										filtredResults.add(tiersIndexedData);
									}
								}
							}
						}
						mav.addObject(TIERS_LIST_ATTRIBUTE_NAME, filtredResults);
					}
					catch (TooManyResultsIndexerException ee) {
						LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
						errors.reject("error.preciser.recherche");
					}
					catch (IndexerException e) {
						LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
						errors.reject("error.recherche");
					}
				}
			}
		}
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		LOGGER.debug("Traitement du formulaire...");
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		TiersCriteriaView bean = (TiersCriteriaView) command;
		HttpSession session = request.getSession();
		session.setAttribute(CTB_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView(getSuccessView() + "?action=effacer"));
		} else {
			mav.setView(new RedirectView(getSuccessView()));
		}

		return mav;
	}

	/**
	 * Appelé depuis d'autres controlleurs pour effacer les critères de recherche utilisés
	 * lors de la recherche d'un contribuable non-habitant pour la constitution d'un couple
	 */
	public static void effaceCriteresRecherche(HttpServletRequest request) {
		removeModuleFromSession(request, CTB_CRITERIA_NAME);
	}
}
