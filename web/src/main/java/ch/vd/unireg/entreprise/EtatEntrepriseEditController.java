package ch.vd.unireg.entreprise;

import javax.validation.Valid;
import java.util.List;

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

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/entreprise/etats")
public class EtatEntrepriseEditController {

	private static final String TRANSITIONS_DISPONIBLES = "transitionDisponibles";
	private static final String PREVIOUS_DATE_AFTER_DATE_OBTENTION = "previousDateAfterDateObtention";

	private TiersDAO tiersDAO;
	private TiersMapHelper tiersMapHelper;
	private TiersService tiersService;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;
	private EntrepriseService entrepriseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEntrepriseService(EntrepriseService entrepriseService) {
		this.entrepriseService = entrepriseService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	private static class EtatsEditValidator extends DelegatingValidator {
		private EtatsEditValidator() {
			addSubValidator(EntrepriseView.class, new DummyValidator<>(EntrepriseView.class));
			addSubValidator(AddEtatEntrepriseView.class, new AddEtatEntrepriseViewValidator());
		}
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new EtatsEditValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	private Autorisations getAutorisations(Tiers tiers) {
		return autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editEntreprise(Model model, @RequestParam(value = "id") long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isEtatsPM()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des états d'entreprise.");
			}
			final EntrepriseView view = entrepriseService.getEntreprise((Entreprise) tiers);
			return showEditEtats(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private String showEditEtats(Model model, long id, EntrepriseView view) {
		model.addAttribute("command", view);
		model.addAttribute("tiersId", id);
		return "/tiers/edition/etats/edit-etats";
	}


	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addEtatEntreprise(@RequestParam(value = "tiersId", required = true) long tiersId,
	                                @RequestParam(value = "date", required = false) RegDate date,
	                                @RequestParam(value = "type", required = false) TypeEtatEntreprise type,
	                                Model model) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isEtatsPM()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création d'états d'entreprise.");
		}

		final AddEtatEntrepriseView view = new AddEtatEntrepriseView(entreprise);
		view.setDateObtention(date != null ? date : RegDate.get()); // Pré-remplissage à la date du jour si date vide.
		view.setType(type);
		model.addAttribute("command", view);
		final List<TypeEtatEntreprise> transitionDisponibles =
				tiersService.getTransitionsEtatEntrepriseDisponibles(entreprise, view.getDateObtention(), TypeGenerationEtatEntreprise.MANUELLE);

		model.addAttribute(TRANSITIONS_DISPONIBLES, tiersMapHelper.getMapForTypeEtatEntreprise(transitionDisponibles));
		model.addAttribute(PREVIOUS_DATE_AFTER_DATE_OBTENTION, RegDateHelper.isAfter(view.getPreviousDate(), view.getDateObtention(), NullDateBehavior.EARLIEST));
		return "etats/add-etat-entreprise";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String addEtatEntreprise(@Valid @ModelAttribute("command") final AddEtatEntrepriseView view, BindingResult result, Model model) throws Exception {

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isEtatsPM()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création d'états d'entreprise.");
		}


		if (result.hasErrors()) {
			final RegDate date = view.getDateObtention() != null ? view.getDateObtention() : RegDate.get();
			final EtatEntreprise etatActuel = entreprise.getEtatActuel();
			view.setPreviousDate(etatActuel.getDateObtention());
			view.setPreviousType(etatActuel.getType());
			final List<TypeEtatEntreprise> transitionDisponibles =
					tiersService.getTransitionsEtatEntrepriseDisponibles(entreprise, date, TypeGenerationEtatEntreprise.MANUELLE);

			model.addAttribute("command", view);
			model.addAttribute(TRANSITIONS_DISPONIBLES, tiersMapHelper.getMapForTypeEtatEntreprise(transitionDisponibles));
			model.addAttribute(PREVIOUS_DATE_AFTER_DATE_OBTENTION, RegDateHelper.isAfter(view.getPreviousDate(), view.getDateObtention(), NullDateBehavior.EARLIEST));
			return "etats/add-etat-entreprise";
		}

		tiersService.changeEtatEntreprise(view.getType(), entreprise, view.getDateObtention(), TypeGenerationEtatEntreprise.MANUELLE);

		return "redirect:/entreprise/etats/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancelEtatEntreprise(long etatEntrepriseId) throws Exception {

		final EtatEntreprise etatEntreprise = hibernateTemplate.get(EtatEntreprise.class, etatEntrepriseId);
		if (etatEntreprise == null) {
			throw new ObjectNotFoundException("L'état entreprise n°" + etatEntrepriseId + " n'existe pas.");
		}
		final Entreprise entreprise = etatEntreprise.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isEtatsPM()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression d'états d'entreprise.");
		}

		tiersService.annuleEtatEntreprise(etatEntreprise);

		return "redirect:/entreprise/etats/edit.do?id=" + entreprise.getId();// + "&highlightFor=" + etatEntreprise.getId();
	}

}
