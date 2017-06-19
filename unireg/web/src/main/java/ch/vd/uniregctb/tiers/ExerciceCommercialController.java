package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.BouclementHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.validator.ChangeDateExerciceCommercialViewValidator;
import ch.vd.uniregctb.tiers.view.ChangeDateExerciceCommercialView;
import ch.vd.uniregctb.tiers.view.ExerciceCommercialView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/exercices")
public class ExerciceCommercialController {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private ParametreAppService parametreAppService;
	private BouclementService bouclementService;
	private HibernateTemplate hibernateTemplate;
	private ExerciceCommercialHelper exerciceCommercialHelper;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

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
		if (tiers == null || !(tiers instanceof Entreprise)) {
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
		final List<DeclarationImpotOrdinairePM> dis = getDeclarationsNonAnnuleesTriees(entreprise);
		final List<ExerciceCommercial> liste = exerciceCommercialHelper.getExercicesCommerciauxExposables(entreprise);
		final List<ExerciceCommercialView> views = new ArrayList<>(liste.size());
		boolean first = true;
		for (ExerciceCommercial exercice : liste) {
			final boolean tooOldToHaveDI = exercice.getDateFin().year() < parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
			final boolean withDI = !tooOldToHaveDI && DateRangeHelper.intersect(exercice, dis);
			views.add(new ExerciceCommercialView(exercice, first, withDI, tooOldToHaveDI));
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
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(pmId);

		final Tiers tiers = tiersDAO.get(pmId);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + pmId);
		}

		final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux((Entreprise) tiers, true);

		model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux));
		model.addAttribute("exercices", viewExercicesCommerciaux);
		model.addAttribute("command", new ChangeDateExerciceCommercialView(pmId));
		model.addAttribute("mode", "no-edit");
		return "tiers/edition/pm/bouclements/list";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ajoute-nouvelle-date-debut.do", method = RequestMethod.POST)
	public String ajouteNouvelleDateDebut(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux));
			model.addAttribute("command", view);
			model.addAttribute("mode", "add-dd");
			return "tiers/edition/pm/bouclements/list";
		}

		// 1. récupération des anciennes dates de bouclement
		final Set<Bouclement> bouclements = entreprise.getBouclements();

		// 2. remplacement de l'ancienne date par la nouvelle et impact jusqu'à la prochaine période fixée
		final List<ExerciceCommercial> anciensExercicesCommerciaux = tiersService.getExercicesCommerciaux(entreprise);
		ExerciceCommercial premierExerciceCommercial = anciensExercicesCommerciaux.get(0);
		RegDate anciennePremiereDateDebutExerciceCommercial = null;
		if (premierExerciceCommercial != null) {
			anciennePremiereDateDebutExerciceCommercial = premierExerciceCommercial.getDateDebut();
			if (view.getNouvelleDate().isAfterOrEqual(premierExerciceCommercial.getDateDebut())) {
				throw new ActionException(String.format("La nouvelle date de début (%s) doit être inférieure à celle du premier exercice commercial.",
				                                        RegDateHelper.dateToDisplayString(view.getNouvelleDate())));
			}
		}

		entreprise.setDateDebutPremierExerciceCommercial(view.getNouvelleDate());

		final SortedSet<RegDate> nouvellesDatesBouclement = new TreeSet<>();
		// Ajout des exercices commerciaux existants
		for (ExerciceCommercial exercice : anciensExercicesCommerciaux) {
			nouvellesDatesBouclement.add(exercice.getDateFin());
		}
		// Ajout des nouvelles dates de bouclement depuis la nouvelle date de début
		if (anciennePremiereDateDebutExerciceCommercial != null) {
			for (RegDate date = view.getNouvelleDate(); date.compareTo(anciennePremiereDateDebutExerciceCommercial) < 0; date = date.addYears(1)) {
				RegDate dateFinBouclement = date.addYears(1).getOneDayBefore();
				if (dateFinBouclement.isAfterOrEqual(anciennePremiereDateDebutExerciceCommercial)) {
					dateFinBouclement = anciennePremiereDateDebutExerciceCommercial.getOneDayBefore();
				}
				nouvellesDatesBouclement.add(dateFinBouclement);
			}
		}

		// 3. re-calcul des cycles
		final List<Bouclement> nouveauxBouclements = bouclementService.extractBouclementsDepuisDates(nouvellesDatesBouclement, 12);

		// 4. comparaison avant/après et application des différences
		BouclementHelper.resetBouclements(entreprise, nouveauxBouclements);

		// 5. contrôle final des dates de bouclements
		controleDatesBouclements(view, anciensExercicesCommerciaux, nouvellesDatesBouclement);

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
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux));
			model.addAttribute("command", view);
			model.addAttribute("mode", "edit-dd");
			return "tiers/edition/pm/bouclements/list";
		}

		// pas de changement -> pas de changement
		if (view.getAncienneDate() == view.getNouvelleDate()) {
			Flash.message("Aucun changement apporté.", 4000);
			return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
		}

		// la date de début du premier exercice commercial ne doit pas s'écarter trop de la date de fin de l'exercice
		// (comme c'est le premier, on accorde un peu plus...)
		final List<ExerciceCommercial> anciensExercicesCommerciaux = tiersService.getExercicesCommerciaux(entreprise);
		final int anneePremierBouclement = anciensExercicesCommerciaux.get(0).getDateFin().year();
		if (anneePremierBouclement - view.getNouvelleDate().year() > 1) {
			throw new ActionException(String.format("Impossible de déplacer la date de début du premier exercice commercial du %s au %s car celui-ci s'étendrait alors sur plus de deux années civiles.",
			                                        RegDateHelper.dateToDisplayString(view.getAncienneDate()),
			                                        RegDateHelper.dateToDisplayString(view.getNouvelleDate())));
		}

		// les autres problèmes seront détectés à la validation de l'entreprise...
		entreprise.setDateDebutPremierExerciceCommercial(view.getNouvelleDate());

		// ... validation que l'on lance maintenant explicitement avec un flush
		hibernateTemplate.flush();

		// si on est ici, c'est que la validation s'est bien passée
		Flash.message(String.format("Date de début du premier exercice commercial déplacée %s au %s.", RegDateHelper.dateToDisplayString(view.getAncienneDate()), RegDateHelper.dateToDisplayString(view.getNouvelleDate())), 4000);
		return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/change-date-fin.do", method = RequestMethod.POST)
	public String changeDateFin(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des bouclements des personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getPmId());

		final Tiers tiers = tiersDAO.get(view.getPmId());
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + view.getPmId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		if (bindingResult.hasErrors()) {
			final List<ExerciceCommercialView> viewExercicesCommerciaux = getViewExercicesCommerciaux(entreprise, true);
			model.addAttribute("exercices", viewExercicesCommerciaux);
			model.addAttribute("dateDebutAjoutable", isDateDebutAjoutable(viewExercicesCommerciaux));
			model.addAttribute("command", view);
			model.addAttribute("mode", "edit-df");
			model.addAttribute("mode_edit_df", view.getAncienneDate().index());
			return "tiers/edition/pm/bouclements/list";
		}

		// pas de changement -> pas de changement
		if (view.getAncienneDate() == view.getNouvelleDate()) {
			Flash.message("Aucun changement apporté.", 4000);
			return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
		}

		// premier contrôle : s'il y a une DI non-annulée qui intersecte la période entre les deux dates, on refuse la modification
		final List<DeclarationImpotOrdinairePM> dis = getDeclarationsNonAnnuleesTriees(entreprise);
		final DateRange deplacement = new DateRangeHelper.Range(RegDateHelper.minimum(view.getAncienneDate(), view.getNouvelleDate(), NullDateBehavior.LATEST),
		                                                        RegDateHelper.maximum(view.getAncienneDate(), view.getNouvelleDate(), NullDateBehavior.EARLIEST));
		if (DateRangeHelper.intersect(deplacement, dis)) {
			throw new ActionException(String.format("Le déplacement de la date de bouclement du %s au %s est impossible car au moins une déclaration d'impôt est impactée.",
			                                        RegDateHelper.dateToDisplayString(view.getAncienneDate()),
			                                        RegDateHelper.dateToDisplayString(view.getNouvelleDate())));
		}

		// on fait la modification demandée
		// 1. récupération des anciennes dates de bouclement
		final Set<Bouclement> bouclements = entreprise.getBouclements();

		// 2. remplacement de l'ancienne date par la nouvelle et impact jusqu'à la prochaine période fixée
		final List<ExerciceCommercial> anciensExercicesCommerciaux = tiersService.getExercicesCommerciaux(entreprise);
		final RegDate prochainBouclementFixeApresNouvelleDate = getPremierBouclementFixeApres(view.getNouvelleDate(), dis);
		final SortedSet<RegDate> nouvellesDatesBouclement = new TreeSet<>();
		for (ExerciceCommercial exercice : anciensExercicesCommerciaux) {
			if (!deplacement.isValidAt(exercice.getDateFin()) && !RegDateHelper.isBetween(exercice.getDateFin(), view.getNouvelleDate(), prochainBouclementFixeApresNouvelleDate, NullDateBehavior.LATEST)) {
				nouvellesDatesBouclement.add(exercice.getDateFin());
			}
		}
		nouvellesDatesBouclement.add(view.getNouvelleDate());
		if (prochainBouclementFixeApresNouvelleDate != null) {
			for (RegDate date = view.getNouvelleDate().addYears(1); date.compareTo(prochainBouclementFixeApresNouvelleDate) < 0; date = date.addYears(1)) {
				nouvellesDatesBouclement.add(date);
			}
		}

		// 3. re-calcul des cycles
		final List<Bouclement> nouveauxBouclements = bouclementService.extractBouclementsDepuisDates(nouvellesDatesBouclement, 12);

		// 4. comparaison avant/après et application des différences
		BouclementHelper.resetBouclements(entreprise, nouveauxBouclements);

		// 5. contrôle final des dates de bouclements
		controleDatesBouclements(view, anciensExercicesCommerciaux, nouvellesDatesBouclement);


		// forçons une validation du tout
		hibernateTemplate.flush();

		// si on est ici, c'est que la validation s'est bien passée
		Flash.message(String.format("Bouclement du %s déplacé au %s.", RegDateHelper.dateToDisplayString(view.getAncienneDate()), RegDateHelper.dateToDisplayString(view.getNouvelleDate())), 4000);
		return "redirect:/exercices/edit.do?pmId=" + view.getPmId();
	}

	private boolean isDateDebutAjoutable(List<ExerciceCommercialView> viewExercicesCommerciaux) {
		boolean dateDebutAjoutable = false;
		if (!viewExercicesCommerciaux.isEmpty()) {
			dateDebutAjoutable = true;
			// Cas spécial du 1er exercice commercial (de plus d'une année)
			// On ne peut pas ajouter une nouvelle date de début d'exercice commercial
			// s'il n'existe pas au moins une date de bouclement par année civile
			ExerciceCommercialView premierEC = viewExercicesCommerciaux.get(viewExercicesCommerciaux.size()-1);
			if (premierEC.getDateDebut().getOneDayBefore().year() < premierEC.getDateFin().year() - 1) {
				dateDebutAjoutable = false;
			}
		}
		return dateDebutAjoutable;
	}

	private void controleDatesBouclements(@Valid @ModelAttribute(value = "command") ChangeDateExerciceCommercialView view, List<ExerciceCommercial> anciensExercicesCommerciaux,
	                                      SortedSet<RegDate> nouvellesDatesBouclement) {
		// contrôle des dates (au moins un bouclement par an sauf potentiellement la première année)
		// ([SIFISC-18030] ce contrôle n'est effectif qu'à partir de la première année vraiment "unireg" des PM...)
		final Set<Integer> anneesBouclements = new HashSet<>(nouvellesDatesBouclement.size());
		for (RegDate dateBouclement : nouvellesDatesBouclement) {
			anneesBouclements.add(dateBouclement.year());
		}
		final int premiereAnnee = nouvellesDatesBouclement.first().year();
		final int derniereAnnee = nouvellesDatesBouclement.last().year();
		final int debutPremierExercice = anciensExercicesCommerciaux.get(0).getDateDebut().year();
		final int premierePeriodeFiscaleDeclarationsPersonnesMorales = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		if (premiereAnnee - debutPremierExercice > 1 && premiereAnnee >= premierePeriodeFiscaleDeclarationsPersonnesMorales) {
			throw new ActionException(String.format("Impossible de déplacer la date de bouclement du %s au %s car alors le premier exercice commercial s'étendrait sur plus de deux années civiles.",
			                                        RegDateHelper.dateToDisplayString(view.getAncienneDate()),
			                                        RegDateHelper.dateToDisplayString(view.getNouvelleDate())));
		}
		for (int annee = Math.max(premiereAnnee + 1, premierePeriodeFiscaleDeclarationsPersonnesMorales) ; annee < derniereAnnee ; ++ annee) {
			if (!anneesBouclements.contains(annee)) {
				throw new ActionException(String.format("Impossible de déplacer la date de bouclement du %s au %s car alors il n'y aurait plus de bouclement sur l'année civile %d.",
				                                        RegDateHelper.dateToDisplayString(view.getAncienneDate()),
				                                        RegDateHelper.dateToDisplayString(view.getNouvelleDate()),
				                                        annee));
			}
		}
	}

	/**
	 * Renvoie la première date de bouclement fixée (= veille de la date de début de la première DI valide) postérieure à la date de référence passée en paramètre (qui ne
	 * doit pas être dans une période couverte par une DI
	 * @param dateReference date de référence
	 * @param dis liste triée des DI non-annulées existante (= c'est ce qui fixe les exercices commerciaux...)
	 * @return la première date de bouclement fixée après la date de référence, ou <code>null</code> si une telle date n'existe pas
	 * @throws IllegalStateException si la date de référence est dans une période couverte par une DI
	 */
	@Nullable
	private static RegDate getPremierBouclementFixeApres(@NotNull RegDate dateReference, List<DeclarationImpotOrdinairePM> dis) {
		if (DateRangeHelper.rangeAt(dis, dateReference) != null) {
			throw new IllegalStateException("Erreur dans l'algorithme : la date de référence est située dans une période couverte par une DI non-annulée.");
		}

		for (DeclarationImpotOrdinairePM di : dis) {
			if (dateReference.compareTo(di.getDateDebut()) < 0) {
				return di.getDateDebut().getOneDayBefore();
			}
		}
		return null;
	}
}
