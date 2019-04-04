package ch.vd.unireg.annonceIDE;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.NonUniqueResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.PaginationHelper;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.ParamSorting;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDEDAO;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.utils.CantonalIdEditor;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.TiersNumberEditor;

/**
 * Contrôleur qui permet le suivi des annonces à l'IDE.
 */
@Controller
@RequestMapping(value = "/annonceIDE")
public class AnnonceIDEController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEController.class);

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits de suivi des annonces à l'IDE";

	private static final String DEFAULT_FIELD = "noticeRequestId";
	private static final String TABLE_NAME = "annonce";

	private TiersMapHelper tiersMapHelper;
	private ServiceEntreprise serviceEntreprise;
	private TiersService tiersService;
	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	private EvenementEntrepriseDAO evtEntrepriseDAO;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}

	public void setEvtEntrepriseDAO(EvenementEntrepriseDAO evtEntrepriseDAO) {
		this.evtEntrepriseDAO = evtEntrepriseDAO;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(Long.class, "cantonalId", new CantonalIdEditor());
		binder.registerCustomEditor(Long.class, "tiersId", new TiersNumberEditor(true));
	}

	/**
	 * Affiche l'écran de suivi des annonces. L'écran de suivi contient un formulaire de recherche et des résultats paginés.
	 */
	@SecurityCheck(rolesToCheck = {Role.SUIVI_ANNONCES_IDE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/find.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String find(@ModelAttribute(value = "view") AnnonceIDEQueryView view, BindingResult bindingResult, HttpServletRequest request, Model model) {

		model.addAttribute("noticeTypes", tiersMapHelper.getTypeAnnonce());
		model.addAttribute("noticeStatuts", tiersMapHelper.getStatutAnnonce());

		if (bindingResult.hasErrors()) {
			model.addAttribute("page", new PageImpl<>(Collections.<AnnonceIDEView>emptyList()));
			model.addAttribute("totalElements", 0);
			return "annonceIDE/find";
		}

		// on interpète la requête
		final int pageSize = view.getResultsPerPage() == 0 ? 10 : view.getResultsPerPage();
		final ParamPagination pagination = new WebParamPagination(request, TABLE_NAME, pageSize, DEFAULT_FIELD, false);

		final int pageNumber = pagination.getNumeroPage() - 1;
		final ParamSorting sorting = pagination.getSorting();
		final Sort.Order order = new Sort.Order(sorting.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, sorting.getField());

		// on effectue la recherche
		final Page<AnnonceIDE> annonces;
		try {
			final List<AnnonceIDE> listeAnnonces = new ArrayList<>();
			final Long tiersId = view.getTiersId();
			if (tiersId != null) {
				final Tiers tiers = tiersService.getTiers(tiersId);
				if (tiers != null) {
					Long tiersIdToUse;
					Long noCantonalToUse;
					if (tiers instanceof Entreprise) {
						final Etablissement etablissementPrincipalActuel = tiersService.getEtablissementPrincipal((Entreprise) tiers, RegDate.get());
						tiersIdToUse = etablissementPrincipalActuel.getNumero();
						noCantonalToUse = etablissementPrincipalActuel.getNumeroEtablissement();
					}
					else {
						tiersIdToUse = tiersId;
						if (tiers instanceof Etablissement) {
							noCantonalToUse = ((Etablissement)tiers).getNumeroEtablissement();
						} else {
							noCantonalToUse = null;
						}
					}
					final List<ReferenceAnnonceIDE> referencesAnnonceIDE = referenceAnnonceIDEDAO.getReferencesAnnonceIDE(tiersIdToUse);

					Map<String, AnnonceIDE> map = new HashMap<>();
					if (referencesAnnonceIDE != null && !referencesAnnonceIDE.isEmpty()) {
						for (ReferenceAnnonceIDE ref : referencesAnnonceIDE) {
							final AnnonceIDE annonceIDE = serviceEntreprise.getAnnonceIDE(ref.getId(), RCEntAnnonceIDEHelper.UNIREG_USER);
							if (annonceIDE != null) {
								map.put(annonceIDE.getUniqueKey(), annonceIDE);
							}
						}
					}

					if (noCantonalToUse != null) {
						final AnnonceIDEQuery query = new AnnonceIDEQuery();
						query.setCantonalId(noCantonalToUse);
						if (StringUtils.isEmpty(query.getUserId())) {
							query.setUserId(RCEntAnnonceIDEHelper.UNIREG_USER);
						}
						final List<AnnonceIDE> annoncesPourEtablissement = serviceEntreprise.findAnnoncesIDE(query, null, 0, 1000).getContent();
						for (AnnonceIDE annonceIDE : annoncesPourEtablissement) {
							map.put(annonceIDE.getUniqueKey(), annonceIDE);
						}
					}

					listeAnnonces.addAll(map.values());
					listeAnnonces.sort(Comparator.comparingLong(AnnonceIDE::getNumero));
				}
				final PageRequest pageable = new PageRequest(pageNumber, pageSize, null);
				annonces = new PageImpl<>(listeAnnonces, pageable, listeAnnonces.size());
			}
			else {
				annonces = serviceEntreprise.findAnnoncesIDE(view.toQuery(), order, pageNumber, pageSize);
			}
		}
		catch (ServiceEntrepriseException e) {
			LOGGER.warn("Erreur lors de la recherche de demandes à l'IDE", e);
			Flash.warning("L'appel à RCEnt a levé l'erreur suivante : " + e.getMessage() + ". Veuillez réessayer plus tard.");
			model.addAttribute("page", new PageImpl<>(Collections.<AnnonceIDEView>emptyList()));
			model.addAttribute("totalElements", 0);
			return "annonceIDE/find";
		}

		// on adapte les annonces
		final List<AnnonceIDEView> content = new ArrayList<>(annonces.getNumberOfElements());
		final List<String> messages = new ArrayList<>();
		for (AnnonceIDE annonce : annonces) {
			// On récupère les informations de tiers entreprise et de d'événement RCEnt en retour, si disponible pour le dernier.
			final Date dateAnnonce = annonce.getDateAnnonce();
			final AnnonceIDEView annonceView = new AnnonceIDEView(annonce);

			final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.get(annonce.getNumero());
			if (referenceAnnonceIDE != null) {
				final Entreprise entreprise = tiersService.getEntreprise(referenceAnnonceIDE.getEtablissement(), RegDateHelper.get(dateAnnonce));

				if (entreprise != null) {
					annonceView.setNumeroTiersEntreprise(entreprise.getNumero());
				}
				try {
					final EvenementEntreprise evenementEntreprise = evtEntrepriseDAO.getEvenementForNoAnnonceIDE(annonce.getNumero());
					if (evenementEntreprise != null) {
						annonceView.setNoEvtOrganisation(evenementEntreprise.getNoEvenement());
						annonceView.setIdEvtOrganisation(evenementEntreprise.getId());
					}
				}
				catch (NonUniqueResultException e) {
					final String message = String.format("Plusieurs événements RCEnt sont associés à l'annonce à l'IDE n°%d. Impossible d'afficher le numéro d'événement.", annonce.getNumero());
					Audit.error(message);
					messages.add(message);
				}
			}
			content.add(annonceView);
		}
		if (!messages.isEmpty()) {
			Flash.warning(String.join("\n", messages));
		}

		// on renseigne le modèle
		final Page<AnnonceIDEView> page = PaginationHelper.buildPage(content, pageNumber, pageSize, annonces.getTotalElements(), new Sort(order));
		model.addAttribute("page", page);
		model.addAttribute("totalElements", (int) page.getTotalElements());

		return "annonceIDE/find";
	}

	/**
	 * Affiche les détails d'une annonce.
	 */
	@SecurityCheck(rolesToCheck = {Role.SUIVI_ANNONCES_IDE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	public String visu(@RequestParam Long id, @RequestParam(required = false) String userId, Model model) {

		if (StringUtils.isBlank(userId)) {
			userId = RCEntAnnonceIDEHelper.UNIREG_USER;
		}

		// on effectue la recherche
		final AnnonceIDEEnvoyee annonce = serviceEntreprise.getAnnonceIDE(id, userId);
		if (annonce == null) {
			throw new ObjectNotFoundException("Aucune demande ne correspond à l'identifiant " + id);
		}

		// on adapte les annonces
		final AnnonceIDEView view = new AnnonceIDEView(annonce);
		model.addAttribute("annonce", view);

		return "annonceIDE/visu";
	}
}
