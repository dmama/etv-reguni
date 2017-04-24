package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.DynamicDelegatingValidator;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.view.AddAllegementFiscalView;
import ch.vd.uniregctb.tiers.view.AddFlagEntrepriseView;
import ch.vd.uniregctb.tiers.view.AddRegimeFiscalView;
import ch.vd.uniregctb.tiers.view.AllegementFiscalView;
import ch.vd.uniregctb.tiers.view.EditAllegementFiscalView;
import ch.vd.uniregctb.tiers.view.EditFlagEntrepriseView;
import ch.vd.uniregctb.tiers.view.EditRegimeFiscalView;
import ch.vd.uniregctb.tiers.view.FlagEntrepriseView;
import ch.vd.uniregctb.tiers.view.RegimeFiscalListEditView;
import ch.vd.uniregctb.type.GroupeFlagsEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
public class SpecificiteFiscaleController {

	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private HibernateTemplate hibernateTemplate;
	private Validator validator;
	private TiersMapHelper tiersMapHelper;
	private ServiceRegimeFiscal regimeFiscalService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setRegimeFiscalService(ServiceRegimeFiscal regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	public void setValidators(Validator... validator) {
		this.validator = new DynamicDelegatingValidator(validator);
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		if (binder.getObjectName().equals("command")) {
			binder.setValidator(validator);
		}
	}

	@NotNull
	private Entreprise getEntreprise(long idpm) throws ObjectNotFoundException {
		final Tiers tiers = hibernateTemplate.get(Tiers.class, idpm);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Pas d'entreprise avec le numéro " + idpm);
		}
		return (Entreprise) tiers;
	}

	private Entreprise checkDroitEcritureTiers(long idpm) throws ObjectNotFoundException, AccessDeniedException {
		final Entreprise entreprise = getEntreprise(idpm);
		checkDroitEcritureTiers(entreprise);
		return entreprise;
	}

	private void checkDroitEcritureTiers(Entreprise entreprise) throws AccessDeniedException {
		controllerUtils.checkAccesDossierEnEcriture(entreprise.getNumero());
	}

	//
	// Les régimes fiscaux
	//

	private void checkDroitModificationRegimesFiscaux() throws AccessDeniedException {
		if (!SecurityHelper.isGranted(securityProvider, Role.REGIMES_FISCAUX)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des régimes fiscaux des personnes morales.");
		}
	}

	private List<RegimeFiscalListEditView> getRegimeFiscalListEditViews(Entreprise entreprise, RegimeFiscal.Portee portee) {

		// map des régimes fiscaux indexés par code
		final Map<String, TypeRegimeFiscal> mapRegimesParCode = getMapRegimesFiscauxParCode();

		// les régimes fiscaux
		final Set<RegimeFiscal> regimes = entreprise.getRegimesFiscaux();
		if (regimes != null) {
			final List<RegimeFiscalListEditView> liste = new ArrayList<>(regimes.size());
			for (RegimeFiscal regime : regimes) {
				if (regime.getPortee() == portee) {
					liste.add(new RegimeFiscalListEditView(regime.getId(), regime.isAnnule(), regime.getDateDebut(), regime.getDateFin(), mapRegimesParCode.get(regime.getCode())));
				}
			}
			liste.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(true));
			for (RegimeFiscalListEditView view : liste) {
				if (!view.isAnnule()) {
					view.setLast(true);
					break;
				}
			}
			for (RegimeFiscalListEditView view : CollectionsUtils.revertedOrder(liste)) {
				if (!view.isAnnule()) {
					view.setFirst(true);
					break;
				}
			}

			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Reconstitution d'une map des régimes fiscaux disponibles indexés par code
	 * @return la map en question
	 */
	@NotNull
	private Map<String, TypeRegimeFiscal> getMapRegimesFiscauxParCode() {
		// map des régimes fiscaux existants indexés par code
		final List<TypeRegimeFiscal> typesRegime = serviceInfrastructureService.getRegimesFiscaux();
		final Map<String, TypeRegimeFiscal> mapRegimesParCode = new HashMap<>(typesRegime.size());
		for (TypeRegimeFiscal type : typesRegime) {
			mapRegimesParCode.put(type.getCode(), type);
		}
		return mapRegimesParCode;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/regimefiscal/edit-list.do", method = RequestMethod.GET)
	public String showListEditRegimeFiscal(@RequestParam("pmId") long pmId, @RequestParam("portee") RegimeFiscal.Portee portee, Model model) throws AccessDeniedException, ObjectNotFoundException {

		checkDroitModificationRegimesFiscaux();
		final Entreprise entreprise = checkDroitEcritureTiers(pmId);

		model.addAttribute("pmId", pmId);
		model.addAttribute("portee", portee);
		model.addAttribute("regimes", getRegimeFiscalListEditViews(entreprise, portee));

		return "tiers/edition/pm/specificites/list-rf";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/regimefiscal/cancel.do", method = RequestMethod.POST)
	public String cancelRegimeFiscal(@RequestParam("rfId") long rfId) throws AccessDeniedException, ObjectNotFoundException {

		checkDroitModificationRegimesFiscaux();

		final RegimeFiscal rf = hibernateTemplate.get(RegimeFiscal.class, rfId);
		if (rf == null) {
			throw new ObjectNotFoundException("Régime fiscal inconnu!");
		}
		checkDroitEcritureTiers(rf.getEntreprise());

		tiersService.annuleRegimeFiscal(rf);
		return "redirect:/regimefiscal/edit-list.do?pmId=" + rf.getEntreprise().getNumero() + "&portee=" + rf.getPortee();
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/regimefiscal/add.do", method = RequestMethod.GET)
	public String showAddRegimeFiscal(@RequestParam("pmId") long pmId, @RequestParam("portee") RegimeFiscal.Portee portee, Model model) throws AccessDeniedException {

		checkDroitModificationRegimesFiscaux();
		checkDroitEcritureTiers(pmId);

		return showAddRegimeFiscal(new AddRegimeFiscalView(pmId, portee), model);
	}

	private String showAddRegimeFiscal(AddRegimeFiscalView view, Model model) {
		model.addAttribute("command", view);
		model.addAttribute("typesRegimeFiscal", buildMapTypesRegimeFiscal(view.getPortee()));
		return "tiers/edition/pm/specificites/add-rf";
	}

	/**
	 * Classe un peu bidon pour qu'une simple String puisse être exposé en JSON
	 */
	public static class StringHolder {
		public final String text;
		public StringHolder(String text) {
			this.text = text;
		}
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/regimefiscal/warning-message.do", method = RequestMethod.GET)
	@ResponseBody
	public StringHolder getMessageWarningRegimeFiscal(@RequestParam("date") RegDate date, @RequestParam("portee") RegimeFiscal.Portee portee, @RequestParam("code") String code) {
		final Map<String, TypeRegimeFiscal> mapRegimes = getMapRegimesFiscauxParCode();
		final TypeRegimeFiscal rf = mapRegimes.get(code);
		if (rf != null) {
			if (portee == RegimeFiscal.Portee.VD && !rf.isCantonal()) {
				return new StringHolder("Régime fiscal interdit au niveau cantonal.");
			}
			if (portee == RegimeFiscal.Portee.CH && !rf.isFederal()) {
				return new StringHolder("Régime fiscal interdit au niveau fédéral.");
			}

			if (date != null) {
				if (rf.getDernierePeriodeFiscaleValidite() != null && date.year() > rf.getDernierePeriodeFiscaleValidite()) {
					return new StringHolder("Ce régime fiscal n'est valide que jusqu'à la période " + rf.getDernierePeriodeFiscaleValidite() + '.');
				}
				if (rf.getPremierePeriodeFiscaleValidite() != null && date.year() < rf.getPremierePeriodeFiscaleValidite()) {
					return new StringHolder("Ce régime fiscal n'est valide qu'à partir de la période " + rf.getPremierePeriodeFiscaleValidite() + '.');
				}
			}
		}

		// rien à dire...
		return null;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/regimefiscal/add.do", method = RequestMethod.POST)
	public String addRegimeFiscal(@Valid @ModelAttribute("command") AddRegimeFiscalView view, BindingResult bindingResult, Model model) throws AccessDeniedException {

		checkDroitModificationRegimesFiscaux();
		final Entreprise entreprise = checkDroitEcritureTiers(view.getPmId());

		// vérification de la validité de la vue
		if (bindingResult.hasErrors()) {
			return showAddRegimeFiscal(view, model);
		}

		// identification du régime fiscal
		final Map<String, TypeRegimeFiscal> mapRegimes = getMapRegimesFiscauxParCode();
		final TypeRegimeFiscal rf = mapRegimes.get(view.getCode());
		if (rf == null) {
			bindingResult.rejectValue("code", "error.type.regime.fiscal.invalide");
			return showAddRegimeFiscal(view, model);
		}

		final List<RegimeFiscal> regimesFiscauxValides = entreprise.getRegimesFiscauxNonAnnulesTries(view.getPortee());

		// vérification que la date de début n'est pas déjà utilisée pour un autre régime fiscal (auquel cas c'est plutôt une édition
		// qu'il faudrait faire...)
		final boolean sameDateDebutUsed = regimesFiscauxValides.stream().anyMatch(existing -> existing.getDateDebut() == view.getDateDebut());
		if (sameDateDebutUsed) {
			bindingResult.rejectValue("dateDebut", "error.date.debut.regime.fiscal.deja.utilisee");
			return showAddRegimeFiscal(view, model);
		}

		// calcul de la date de fin (= la veille de la date de début du prochain)
		final RegDate dateFin = regimesFiscauxValides.stream()
				.filter(existing -> RegDateHelper.isAfter(existing.getDateDebut(), view.getDateDebut(), NullDateBehavior.EARLIEST))
				.map(RegimeFiscal::getDateDebut)
				.min(Comparator.naturalOrder())
				.map(RegDate::getOneDayBefore)
				.orElse(null);

		// tout est bon, on fonce
		tiersService.addRegimeFiscal(entreprise, view.getPortee(), rf, view.getDateDebut(), dateFin);
		return "redirect:/regimefiscal/edit-list.do?pmId=" + entreprise.getNumero() + "&portee=" + view.getPortee();
	}

	/**
	 * @return Map de clé = code de régime, et valeur = libellé associé (l'itérateur sur la map donne les entrées dans l'ordre alphabétique des libellés
	 */
	private Map<String, String> buildMapTypesRegimeFiscal(RegimeFiscal.Portee portee) {
		final List<TypeRegimeFiscal> rfs = serviceInfrastructureService.getRegimesFiscaux();
		final Map<String, String> map = new HashMap<>(rfs.size());
		for (TypeRegimeFiscal rf : rfs) {
			if (portee == null || (portee == RegimeFiscal.Portee.CH && rf.isFederal()) || (portee == RegimeFiscal.Portee.VD && rf.isCantonal())) {
				map.put(rf.getCode(), rf.getLibelleAvecCode());
			}
		}

		// on va trier par ordre alphabétique des libellés
		final List<Map.Entry<String, String>> flatMap = new ArrayList<>(map.entrySet());
		flatMap.sort(Comparator.comparing(Map.Entry::getValue, Comparator.naturalOrder()));     // ordre alphabétique des valeurs
		final Map<String, String> sortedMap = new LinkedHashMap<>(map.size());
		for (Map.Entry<String, String> entry : flatMap) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	private String showEditRegimeFiscal(EditRegimeFiscalView view, Model model) {
		model.addAttribute("command", view);
		model.addAttribute("typesRegimeFiscal", buildMapTypesRegimeFiscal(view.getPortee()));
		return "tiers/edition/pm/specificites/edit-rf";
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/regimefiscal/edit.do", method = RequestMethod.GET)
	public String showEditRegimeFiscal(@RequestParam("rfId") long rfId, Model model) throws ObjectNotFoundException, AccessDeniedException {
		checkDroitModificationRegimesFiscaux();
		final RegimeFiscal rf = hibernateTemplate.get(RegimeFiscal.class, rfId);
		if (rf == null) {
			throw new ObjectNotFoundException("Régime fiscal inconnu!");
		}
		checkDroitEcritureTiers(rf.getEntreprise());

		return showEditRegimeFiscal(new EditRegimeFiscalView(rf, regimeFiscalService), model);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/regimefiscal/edit.do", method = RequestMethod.POST)
	public String editRegimeFiscal(@Valid @ModelAttribute("command") EditRegimeFiscalView view, BindingResult bindingResult, Model model) throws ObjectNotFoundException, AccessDeniedException {

		checkDroitModificationRegimesFiscaux();

		// vérification de la validité de la vue
		if (bindingResult.hasErrors()) {
			return showEditRegimeFiscal(view, model);
		}

		final RegimeFiscal rf = hibernateTemplate.get(RegimeFiscal.class, view.getRfId());
		if (rf == null) {
			throw new ObjectNotFoundException("Régime fiscal inconnu!");
		}
		checkDroitEcritureTiers(rf.getEntreprise());

		// on peut changer : uniquement le type de régime fiscal

		final boolean sameCode = Objects.equals(rf.getCode(), view.getCode());
		if (sameCode) {
			Flash.warning("Aucune modification effectuée.", 4000);
		}
		else {
			final Map<String, TypeRegimeFiscal> mapRegimes = getMapRegimesFiscauxParCode();
			final TypeRegimeFiscal nouveauTypeRegime = mapRegimes.get(view.getCode());
			if (nouveauTypeRegime == null) {
				throw new ActionException("Ce type de régime fiscal est inconnu.");
			}
			tiersService.replaceRegimeFiscal(rf, nouveauTypeRegime);
		}

		return "redirect:/regimefiscal/edit-list.do?pmId=" + rf.getEntreprise().getNumero() + "&portee=" + rf.getPortee();
	}

	//
	// Les allègements fiscaux
	//

	private void checkDroitModificationAllegements() throws AccessDeniedException {
		if (!SecurityHelper.isGranted(securityProvider, Role.ALLEGEMENTS_FISCAUX)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des allègements fiscaux des personnes morales.");
		}
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/allegement/edit-list.do", method = RequestMethod.GET)
	public String showListEditAllegements(@RequestParam("pmId") long pmId, Model model) throws AccessDeniedException, ObjectNotFoundException {

		checkDroitModificationAllegements();
		final Entreprise entreprise = checkDroitEcritureTiers(pmId);

		model.addAttribute("pmId", pmId);
		model.addAttribute("allegements", getAllegementFiscalListEditViews(entreprise));

		return "tiers/edition/pm/specificites/list-allegements";
	}

	private static List<AllegementFiscalView> getAllegementFiscalListEditViews(Entreprise entreprise) {
		final Set<AllegementFiscal> afs = entreprise.getAllegementsFiscaux();
		if (afs == null || afs.isEmpty()) {
			return Collections.emptyList();
		}

		final List<AllegementFiscalView> views = new ArrayList<>(afs.size());
		for (AllegementFiscal af : afs) {
			views.add(new AllegementFiscalView(af));
		}
		Collections.sort(views, AllegementFiscalView.DEFAULT_COMPARATOR);
		return views;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/allegement/cancel.do", method = RequestMethod.POST)
	public String cancelAllegement(@RequestParam("afId") long afId) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationAllegements();

		final AllegementFiscal af = hibernateTemplate.get(AllegementFiscal.class, afId);
		if (af == null) {
			throw new ObjectNotFoundException("Allègement fiscal inconnu!");
		}
		checkDroitEcritureTiers(af.getEntreprise());

		tiersService.annuleAllegementFiscal(af);
		return "redirect:/allegement/edit-list.do?pmId=" + af.getEntreprise().getNumero();
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/allegement/add.do", method = RequestMethod.GET)
	public String showAddAllegement(@RequestParam("pmId") long pmId, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationAllegements();
		checkDroitEcritureTiers(pmId);
		return showAddAllegement(new AddAllegementFiscalView(pmId), model);
	}

	private String showAddAllegement(AddAllegementFiscalView view, Model model) {
		model.addAttribute("command", view);
		model.addAttribute("typesCollectivite", tiersMapHelper.getTypesCollectiviteAllegement());
		model.addAttribute("typesImpot", tiersMapHelper.getTypesImpotAllegement());
		model.addAttribute("typesICC", tiersMapHelper.getTypesICCAllegement());
		model.addAttribute("typesIFD", tiersMapHelper.getTypesIFDAllegement());
		return "tiers/edition/pm/specificites/add-allegement";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/allegement/add.do", method = RequestMethod.POST)
	public String addAllegement(@Valid @ModelAttribute("command") AddAllegementFiscalView view, BindingResult bindingResult, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationAllegements();

		// renvoi en cas d'erreur
		if (bindingResult.hasErrors()) {
			return showAddAllegement(view, model);
		}

		final Entreprise entreprise = checkDroitEcritureTiers(view.getPmId());
		final BigDecimal pourcentage = view.getFlagPourcentageMontant() == AddAllegementFiscalView.PourcentageMontant.POURCENTAGE ? view.getPourcentageAllegement() : null;
		switch (view.getTypeCollectivite()) {
		case CANTON:
			tiersService.addAllegementFiscalCantonal(entreprise, pourcentage, view.getTypeImpot(), view.getDateDebut(), view.getDateFin(), view.getTypeICC());
			break;
		case COMMUNE:
			tiersService.addAllegementFiscalCommunal(entreprise, pourcentage, view.getTypeImpot(), view.getDateDebut(), view.getDateFin(), view.getTypeICC(), view.getNoOfsCommune());
			break;
		case CONFEDERATION:
			tiersService.addAllegementFiscalFederal(entreprise, pourcentage, view.getTypeImpot(), view.getDateDebut(), view.getDateFin(), view.getTypeIFD());
			break;
		default:
			throw new IllegalArgumentException("Type de collectivité non supporté : " + view.getTypeCollectivite());
		}

		return "redirect:/allegement/edit-list.do?pmId=" + entreprise.getNumero();
	}

	private String showEditAllegement(EditAllegementFiscalView editView, AllegementFiscalView view, Model model) {
		model.addAttribute("command", editView);
		model.addAttribute("allegement", view);
		return "tiers/edition/pm/specificites/edit-allegement";
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/allegement/edit.do", method = RequestMethod.GET)
	public String showEditAllegement(@RequestParam("afId") long afId, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationAllegements();
		final AllegementFiscal af = hibernateTemplate.get(AllegementFiscal.class, afId);
		if (af == null) {
			throw new ObjectNotFoundException("Allègement fiscal inconnu!");
		}
		checkDroitEcritureTiers(af.getEntreprise());
		return showEditAllegement(new EditAllegementFiscalView(af), new AllegementFiscalView(af), model);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/allegement/edit.do", method = RequestMethod.POST)
	public String editAllegement(@Valid @ModelAttribute("command") EditAllegementFiscalView view, BindingResult bindingResult, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationAllegements();

		final AllegementFiscal af = hibernateTemplate.get(AllegementFiscal.class, view.getAfId());
		if (af == null) {
			throw new ObjectNotFoundException("Allègement fiscal inconnu!");
		}
		checkDroitEcritureTiers(af.getEntreprise());

		// renvoi en cas d'erreur
		if (bindingResult.hasErrors()) {
			view.resetNonEditableValues(af);
			return showEditAllegement(view, new AllegementFiscalView(af), model);
		}

		// pour le moment, on ne gère que la fermeture de l'allègement
		if (af.getDateFin() != null) {
			throw new ActionException("L'allègement est déjà fermé.");
		}
		if (view.getDateFin() == null) {
			Flash.warning("Aucune modification effectuée.", 4000);
		}
		else {
			tiersService.closeAllegementFiscal(af, view.getDateFin());
		}
		return "redirect:/allegement/edit-list.do?pmId=" + af.getEntreprise().getNumero();
	}

	//
	// Les flags
	//

	private void checkDroitModificationFlags() throws AccessDeniedException {
		if (!SecurityHelper.isGranted(securityProvider, Role.FLAGS_PM)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la modification des spécificités fiscales des personnes morales.");
		}
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/flag-entreprise/edit-list.do", method = RequestMethod.GET)
	public String showListEditFlags(@RequestParam("pmId") long pmId, @RequestParam("group") GroupeFlagsEntreprise groupe, Model model) throws AccessDeniedException, ObjectNotFoundException {

		checkDroitModificationFlags();
		final Entreprise entreprise = checkDroitEcritureTiers(pmId);

		model.addAttribute("pmId", pmId);
		model.addAttribute("group", groupe);
		model.addAttribute("flags", getFlagListEditViews(entreprise, groupe));

		return "tiers/edition/pm/specificites/list-flags";
	}

	private static List<FlagEntrepriseView> getFlagListEditViews(Entreprise entreprise, GroupeFlagsEntreprise groupe) {
		final Set<FlagEntreprise> flags = entreprise.getFlags();
		if (flags == null || flags.isEmpty()) {
			return Collections.emptyList();
		}

		final List<FlagEntrepriseView> views = new ArrayList<>(flags.size());
		for (FlagEntreprise flag : flags) {
			if (flag.getGroupe() == groupe) {
				views.add(new FlagEntrepriseView(flag));
			}
		}
		Collections.sort(views, new AnnulableHelper.AnnulableDateRangeComparator<>(true));
		return views;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/flag-entreprise/cancel.do", method = RequestMethod.POST)
	public String cancelFlag(@RequestParam("flagId") long flagId) throws AccessDeniedException, ObjectNotFoundException {

		checkDroitModificationFlags();
		final FlagEntreprise flag = hibernateTemplate.get(FlagEntreprise.class, flagId);
		if (flag == null) {
			throw new ObjectNotFoundException("Spécificité inconnue!");
		}
		checkDroitEcritureTiers(flag.getEntreprise());

		tiersService.annuleFlagEntreprise(flag);
		return "redirect:/flag-entreprise/edit-list.do?pmId=" + flag.getEntreprise().getNumero() + "&group=" + flag.getGroupe();
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/flag-entreprise/add.do", method = RequestMethod.GET)
	public String showAddFlag(@RequestParam("pmId") long pmId, @RequestParam("group") GroupeFlagsEntreprise groupe, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationFlags();
		checkDroitEcritureTiers(pmId);
		return showAddFlag(new AddFlagEntrepriseView(pmId), groupe, model);
	}

	private String showAddFlag(AddFlagEntrepriseView view, GroupeFlagsEntreprise groupe, Model model) {
		model.addAttribute("command", view);
		model.addAttribute("group", groupe);
		model.addAttribute("flagTypes", tiersMapHelper.getTypesFlagEntreprise(groupe));
		return "tiers/edition/pm/specificites/add-flag";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/flag-entreprise/add.do", method = RequestMethod.POST)
	public String addFlag(@Valid @ModelAttribute("command") AddFlagEntrepriseView view, BindingResult bindingResult,
	                      @RequestParam("group") GroupeFlagsEntreprise groupe,
	                      Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationFlags();

		// vérification des erreurs avant d'aller plus loin
		if (bindingResult.hasErrors()) {
			return showAddFlag(view, groupe, model);
		}

		final Entreprise entreprise = checkDroitEcritureTiers(view.getPmId());
		tiersService.addFlagEntreprise(entreprise, view.getValue(), view.getDateDebut(), view.getDateFin());
		return "redirect:/flag-entreprise/edit-list.do?pmId=" + view.getPmId() + "&group=" + view.getValue().getGroupe();
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/flag-entreprise/edit.do", method = RequestMethod.GET)
	public String showEditFlag(@RequestParam("flagId") long flagId, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationFlags();
		final FlagEntreprise flag = hibernateTemplate.get(FlagEntreprise.class, flagId);
		if (flag == null) {
			throw new ObjectNotFoundException("Spécificité inconnue!");
		}
		checkDroitEcritureTiers(flag.getEntreprise());
		return showEditFlag(new EditFlagEntrepriseView(flag), new FlagEntrepriseView(flag), flag.getGroupe(), model);
	}

	private String showEditFlag(EditFlagEntrepriseView editView, FlagEntrepriseView view, GroupeFlagsEntreprise groupe, Model model) {
		model.addAttribute("command", editView);
		model.addAttribute("flag", view);
		model.addAttribute("group", groupe);
		return "tiers/edition/pm/specificites/edit-flag";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/flag-entreprise/edit.do", method = RequestMethod.POST)
	public String editFlag(@Valid @ModelAttribute("command") EditFlagEntrepriseView view, BindingResult bindingResult, Model model) throws AccessDeniedException, ObjectNotFoundException {
		checkDroitModificationFlags();

		final FlagEntreprise flag = hibernateTemplate.get(FlagEntreprise.class, view.getFlagId());
		if (flag == null) {
			throw new ObjectNotFoundException("Spécificité inconnue!");
		}
		checkDroitEcritureTiers(flag.getEntreprise());

		// traitement des cas d'erreur
		if (bindingResult.hasErrors()) {
			view.resetNonEditableValues(flag);
			return showEditFlag(view, new FlagEntrepriseView(flag), flag.getGroupe(), model);
		}

		// pour le moment, on ne gère que la fermeture du flag
		if (flag.getDateFin() != null) {
			throw new ActionException("La spécificité est déjà fermée.");
		}
		if (view.getDateFin() == null) {
			Flash.warning("Aucune modification effectuée.", 4000);
		}
		else {
			tiersService.closeFlagEntreprise(flag, view.getDateFin());
		}
		return "redirect:/flag-entreprise/edit-list.do?pmId=" + flag.getEntreprise().getNumero() + "&group=" + flag.getGroupe();
	}
}
