package ch.vd.unireg.entreprise;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

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
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.common.LiteralStringHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.CapitalFiscalEntreprise;
import ch.vd.unireg.tiers.DegreAssociationRegistreCivil;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.tiers.validator.ContribuableInfosEntrepriseViewValidator;
import ch.vd.unireg.tiers.view.ContribuableInfosEntrepriseView;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.utils.RegDateEditor;

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
			addSubValidator(RaisonSocialeView.Add.class, new RaisonSocialeViewValidator());
			addSubValidator(RaisonSocialeView.Edit.class, new RaisonSocialeViewValidator());
			addSubValidator(FormeJuridiqueView.Add.class, new FormeJuridiqueViewValidator());
			addSubValidator(FormeJuridiqueView.Edit.class, new FormeJuridiqueViewValidator());
			addSubValidator(CapitalView.Add.class, new CapitalViewValidator());
			addSubValidator(CapitalView.Edit.class, new CapitalViewValidator());
			addSubValidator(ContribuableInfosEntrepriseView.class, new ContribuableInfosEntrepriseViewValidator());
			addSubValidator(SiegeView.Add.class, new SiegeViewValidator());
			addSubValidator(SiegeView.Edit.class, new SiegeViewValidator());
			addSubValidator(EditSecteurActiviteEntrepriseView.class, new EditSecteurActiviteEntrepriseViewValidator());
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
		if (tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
			checkEditionAutorisee((Entreprise) tiers);
			final EntrepriseView view = entrepriseService.getEntreprise((Entreprise) tiers);
			return showEditEntreprise(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private void checkEditionAutorisee(Entreprise entreprise) {
		tiersService.checkEditionCivileAutorisee(entreprise);
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

		return showAddRaisonSociale(model, new RaisonSocialeView.Add(entreprise.getNumero(), RegDate.get(), null, null));
	}

	private String showAddRaisonSociale(Model model, RaisonSocialeView.Add view) {
		model.addAttribute("command", view);
		return "donnees-civiles/add-raison-sociale";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonsociale/add.do", method = RequestMethod.POST)
	public String addRaisonSociale(@Valid @ModelAttribute("command") final RaisonSocialeView.Add view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showAddRaisonSociale(model, view);
		}

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de raison sociale.");
		}

		tiersService.addRaisonSocialeFiscale(entreprise, LiteralStringHelper.stripExtraSpacesAndBlanks(view.getRaisonSociale()), view.getDateDebut(), view.getDateFin());

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

		return showEditRaisonSociale(model, new RaisonSocialeView.Edit(raisonSociale));
	}

	private String showEditRaisonSociale(Model model, RaisonSocialeView.Edit view) {
		model.addAttribute("command", view);
		return "donnees-civiles/edit-raison-sociale";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonsociale/edit.do", method = RequestMethod.POST)
	public String editRaisonSociale(@Valid @ModelAttribute("command") final RaisonSocialeView.Edit view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showEditRaisonSociale(model, view);
		}

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

			tiersService.updateRaisonSocialeFiscale(raisonSociale, view.getRaisonSociale());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancelRaisonSociale(long raisonSocialeId) throws TiersException {

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

		return showAddFormeJuridique(model, new FormeJuridiqueView.Add(entreprise.getNumero(), RegDate.get(), null, null));
	}

	private String showAddFormeJuridique(Model model, FormeJuridiqueView.Add view) {
		model.addAttribute("command", view);
		model.addAttribute(FORMES_JURIDIQUES_ENTREPRISE_NAME, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		return "donnees-civiles/add-forme-juridique";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/add.do", method = RequestMethod.POST)
	public String addFormeJuridique(@Valid @ModelAttribute("command") final FormeJuridiqueView.Add view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showAddFormeJuridique(model, view);
		}

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de forme juridique.");
		}

		// on ajoute la forme juridique demandée
		tiersService.addFormeJuridiqueFiscale(entreprise, view.getFormeJuridique(), view.getDateDebut(), view.getDateFin());

		final DegreAssociationRegistreCivil degre = tiersService.determineDegreAssociationCivil(entreprise, view.getDateDebut());
		if (degre == DegreAssociationRegistreCivil.CIVIL_ESCLAVE) {
			// [SIFISC-22479] si l'entreprise ne peut plus être éditée fiscalement, on redirige vers la page de visulation
			return "redirect:/tiers/visu.do?id=" + tiersId;
		}
		else {
			// autrement, on continue l'édition
			return "redirect:/civil/entreprise/edit.do?id=" + tiersId;// + buildHighlightForParam(newFor); plus tard
		}
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

		return showEditFormeJuridique(model, new FormeJuridiqueView.Edit(formeJuridique));
	}

	private String showEditFormeJuridique(Model model, FormeJuridiqueView.Edit view) {
		model.addAttribute("command", view);
		model.addAttribute(FORMES_JURIDIQUES_ENTREPRISE_NAME, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		return "donnees-civiles/edit-forme-juridique";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/edit.do", method = RequestMethod.POST)
	public String editFormeJuridique(@Valid @ModelAttribute("command") final FormeJuridiqueView.Edit view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showEditFormeJuridique(model, view);
		}

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
			tiersService.updateFormeJuridiqueFiscale(formeJuridique, view.getFormeJuridique());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/formejuridique/cancel.do", method = RequestMethod.POST)
	public String cancelFormeJuridique(long formeJuridiqueId) throws TiersException {

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

		return showAddCapital(model, new CapitalView.Add(entreprise.getNumero(), RegDate.get(), null, null, MontantMonetaire.CHF));
	}

	private String showAddCapital(Model model, CapitalView.Add view) {
		model.addAttribute("command", view);
		return "donnees-civiles/add-capital";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/add.do", method = RequestMethod.POST)
	public String addCapital(@Valid @ModelAttribute("command") final CapitalView.Add view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showAddCapital(model, view);
		}

		final long tiersId = view.getTiersId();

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new ObjectNotFoundException("L'entreprise avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de capital.");
		}

		tiersService.addCapitalFiscal(entreprise, view.getMontant(), StringUtils.upperCase(view.getMonnaie()), view.getDateDebut(), view.getDateFin());

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

		return showEditCapital(model, new CapitalView.Edit(capital));
	}

	private String showEditCapital(Model model, CapitalView.Edit view) {
		model.addAttribute("command", view);
		return "donnees-civiles/edit-capital";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/edit.do", method = RequestMethod.POST)
	public String editCapital(@Valid @ModelAttribute("command") final CapitalView.Edit view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showEditCapital(model, view);
		}

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
			tiersService.updateCapitalFiscal(capital, view.getMontant(), view.getMonnaie(), view.getDateFin());
		}

		return "redirect:/civil/entreprise/edit.do?id=" + entreprise.getNumero();// + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/capital/cancel.do", method = RequestMethod.POST)
	public String cancelCapital(long capitalId) throws TiersException {

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
		if (tiers instanceof Entreprise) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
			return showEditIdeEntreprise(model, new ContribuableInfosEntrepriseView((Entreprise) tiers), id);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private String showEditIdeEntreprise(Model model, ContribuableInfosEntrepriseView view, long id) {
		model.addAttribute(DATA, view);
		model.addAttribute(TIERS_ID, id);
		return "/tiers/edition/civil/edit-ide";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.POST)
	public String editIdeEntreprise(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) ContribuableInfosEntrepriseView view, BindingResult bindingResult) throws TiersException {

		if (bindingResult.hasErrors()) {
			return showEditIdeEntreprise(model, view, id);
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers instanceof Entreprise) {

			Entreprise entreprise = (Entreprise) tiers;
			final Set<IdentificationEntreprise> identificationsEntreprise = entreprise.getIdentificationsEntreprise();
			boolean ideVide = identificationsEntreprise == null || identificationsEntreprise.isEmpty();
			if (ideVide && tiersService.determineDegreAssociationCivil(entreprise, RegDate.get()) == DegreAssociationRegistreCivil.CIVIL_ESCLAVE) {
				throw new AccessDeniedException("Le numéro IDE de l'entreprise ne peut être édité. Il est fourni par le registre civil.");
			}

			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
			}
		}
		else {
			throw new TiersNotFoundException(id);
		}

		tiersService.setIdentifiantEntreprise((Entreprise) tiers, StringUtils.trimToNull(view.getIde()));

		return "redirect:/civil/entreprise/edit.do?id=" + id;
	}

	/* Siège */

	@RequestMapping(value = "/siege/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addSiege(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(tiersId);
		if (entreprise == null) {
			throw new TiersNotFoundException(tiersId);
		}
		final List<DateRanged<Etablissement>> etablissementsPrincipauxEntreprise = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
		if (etablissementsPrincipauxEntreprise.isEmpty() || CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise) == null) {
			throw new TiersNotFoundException(entreprise.getNumero());
		}
		final DateRanged<Etablissement> etablissementPrincipalRange = CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise);
		final Etablissement etablissement = etablissementPrincipalRange.getPayload();

		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de sieges.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		return showAddSiege(model, new SiegeView.Add(etablissement.getNumero(), entreprise.getNumero(), RegDate.get(), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));
	}

	private String showAddSiege(Model model, SiegeView.Add view) {
		model.addAttribute("typesDomicileFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("command", view);
		return "donnees-civiles/add-siege";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/siege/add.do", method = RequestMethod.POST)
	public String addSiege(@Valid @ModelAttribute("command") final SiegeView.Add view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showAddSiege(model, view);
		}

		final long tiersId = view.getEtablissementId();

		final Etablissement etablissement = (Etablissement) tiersDAO.get(tiersId);
		if (etablissement == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de sieges.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		tiersService.addDomicileFiscal(etablissement, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale(), view.getDateDebut(), view.getDateFin());

		return "redirect:/civil/entreprise/edit.do?id=" + view.getEntrepriseId();
	}

	@RequestMapping(value = "/siege/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editSiege(@RequestParam(value = "domicileId", required = true) long domicileId, @RequestParam(value = "entrepriseId", required = true) long entrepriseId, @RequestParam(value = "peutEditerDateFin", required = true) boolean peutEditerDateFin, Model model) {

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, domicileId);
		if (domicile == null) {
			throw new ObjectNotFoundException("Le siege avec l'id = " + domicileId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(domicile.getEtablissement());
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de sieges.");
		}
		controllerUtils.checkAccesDossierEnEcriture(domicile.getEtablissement().getNumero());
		return showEditSiege(model, new SiegeView.Edit(domicile, entrepriseId, peutEditerDateFin));
	}

	private String showEditSiege(Model model, SiegeView.Edit view) {
		model.addAttribute("command", view);
		model.addAttribute("typesDomicileFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		return "donnees-civiles/edit-siege";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/siege/edit.do", method = RequestMethod.POST)
	public String editSiege(@Valid @ModelAttribute("command") final SiegeView.Edit view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showEditSiege(model, view);
		}

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, view.getId());
		if (domicile == null) {
			throw new ObjectNotFoundException("Le siege avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Etablissement etablissement = domicile.getEtablissement();

		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de sieges.");
		}

		if (!domicile.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale()) || domicile.getDateFin() != view.getDateFin()) {

			final long ctbId = etablissement.getNumero();
			controllerUtils.checkAccesDossierEnEcriture(ctbId);

			final RegDate dateFermeture = view.getDateFin();
			if (dateFermeture == domicile.getDateFin()) {
				tiersService.updateDomicileFiscal(domicile, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());
			}
			else if (domicile.getDateFin() == null
					&& domicile.getTypeAutoriteFiscale() == view.getTypeAutoriteFiscale()
					&& domicile.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale())) {
				tiersService.closeDomicileEtablissement(domicile, dateFermeture);
			}
			else {
				tiersService.updateDomicileFiscal(domicile, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale(), view.getDateFin());
			}
		}

		return "redirect:/civil/entreprise/edit.do?id=" + view.getEntrepriseId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/siege/cancel.do", method = RequestMethod.POST)
	public String cancelSiege(long domicileId, long entrepriseId) throws TiersException {

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, domicileId);
		if (domicile == null) {
			throw new ObjectNotFoundException("Le siege avec l'id = " + domicileId + " n'existe pas.");
		}
		final Etablissement etablissement = domicile.getEtablissement();

		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de sieges.");
		}

		controllerUtils.checkAccesDossierEnEcriture(etablissement.getNumero());

		tiersService.annuleDomicileFiscal(domicile);

		return "redirect:/civil/entreprise/edit.do?id=" + entrepriseId;
	}

		/* Secteur d'activité */

	@RequestMapping(value = "/secteuractivite/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editSecteurActivite(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (!(tiers instanceof Entreprise)) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
		}

		return showSecteurActivite(model, new EditSecteurActiviteEntrepriseView((Entreprise) tiers));
	}

	private String showSecteurActivite(Model model, EditSecteurActiviteEntrepriseView view) {
		model.addAttribute("command", view);
		return "donnees-civiles/edit-secteur-activite";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/secteuractivite/edit.do", method = RequestMethod.POST)
	public String editSecteurActivite(@Valid @ModelAttribute("command") final EditSecteurActiviteEntrepriseView view, BindingResult result, Model model) throws Exception {

		if (result.hasErrors()) {
			return showSecteurActivite(model, view);
		}

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (!(tiers instanceof Entreprise)) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		final Entreprise entreprise = (Entreprise) tiers;
		final Autorisations auth = getAutorisations(entreprise);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'entreprises.");
		}

		checkEditionAutorisee((Entreprise) tiers);

		entreprise.changeSecteurActivite(LiteralStringHelper.stripExtraSpacesAndBlanks(view.getSecteurActivite()));
		return "redirect:/civil/entreprise/edit.do?id=" + tiers.getNumero();
	}

}
