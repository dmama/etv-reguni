package ch.vd.uniregctb.common;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.annulation.couple.AnnulationCoupleListController;
import ch.vd.uniregctb.annulation.separation.AnnulationSeparationListController;
import ch.vd.uniregctb.contribuableAssocie.ContribuableAssocieListController;
import ch.vd.uniregctb.fusion.HabitantListController;
import ch.vd.uniregctb.fusion.NonHabitantListController;
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

	@RequestMapping(value = "/contribuable-associe/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheContribuableAssocie(HttpSession session, @RequestParam("numeroDpi") long dpi) {
		session.removeAttribute(ContribuableAssocieListController.CONTRIBUABLE_ASSOCIE_CRITERIA_NAME);
		return String.format("redirect:/contribuable-associe/list.do?numeroDpi=%d", dpi);
	}

	@RequestMapping(value = "/deces/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDeces() {
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
	public String effacerCriteresRechercheRapport(HttpSession session, @RequestParam("numero") long id) {
		session.removeAttribute(RapportListController.TIERS_LIE_CRITERIA_NAME);
		return String.format("redirect:/rapport/search.do?numero=%d", id);
	}

	@RequestMapping(value = "/rt/debiteur/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDebiteurPourRT(HttpSession session, @RequestParam("numeroSrc") long numeroSrc) {
		session.removeAttribute(DebiteurListController.DEBITEUR_CRITERIA_NAME);
		return String.format("redirect:/rt/list-debiteur.do?numeroSrc=%d", numeroSrc);
	}

	@RequestMapping(value = "/rt/sourcier/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheSourcierPourRT(HttpSession session, @RequestParam("numeroDpi") long numeroDpi) {
		session.removeAttribute(SourcierListController.SOURCIER_CRITERIA_NAME);
		return String.format("redirect:/rt/list-sourcier.do?numeroDpi=%d", numeroDpi);
	}

}
