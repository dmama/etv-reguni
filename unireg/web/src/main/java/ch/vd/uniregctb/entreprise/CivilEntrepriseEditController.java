package ch.vd.uniregctb.entreprise;

import javax.validation.Valid;

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
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/civil/entreprise")
public class CivilEntrepriseEditController {

	private static final String ID = "id";

	private static final String FORME_JURIDIQUE_MAP_NAME = "formesJuridiques";
	private static final String TIERS_ID = "tiersId";
	private static final String DATA = "data";

	private TiersDAO tiersDAO;
	private TiersMapHelper tiersMapHelper;
	private TiersService tiersService;
	private ControllerUtils controllerUtils;
	private ServiceInfrastructureService infraService;
	private SecurityProviderInterface securityProvider;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;
	private EntrepriseService entrepriseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
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

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	private static class CivilEditValidator extends DelegatingValidator {
		private CivilEditValidator() {
			addSubValidator(EntrepriseView.class, new DummyValidator<>(EntrepriseView.class));
			addSubValidator(AddRaisonSocialeView.class, new AddRaisonSocialeViewValidator());
			/*addSubValidator(AddFormeJuridiqueViewValidator.class, new AddFormeJuridiqueViewValidator());*/
		}
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new CivilEditValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	private Autorisations getAutorisations(Tiers tiers) {
		return autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	private void checkDroitEditionDonneesCiviles(Tiers tiers) {
		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isEditable() || !auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des données civiles des tiers de ce type.");
		}
	}

	private boolean hasDroitsModificationsCtb(){
		return SecurityHelper.isGranted(securityProvider, Role.MODIF_HAB_DEBPUR) ||
				SecurityHelper.isGranted(securityProvider,Role.MODIF_HC_HS) ||
				SecurityHelper.isGranted(securityProvider,Role.MODIF_NONHAB_DEBPUR) ||
				SecurityHelper.isGranted(securityProvider,Role.MODIF_NONHAB_INACTIF) ||
				SecurityHelper.isGranted(securityProvider,Role.MODIF_VD_ORD) ||
				SecurityHelper.isGranted(securityProvider,Role.MODIF_VD_SOURC);
	}


	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editEntreprise(Model model, @RequestParam(value = ID) long id) {
/*
		if (!SecurityHelper.isGranted(securityProvider, Role.MODIF_AC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des tiers de ce type.");
		}
*/

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Entreprise) {
//			checkDroitEditionDonneesCiviles(tiers);
			final EntrepriseView view = entrepriseService.get((Entreprise) tiers);
			return showEditEntreprise(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private String showEditEntreprise(Model model, long id, EntrepriseView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(TIERS_ID, id);
		return "/tiers/edition/civil/edit-entreprise";
	}

	@RequestMapping(value = "/raisonsociale/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addRaisonSociale(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de raison sociale.");
		}

		//controllerUtils.checkAccesDossierEnEcriture(tiersId); // TODO: A utiliser pour les données civiles ??

		model.addAttribute("command", new AddRaisonSocialeView(entreprise.getNumero(), RegDate.get(), null, null));
		return "donnees-civiles/addRaisonSociale";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonsociale/add.do", method = RequestMethod.POST)
	public String addRaisonSociale(@Valid @ModelAttribute("command") final AddRaisonSocialeView view, BindingResult result, Model model) throws Exception {

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de raison sociale.");
		}

		//controllerUtils.checkAccesDossierEnEcriture(ctbId); // TODO: A utiliser pour les données civiles ??

		if (result.hasErrors()) {
			model.addAttribute("raisonSociale", view);
			return "donnees-civiles/addRaisonSociale";
		}

		tiersService.addRaisonSocialeFiscale(entreprise, view.getRaisonSociale(), view.getDateDebut(), view.getDateFin());

		return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancelRaisonSociale(long raisonSocialeId) throws Exception {

		final RaisonSocialeFiscaleEntreprise raisonSociale = hibernateTemplate.get(RaisonSocialeFiscaleEntreprise.class, raisonSocialeId);
		if (raisonSociale == null) {
			throw new ObjectNotFoundException("La raison sociale n°" + raisonSocialeId + " n'existe pas.");
		}
		final Entreprise entreprise = raisonSociale.getEntreprise();
		//controllerUtils.checkAccesDossierEnEcriture(entreprise.getId()); // TODO: A utiliser pour les données civiles ??

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de raisons sociales.");
		}

		tiersService.annuleRaisonSocialeFiscale(raisonSociale);

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getId();// + "&highlightFor=" + raisonSociale.getId();
	}

/*
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/entreprise/formejuridique/add.do", method = RequestMethod.POST)
	public String addFormeJuridique(@Valid @ModelAttribute("command") final AddFormeJuridiqueView view, BindingResult result, Model model) throws Exception {

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de formes juridiques.");
		}

		//controllerUtils.checkAccesDossierEnEcriture(ctbId); // TODO: A utiliser pour les données civiles ??

		if (result.hasErrors()) {
			model.addAttribute("command", view);
			return "donnees-civiles/addFormeJuridique";
		}

		//tiersService.addFormeJuridiqueFiscale(entreprise, new FormeJuridiqueFiscaleEntreprise(view.getDateDebut(), view.getDateFin(), view.getType());

		return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}
*/

	/*
	@RequestMapping(value = "/principal/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editPrincipal(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, forId);
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ffp.getTiers().getNumero());

		model.addAttribute("command", new EditForPrincipalView(ffp));
		return "fors/principal/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/principal/edit.do", method = RequestMethod.POST)
	public String editPrincipal(@Valid @ModelAttribute("command") final EditForPrincipalView view, BindingResult result, Model model) throws Exception {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, view.getId());
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		final long ctbId = ffp.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			return "fors/principal/edit";
		}

		final ForFiscalPrincipal newFor = tiersService.updateForPrincipal(ffp, view.getDateFin(), view.getMotifFin(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	private static String buildHighlightForParam(@Nullable ForFiscal newFor) {
		if (newFor == null) {
			return StringUtils.EMPTY;
		}
		return "&highlightFor=" + newFor.getId();
	}

*/
}
