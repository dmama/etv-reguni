package ch.vd.uniregctb.annonceIDE;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamSorting;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * Contrôleur qui permet le suivi des annonces à l'IDE.
 */
@Controller
@RequestMapping(value = "/annonceIDE")
public class AnnonceIDEController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec de suivi des annonces à l'IDE";

	private TiersMapHelper tiersMapHelper;
	private ServiceOrganisationService organisationService;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setOrganisationService(ServiceOrganisationService organisationService) {
		this.organisationService = organisationService;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	/**
	 * Affiche l'écran de suivi des annonces. L'écran de suivi contient un formulaire de recherche et des résultats paginés.
	 */
	@SecurityCheck(rolesToCheck = {Role.SUIVI_ANNONCES_IDE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/find.do", method = RequestMethod.GET)
	public String find(@ModelAttribute(value = "view") AnnonceIDEQueryView view, HttpServletRequest request, Model model) {

		// on interpète la requête
		final int pageSize = view.getResultsPerPage() == 0 ? 10 : view.getResultsPerPage();
		final WebParamPagination pagination = new WebParamPagination(request, "annonce", pageSize);
		final int pageNumber = pagination.getNumeroPage() - 1;
		final ParamSorting sorting = pagination.getSorting();
		final Sort.Order order = StringUtils.isBlank(sorting.getField()) ? null :
				new Sort.Order(sorting.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, sorting.getField());

		// on effectue la recherche
		final Page<AnnonceIDE> annonces = organisationService.findAnnoncesIDE(view.toQuery(), order, pageNumber, pageSize);

		// on adapte les annonces
		final List<AnnonceIDEView> content = new ArrayList<>(annonces.getNumberOfElements());
		for (AnnonceIDE annonce : annonces) {
			content.add(new AnnonceIDEView(annonce));
		}

		// on renseigne le modèle
		final PageRequest pageable = new PageRequest(pageNumber, pageSize, order == null ? null : new Sort(order));
		final Page<AnnonceIDEView> page = new PageImpl<>(content, pageable, annonces.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("totalElements", (int) page.getTotalElements());
		model.addAttribute("noticeTypes", tiersMapHelper.getTypeAnnonce());
		model.addAttribute("noticeStatuts", tiersMapHelper.getStatutAnnonce());

		return "annonceIDE/find";
	}

	/**
	 * Affiche les détails d'une annonce.
	 */
	@SecurityCheck(rolesToCheck = {Role.SUIVI_ANNONCES_IDE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	public String visu(@RequestParam Long id, Model model) {

		// on effectue la recherche
		final AnnonceIDEEnvoyee annonce = organisationService.getAnnonceIDE(id);
		if (annonce == null) {
			throw new ObjectNotFoundException("Aucune demande ne correspond à l'identifiant " + id);
		}

		// on adapte les annonces
		final AnnonceIDEView view = new AnnonceIDEView(annonce);
		model.addAttribute("annonce", view);

		return "annonceIDE/visu";
	}
}
