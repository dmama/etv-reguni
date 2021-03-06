package ch.vd.unireg.common;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur qui regroupe les actions /reset-search.do des contrôleurs pas encore passés en Spring 3+
 */
@Controller
public class SearchCriteriaRemovalController {

	@RequestMapping(value = "/contribuable-associe/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheContribuableAssocie(HttpSession session, @RequestParam("numeroDpi") long dpi) {
		return String.format("redirect:/contribuable-associe/list.do?numeroDpi=%d", dpi);
	}

	@RequestMapping(value = "/deces/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDeces() {
		return "redirect:/deces/list.do";
	}

	@RequestMapping(value = "/rapport/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheRapport(@RequestParam("tiersId") long id) {
		return String.format("redirect:/rapport/add-search.do?tiersId=%d", id);
	}

	@RequestMapping(value = "/rt/debiteur/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheDebiteurPourRT(HttpSession session, @RequestParam("numeroSrc") long numeroSrc) {
		return String.format("redirect:/rapports-prestation/search-debiteur.do?numeroSourcier=%d", numeroSrc);
	}

	@RequestMapping(value = "/rt/sourcier/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheSourcierPourRT(HttpSession session, @RequestParam("numeroDpi") long numeroDpi) {
		return String.format("redirect:/rapports-prestation/search-sourcier.do?numeroDebiteur=%d", numeroDpi);
	}

}
