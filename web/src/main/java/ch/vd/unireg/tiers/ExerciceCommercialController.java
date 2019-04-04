package ch.vd.unireg.tiers;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
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
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.bouclement.BouclementException;
import ch.vd.unireg.metier.bouclement.BouclementService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.validator.ChangeDateExerciceCommercialViewValidator;
import ch.vd.unireg.tiers.view.ChangeDateExerciceCommercialView;
import ch.vd.unireg.tiers.view.ExerciceCommercialView;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/exercices")
public class ExerciceCommercialController {

	private TiersDAO tiersDAO;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private ParametreAppService parametreAppService;
	private BouclementService bouclementService;
	private HibernateTemplate hibernateTemplate;
	private ExerciceCommercialHelper exerciceCommercialHelper;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setBouclementService(BouclementService bouclementService) {
		this.bouclementService = bouclementService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		this.exerciceCommercialHelper = exerciceCommercialHelper;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new ChangeDateExerciceCommercialViewValidator());
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@ResponseBody
	public List<ExerciceCommercialView> getExercicesCommerciaux(@RequestParam("pmId") long pmId,
	                                                            @RequestParam(value = "reversed", required = false, defaultValue = "false") boolean reversed) {
		final Tiers tiers = tiersDAO.get(pmId);
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + pmId);
		}
		controllerUtils.checkAccesDossierEnLecture(pmId);

		// constitution de la liste
		return getViewExercicesCommerciaux((Entreprise) tiers, reversed);
	}

	private List<DeclarationImpotOrdinairePM> getDeclarationsNonAnnuleesTriees(Entreprise entreprise) {
		return entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false);
	}

	private List<ExerciceCommercialView> getViewExercicesCommerciaux(Entreprise entreprise, boolean reversed) {
		return getViewExercicesCommerciauxCalculees(entreprise, reversed);
	}
	
	@NotNull
	private List<ExerciceCommercialView> getViewExercicesCommerciauxCalculees(Entreprise entreprise, boolean reversed) {
		final List<DeclarationImpotOrdinairePM> dis = getDeclarationsNonAnnuleesTriees(entreprise);
		final List<ExerciceCommercial> liste = exerciceCommercialHelper.getExercicesCommerciauxExposables(entreprise);
		final List<ExerciceCommercialView> views = new ArrayList<>(liste.size());
		boolean first = true;
		for (ExerciceCommercial exercice : liste) {
			final boolean tooOldToHaveDI = exercice.getDateFin().year() < parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
			final boolean withDI = !tooOldToHaveDI && DateRangeHelper.intersect(exercice, dis);
			views.add(new ExerciceCommercialView(exercice, first, withDI, tooOldToHaveDI, entreprise.hasDateDebutPremierExercice(), entreprise.hasBouclements()));
			first = false;
		}

		// dans quel sens les données doivent-elles être triées ?
		if (reversed) {
			views.sort(Collections.reverseOrder(new DateRangeComparator<>()));
		}
		return views;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String showEditBouclements(@RequestParam("pmId") long pmId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(pmId);

		final Tiers tiers = tiersDAO.get(pmId);
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + pmId);
		}

		final Entreprise entreprise = (Entreprise) tiers;
		final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);

		model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux, entreprise));
		model.addAttribute("exercices", viewExercicesCommerciaux);
		model.addAttribute("command", new ChangeDateExerciceCommercialView(pmId));
		model.addAttribute("mode", "no-edit");
		return "tiers/edition/pm/bouclements/list";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ajoute-nouvelle-date-debut.do", method = RequestMethod.POST)
	public String ajouteNouvelleDateDebut(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux,entreprise));
			model.addAttribute("command", view);
			model.addAttribute("mode", "add-dd");
			return "tiers/edition/pm/bouclements/list";
		}

		// on corrige la date de début
		try {
			bouclementService.corrigeDateDebutPremierExerciceCommercial(entreprise, view.getNouvelleDate());
		}
		catch (BouclementException  e) {
			throw new ActionException(e.getMessage());
		}

		// forçons une validation du tout
		hibernateTemplate.flush();

		// si on est ici, c'est que la validation s'est bien passée
		Flash.message(String.format("Date de premier exercice commercial ajoutée au %s.", RegDateHelper.dateToDisplayString(view.getNouvelleDate())), 4000);
		return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/change-date-debut.do", method = RequestMethod.POST)
	public String changeDateDebut(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux,entreprise));
			model.addAttribute("command", view);
			model.addAttribute("mode", "edit-dd");
			return "tiers/edition/pm/bouclements/list";
		}

		// pas de changement -> pas de changement
		if (view.getAncienneDate() == view.getNouvelleDate()) {
			Flash.message("Aucun changement apporté.", 4000);
			return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
		}

		// on renseigne la date de début
		try {
			bouclementService.setDateDebutPremierExerciceCommercial(entreprise, view.getNouvelleDate());
		}
		catch (BouclementException  e) {
			throw new ActionException(e.getMessage());
		}

		// on force la validation maintenant avec un flush
		hibernateTemplate.flush();

		// si on est ici, c'est que la validation s'est bien passée
		Flash.message(String.format("Date de début du premier exercice commercial déplacée %s au %s.", RegDateHelper.dateToDisplayString(view.getAncienneDate()), RegDateHelper.dateToDisplayString(view.getNouvelleDate())), 4000);
		return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/change-date-fin.do", method = RequestMethod.POST)
	public String changeDateFin(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux,entreprise));
			model.addAttribute("command", view);
			model.addAttribute("mode", "edit-df");
			model.addAttribute("mode_edit_df", view.getAncienneDate() == null ? "" : view.getAncienneDate().index());
			return "tiers/edition/pm/bouclements/list";
		}

		final RegDate ancienneDate = view.getAncienneDate();
		final RegDate nouvelleDate = view.getNouvelleDate();

		// pas de changement -> pas de changement
		if (ancienneDate == nouvelleDate) {
			Flash.message("Aucun changement apporté.", 4000);
			return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
		}

		// on change la date de fin
		try {
			bouclementService.changeDateFinBouclement(entreprise, ancienneDate, nouvelleDate);
		}
		catch (BouclementException  e) {
			throw new ActionException(e.getMessage());
		}

		// forçons une validation du tout
		hibernateTemplate.flush();

		// si on est ici, c'est que la validation s'est bien passée
		Flash.message(String.format("Bouclement du %s déplacé au %s.", RegDateHelper.dateToDisplayString(ancienneDate), RegDateHelper.dateToDisplayString(nouvelleDate)), 4000);
		return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
	}

	private boolean isDateDebutAjoutable(List<ExerciceCommercialView> viewExercicesCommerciaux,Entreprise entreprise) {

		if (!entreprise.hasDateDebutPremierExercice()) {
			return true;
		}
		boolean dateDebutAjoutable = false;
		if (!viewExercicesCommerciaux.isEmpty()) {
			dateDebutAjoutable = true;
			// Cas spécial du 1er exercice commercial (de plus d'une année)
			// On ne peut pas ajouter une nouvelle date de début d'exercice commercial
			// s'il n'existe pas au moins une date de bouclement par année civile
			ExerciceCommercialView premierEC = viewExercicesCommerciaux.get(viewExercicesCommerciaux.size()-1);
			if (premierEC.getDateFin()!=null && premierEC.getDateDebut().getOneDayBefore().year() < premierEC.getDateFin().year() - 1) {
				dateDebutAjoutable = false;
			}
		}
		return dateDebutAjoutable;
	}
}
