package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.DynamicDelegatingValidator;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.view.AddRegimeFiscalView;
import ch.vd.uniregctb.tiers.view.EditRegimeFiscalView;
import ch.vd.uniregctb.tiers.view.RegimeFiscalListEditView;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
public class SpecificiteFiscaleController {

	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private HibernateTemplate hibernateTemplate;
	private Validator validator;

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

	public void setValidators(Validator... validator) {
		this.validator = new DynamicDelegatingValidator(validator);
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(validator);
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
			Collections.sort(liste, new AnnulableHelper.AnnulableDateRangeComparator<>(true));
			for (RegimeFiscalListEditView view : liste) {
				if (!view.isAnnule()) {
					view.setLast(true);
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
	public StringHolder getMessageWarningRegimeFiscal(@RequestParam("pmId") long pmId, @RequestParam("date") RegDate date, @RequestParam("portee") RegimeFiscal.Portee portee, @RequestParam("code") String code) {
		final Entreprise entreprise = getEntreprise(pmId);
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
					return new StringHolder("Ce régime fiscal n'est valide que jusquà la période " + rf.getDernierePeriodeFiscaleValidite() + '.');
				}
				if (rf.getPremierePeriodeFiscaleValidite() != null && date.year() < rf.getPremierePeriodeFiscaleValidite()) {
					return new StringHolder("Ce régime fiscal n'est valide qu'à partir de la période " + rf.getPremierePeriodeFiscaleValidite() + '.');
				}

				final CategorieEntreprise categorie = tiersService.getCategorieEntreprise(entreprise, date);
				if (categorie != null) {
					switch (categorie) {
					case DPAPM:
					case APM:
						if (!rf.isPourAPM()) {
							return new StringHolder("Ce régime fiscal n'est en principe pas utilisé pour les entreprises de catégorie APM.");
						}
						break;
					case PM:
					case DPPM:
						if (!rf.isPourPM()) {
							return new StringHolder("Ce régime fiscal n'est en principe par utilisé pour les entreprises de catégorie PM.");
						}
						break;
					default:
						break;
					}
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
			bindingResult.addError(new FieldError("command", "code", ""));
		}

		// tout est bon, on fonce
		tiersService.addRegimeFiscal(entreprise, view.getPortee(), rf, view.getDateDebut(), view.getDateFin());
		return "redirect:/regimefiscal/edit-list.do?pmId=" + entreprise.getNumero() + "&portee=" + view.getPortee();
	}

	/**
	 * @return Map de clé = code de régime, et
	 */
	private Map<String, String> buildMapTypesRegimeFiscal(RegimeFiscal.Portee portee) {
		final List<TypeRegimeFiscal> rfs = serviceInfrastructureService.getRegimesFiscaux();
		final Map<String, String> map = new LinkedHashMap<>(rfs.size());
		for (TypeRegimeFiscal rf : rfs) {
			if (portee == null || (portee == RegimeFiscal.Portee.CH && rf.isFederal()) || (portee == RegimeFiscal.Portee.VD && rf.isCantonal())) {
				map.put(rf.getCode(), rf.getLibelle());
			}
		}
		return map;
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

		return showEditRegimeFiscal(new EditRegimeFiscalView(rf), model);
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

		// pour le moment, on ne peut juste que fermer le régime fiscal, il faut donc qu'il soit ouvert avant...
		if (rf.getDateFin() != null) {
			throw new ActionException("Le régime fiscal est déjà fermé.");
		}
		if (view.getDateFin() == null) {
			Flash.warning("Aucune modification effectuée.", 4000);
		}
		else {
			rf.setDateFin(view.getDateFin());
		}

		return "redirect:/regimefiscal/edit-list.do?pmId=" + rf.getEntreprise().getNumero() + "&portee=" + rf.getPortee();
	}

	//
	// Les allègements fiscaux
	//


	//
	// Les flags
	//


}
