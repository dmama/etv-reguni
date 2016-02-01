package ch.vd.uniregctb.entreprise;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.ContribuableInfosEntrepriseViewValidator;
import ch.vd.uniregctb.tiers.view.ContribuableInfosEntrepriseView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/civil/entreprise")
public class CivilEntrepriseEditController {

	private static final String ID = "id";

	private static final String FORMES_JURIDIQUES_ENTREPRISE_NAME = "formesJuridiquesEntrepriseEnum";
	private static final String TIERS_ID = "tiersId";
	private static final String DATA = "data";

	private TiersDAO tiersDAO;
	private TiersMapHelper tiersMapHelper;
	private TiersService tiersService;
	private ControllerUtils controllerUtils;
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

	private static class CivilEditValidator extends DelegatingValidator {
		private CivilEditValidator() {
			addSubValidator(EntrepriseView.class, new DummyValidator<>(EntrepriseView.class));
			addSubValidator(AddRaisonSocialeView.class, new AddRaisonSocialeViewValidator());
			addSubValidator(EditRaisonSocialeView.class, new EditRaisonSocialeViewValidator());
			addSubValidator(AddFormeJuridiqueView.class, new AddFormeJuridiqueViewValidator());
			addSubValidator(EditFormeJuridiqueView.class, new EditFormeJuridiqueViewValidator());
			addSubValidator(AddCapitalView.class, new AddCapitalViewValidator());
			addSubValidator(EditCapitalView.class, new EditCapitalViewValidator());
			addSubValidator(ContribuableInfosEntrepriseView.class, new ContribuableInfosEntrepriseViewValidator());
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

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editEntreprise(Model model, @RequestParam(value = ID) long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
			final EntrepriseView view = entrepriseService.getEntreprise((Entreprise) tiers);
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

	/* Raison sociale */

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

		model.addAttribute("command", new AddRaisonSocialeView(entreprise.getNumero(), RegDate.get(), null, null));
		return "donnees-civiles/add-raison-sociale";
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

		if (result.hasErrors()) {
			model.addAttribute("command", view);
			return "donnees-civiles/add-raison-sociale";
		}

		tiersService.addRaisonSocialeFiscale(entreprise, view.getRaisonSociale(), view.getDateDebut(), view.getDateFin());

		return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}

	@RequestMapping(value = "/raisonsociale/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editRaisonSociale(@RequestParam(value = "raisonSocialeId", required = true) long raisonSocialeId, Model model) {

		final RaisonSocialeFiscaleEntreprise raisonSociale = hibernateTemplate.get(RaisonSocialeFiscaleEntreprise.class, raisonSocialeId);
		if (raisonSociale == null) {
			throw new ObjectNotFoundException("La raison sociale avec l'id = " + raisonSocialeId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(raisonSociale.getEntreprise());
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de raisons sociales.");
		}

		model.addAttribute("command", new EditRaisonSocialeView(raisonSociale));
		return "donnees-civiles/edit-raison-sociale";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonsociale/edit.do", method = RequestMethod.POST)
	public String editRaisonSociale(@Valid @ModelAttribute("command") final EditRaisonSocialeView view, BindingResult result, Model model) throws Exception {

		final RaisonSocialeFiscaleEntreprise raisonSociale = hibernateTemplate.get(RaisonSocialeFiscaleEntreprise.class, view.getId());
		if (raisonSociale == null) {
			throw new ObjectNotFoundException("La raison sociale avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Entreprise entreprise = raisonSociale.getEntreprise();

		if (raisonSociale.getRaisonSociale() != null && ! raisonSociale.getRaisonSociale().equals(view.getRaisonSociale())) {

			final Autorisations auth = getAutorisations(entreprise);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de raisons sociales.");
			}

			final long ctbId = entreprise.getNumero();
			controllerUtils.checkAccesDossierEnEcriture(ctbId);

			if (result.hasErrors()) {
				return "donnees-civiles/edit-raison-sociale";
			}

			tiersService.updateRaisonSocialeFiscale(raisonSociale, view.getRaisonSociale());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancelRaisonSociale(long raisonSocialeId) throws Exception {

		final RaisonSocialeFiscaleEntreprise raisonSociale = hibernateTemplate.get(RaisonSocialeFiscaleEntreprise.class, raisonSocialeId);
		if (raisonSociale == null) {
			throw new ObjectNotFoundException("La raison sociale n°" + raisonSocialeId + " n'existe pas.");
		}
		final Entreprise entreprise = raisonSociale.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de raisons sociales.");
		}

		tiersService.annuleRaisonSocialeFiscale(raisonSociale);

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getId();// + "&highlightFor=" + raisonSociale.getId();
	}

	/* Forme juridique */

	@RequestMapping(value = "/formejuridique/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addFormeJuridique(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de forme juridique.");
		}

		model.addAttribute("command", new AddFormeJuridiqueView(entreprise.getNumero(), RegDate.get(), null, null));
		model.addAttribute(FORMES_JURIDIQUES_ENTREPRISE_NAME, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		return "donnees-civiles/add-forme-juridique";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/add.do", method = RequestMethod.POST)
	public String addFormeJuridique(@Valid @ModelAttribute("command") final AddFormeJuridiqueView view, BindingResult result, Model model) throws Exception {

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de forme juridique.");
		}

		if (result.hasErrors()) {
			model.addAttribute("command", view);
			model.addAttribute(FORMES_JURIDIQUES_ENTREPRISE_NAME, tiersMapHelper.getMapFormeJuridiqueEntreprise());
			return "donnees-civiles/add-forme-juridique";
		}

		tiersService.addFormeJuridiqueFiscale(entreprise, view.getFormeJuridique(), view.getDateDebut(), view.getDateFin());

		return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}

	@RequestMapping(value = "/formejuridique/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editFormeJuridique(@RequestParam(value = "formeJuridiqueId", required = true) long formeJuridiqueId, Model model) {

		final FormeJuridiqueFiscaleEntreprise formeJuridique = hibernateTemplate.get(FormeJuridiqueFiscaleEntreprise.class, formeJuridiqueId);
		if (formeJuridique == null) {
			throw new ObjectNotFoundException("La forme juridique avec l'id = " + formeJuridiqueId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(formeJuridique.getEntreprise());
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de formes juridiques.");
		}

		model.addAttribute("command", new EditFormeJuridiqueView(formeJuridique));
		model.addAttribute(FORMES_JURIDIQUES_ENTREPRISE_NAME, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		return "donnees-civiles/edit-forme-juridique";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/edit.do", method = RequestMethod.POST)
	public String editFormeJuridique(@Valid @ModelAttribute("command") final EditFormeJuridiqueView view, BindingResult result, Model model) throws Exception {

		final FormeJuridiqueFiscaleEntreprise formeJuridique = hibernateTemplate.get(FormeJuridiqueFiscaleEntreprise.class, view.getId());
		if (formeJuridique == null) {
			throw new ObjectNotFoundException("La forme juridique avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Entreprise entreprise = formeJuridique.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de formes juridiques.");
		}

		if (formeJuridique.getFormeJuridique() != null && ! formeJuridique.getFormeJuridique().equals(view.getFormeJuridique())) {

			final long ctbId = entreprise.getNumero();
			controllerUtils.checkAccesDossierEnEcriture(ctbId);

			if (result.hasErrors()) {
				return "donnees-civiles/edit-forme-juridique";
			}

			tiersService.updateFormeJuridiqueFiscale(formeJuridique, view.getFormeJuridique());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/cancel.do", method = RequestMethod.POST)
	public String cancelFormeJuridique(long formeJuridiqueId) throws Exception {

		final FormeJuridiqueFiscaleEntreprise formeJuridique = hibernateTemplate.get(FormeJuridiqueFiscaleEntreprise.class, formeJuridiqueId);
		if (formeJuridique == null) {
			throw new ObjectNotFoundException("La forme juridique n°" + formeJuridiqueId + " n'existe pas.");
		}
		final Entreprise entreprise = formeJuridique.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de formes juridiques.");
		}

		tiersService.annuleFormeJuridiqueFiscale(formeJuridique);

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getId();// + "&highlightFor=" + formeJuridique.getId();
	}

	/* Capital */

	@RequestMapping(value = "/capital/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addCapital(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de capital.");
		}

		model.addAttribute("command", new AddCapitalView(entreprise.getNumero(), RegDate.get(), null, null, null));
		return "donnees-civiles/add-capital";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/add.do", method = RequestMethod.POST)
	public String addCapital(@Valid @ModelAttribute("command") final AddCapitalView view, BindingResult result, Model model) throws Exception {

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de capital.");
		}

		if (result.hasErrors()) {
			model.addAttribute("command", view);
			return "donnees-civiles/add-capital";
		}

		tiersService.addCapitalFiscal(entreprise, view.getMontant(), view.getMonnaie(), view.getDateDebut(), view.getDateFin());

		return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
	}

	@RequestMapping(value = "/capital/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editCapital(@RequestParam(value = "capitalId", required = true) long capitalId, Model model) {

		final CapitalFiscalEntreprise capital = hibernateTemplate.get(CapitalFiscalEntreprise.class, capitalId);
		if (capital == null) {
			throw new ObjectNotFoundException("Le capital avec l'id = " + capitalId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(capital.getEntreprise());
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de capitaux.");
		}

		model.addAttribute("command", new EditCapitalView(capital));
		return "donnees-civiles/edit-capital";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/edit.do", method = RequestMethod.POST)
	public String editCapital(@Valid @ModelAttribute("command") final EditCapitalView view, BindingResult result, Model model) throws Exception {

		final CapitalFiscalEntreprise capital = hibernateTemplate.get(CapitalFiscalEntreprise.class, view.getId());
		if (capital == null) {
			throw new ObjectNotFoundException("Le capital avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Entreprise entreprise = capital.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);

		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de capitaux.");
		}

		if (capital.getMontant() != null && ! (capital.getMontant().getMontant().equals(view.getMontant()) && capital.getMontant().getMonnaie().equals(view.getMonnaie()))) {

			final long ctbId = entreprise.getNumero();
			controllerUtils.checkAccesDossierEnEcriture(ctbId);

			if (result.hasErrors()) {
				return "donnees-civiles/edit-capital";
			}

			tiersService.updateCapitalFiscal(capital, view.getMontant(), view.getDateFin());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/cancel.do", method = RequestMethod.POST)
	public String cancelCapital(long capitalId) throws Exception {

		final CapitalFiscalEntreprise capital = hibernateTemplate.get(CapitalFiscalEntreprise.class, capitalId);
		if (capital == null) {
			throw new ObjectNotFoundException("Le capital n°" + capitalId + " n'existe pas.");
		}
		final Entreprise entreprise = capital.getEntreprise();

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de capitaux.");
		}

		tiersService.annuleCapitalFiscal(capital);

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getId();// + "&highlightFor=" + formeJuridique.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.GET)
	public String editIdeEntreprise(Model model, @RequestParam(value = ID) long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
			final ContribuableInfosEntrepriseView view = new ContribuableInfosEntrepriseView((Entreprise) tiers);
			model.addAttribute(DATA, view);
			model.addAttribute(TIERS_ID, id);
			return "/tiers/edition/civil/edit-ide";
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.POST)
	public String editIdeEntreprise(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) ContribuableInfosEntrepriseView view, BindingResult bindingResult) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
		}
		else {
			throw new TiersNotFoundException(id);
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute(DATA, view);
			model.addAttribute(TIERS_ID, id);
			return "/tiers/edition/civil/edit-ide";
		}

		tiersService.setIdentifiantEntreprise((Entreprise) tiers, StringUtils.trimToNull(view.getIde()));

		return "redirect:/civil/entreprise/edit.do?id=" + id;
	}
}
