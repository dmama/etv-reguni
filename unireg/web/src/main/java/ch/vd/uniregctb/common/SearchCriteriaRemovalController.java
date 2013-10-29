package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.uniregctb.activation.TiersActivationListController;
import ch.vd.uniregctb.annulation.couple.AnnulationCoupleListController;
import ch.vd.uniregctb.annulation.separation.AnnulationSeparationListController;
import ch.vd.uniregctb.contribuableAssocie.ContribuableAssocieListController;
import ch.vd.uniregctb.deces.DecesListController;
import ch.vd.uniregctb.fusion.HabitantListController;
import ch.vd.uniregctb.fusion.NonHabitantListController;
import ch.vd.uniregctb.identification.contribuable.IdentificationMessagesEditController;
import ch.vd.uniregctb.rapport.RapportListController;
import ch.vd.uniregctb.rt.DebiteurListController;
import ch.vd.uniregctb.rt.SourcierListController;
import ch.vd.uniregctb.separation.SeparationListController;

/**
 * Contrôleur qui regroupe les actions /reset-search.do des contrôleurs pas encore passés en Spring 3+
 */
@Controller
public class SearchCriteriaRemovalController {

	@RequestMapping(value = "/separation/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheSeparation(HttpSession session) {
		session.removeAttribute(SeparationListController.SEPARATION_CRITERIA_NAME);
		return "redirect:/separation/list.do";
	}

	@RequestMapping(value = "/activation/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheActivation(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(TiersActivationListController.ACTIVATION_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/annulation/couple/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheAnnulationCouple(HttpSession session) {
		session.removeAttribute(AnnulationCoupleListController.ANNULATION_COUPLE_CRITERIA_NAME);
		return "redirect:/annulation/couple/list.do";
	}

	@RequestMapping(value = "/annulation/separation/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheAnnulationSeparation(HttpSession session) {
		session.removeAttribute(AnnulationSeparationListController.ANNULATION_SEPARATION_CRITERIA_NAME);
		return "redirect:/annulation/separation/list.do";
	}

	@RequestMapping(value = "/identification/gestion-messages/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheIdentification(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(IdentificationMessagesEditController.PP_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/contribuable-associe/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheContribuableAssocie(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(ContribuableAssocieListController.CONTRIBUABLE_ASSOCIE_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/deces/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDeces(HttpSession session) {
		session.removeAttribute(DecesListController.DECES_CRITERIA_NAME);
		return "redirect:/deces/list.do";
	}

	@RequestMapping(value = "/fusion/habitant/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheFusionHabitant(HttpSession session) {
		session.removeAttribute(HabitantListController.HABITANT_CRITERIA_NAME);
		return "redirect:/fusion/list-habitant.do";
	}

	@RequestMapping(value = "/fusion/nonhabitant/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheFusionNonHabitant(HttpSession session) {
		session.removeAttribute(NonHabitantListController.NON_HABITANT_CRITERIA_NAME);
		return "redirect:/fusion/list-non-habitant.do";
	}

	@RequestMapping(value = "/rapport/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheRapport(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(RapportListController.TIERS_LIE_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/rt/debiteur/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDebiteurPourRT(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(DebiteurListController.DEBITEUR_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/rt/sourcier/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheSourcierPourRT(HttpSession session, HttpServletRequest request) {
		session.removeAttribute(SourcierListController.SOURCIER_CRITERIA_NAME);
		return HttpHelper.getRedirectPagePrecedente(request);
	}

}
