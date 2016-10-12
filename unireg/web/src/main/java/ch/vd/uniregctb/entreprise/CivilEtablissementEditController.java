package ch.vd.uniregctb.entreprise;

import javax.validation.Valid;
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
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.DegreAssociationRegistreCivil;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.ContribuableInfosEntrepriseViewValidator;
import ch.vd.uniregctb.tiers.view.ContribuableInfosEntrepriseView;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/civil/etablissement")
public class CivilEtablissementEditController {

	private static final String ID = "id";

	private static final String TIERS_ID = "tiersId";
	private static final String DATA = "data";

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AutorisationManager autorisationManager;
	private EntrepriseService entrepriseService;
	private TiersMapHelper tiersMapHelper;
	private ControllerUtils controllerUtils;
	private HibernateTemplate hibernateTemplate;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEntrepriseService(EntrepriseService entrepriseService) {
		this.entrepriseService = entrepriseService;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	private static class CivilEtablissementEditValidator extends DelegatingValidator {
		private CivilEtablissementEditValidator() {
			addSubValidator(EtablissementView.class, new DummyValidator<>(EtablissementView.class));
			addSubValidator(RaisonSocialeView.class, new RaisonSocialeViewValidator());
			addSubValidator(EditRaisonEnseigneEtablissementView.class, new EditRaisonEnseigneEtablissementViewValidator());
			addSubValidator(ContribuableInfosEntrepriseView.class, new ContribuableInfosEntrepriseViewValidator());
			addSubValidator(DomicileView.Add.class, new DomicileViewValidator());
			addSubValidator(DomicileView.Edit.class, new DomicileViewValidator());
		}
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new CivilEtablissementEditValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	private Autorisations getAutorisations(Tiers tiers) {
		return autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editEtablissement(Model model, @RequestParam(value = ID) long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Etablissement) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
			}
			checkEditionAutorisee((Etablissement) tiers);
			final EtablissementView view = entrepriseService.getEtablissement((Etablissement) tiers);
			return showEditEtablissement(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private void checkEditionAutorisee(Etablissement etablissement) {
		tiersService.checkEditionCivileAutorisee(etablissement);
	}

	private String showEditEtablissement(Model model, long id, EtablissementView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(TIERS_ID, id);
		return "/tiers/edition/civil/edit-etablissement";
	}

	/* Raison sociale et enseigne */

	@RequestMapping(value = "/raisonenseigne/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editRaisonSociale(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
		}

		return showRaisonSociale(model, new EditRaisonEnseigneEtablissementView((Etablissement) tiers));
	}

	private String showRaisonSociale(Model model, EditRaisonEnseigneEtablissementView view) {
		model.addAttribute("command", view);
		return "donnees-civiles/edit-raison-enseigne";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonenseigne/edit.do", method = RequestMethod.POST)
	public String editRaisonSociale(@Valid @ModelAttribute("command") final EditRaisonEnseigneEtablissementView view, BindingResult result, Model model) throws Exception {

		if (result.hasErrors()) {
			return showRaisonSociale(model, view);
		}

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		final Etablissement etablissement = (Etablissement) tiers;
		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
		}

		checkEditionAutorisee((Etablissement) tiers);

		etablissement.setRaisonSociale(view.getRaisonSociale());
		etablissement.setEnseigne(view.getEnseigne());
		return "redirect:/civil/etablissement/edit.do?id=" + tiers.getNumero();
	}


	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.GET)
	public String editIdeEtablissement(Model model, @RequestParam(value = ID) long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
		}

		return showEditIdeEtablissement(model, new ContribuableInfosEntrepriseView((Etablissement) tiers), id);
	}

	private String showEditIdeEtablissement(Model model, ContribuableInfosEntrepriseView view, long tiersId) {
		model.addAttribute(DATA, view);
		model.addAttribute(TIERS_ID, tiersId);
		return "/tiers/edition/civil/edit-ide";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.POST)
	public String editIdeEtablissement(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) ContribuableInfosEntrepriseView view, BindingResult bindingResult) throws TiersException {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(id);
		}
		Etablissement etablissement = (Etablissement) tiers;
		final Set<IdentificationEntreprise> identificationsEntreprise = etablissement.getIdentificationsEntreprise();
		boolean ideVide = identificationsEntreprise == null || identificationsEntreprise.isEmpty();
		if (ideVide && tiersService.determineDegreAssociationCivil(etablissement, RegDate.get()) == DegreAssociationRegistreCivil.CIVIL_ESCLAVE) {
			throw new AccessDeniedException("Le numéro IDE de l'établissement ne peut être édité. Il est fourni par le registre civil.");
		}

		if (bindingResult.hasErrors()) {
			return showEditIdeEtablissement(model, view, id);
		}

		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {      // FIXME: Et aussi IDE?
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
		}
		tiersService.setIdentifiantEntreprise((Etablissement) tiers, StringUtils.trimToNull(view.getIde()));

		return "redirect:/civil/etablissement/edit.do?id=" + id;
	}

	/* Domicile */

	@RequestMapping(value = "/domicile/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addDomicile(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(tiersId);
		}

		final Etablissement etablissement = (Etablissement) tiers;
		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de domiciles.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		return showAddDomicile(model, new DomicileView.Add(etablissement.getNumero(), RegDate.get(), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));
	}

	private String showAddDomicile(Model model, DomicileView.Add view) {
		model.addAttribute("typesDomicileFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("command", view);
		return "donnees-civiles/add-domicile";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/domicile/add.do", method = RequestMethod.POST)
	public String addDomicile(@Valid @ModelAttribute("command") final DomicileView.Add view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showAddDomicile(model, view);
		}

		final Long tiersId = view.getTiersId();
		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null || !(tiers instanceof Etablissement)) {
			throw new TiersNotFoundException(tiersId);
		}

		final Etablissement etablissement = (Etablissement) tiers;
		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de domiciles.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		tiersService.addDomicileFiscal(etablissement, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale(), view.getDateDebut(), view.getDateFin());
		return "redirect:/civil/etablissement/edit.do?id=" + tiersId;
	}

	@RequestMapping(value = "/domicile/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editDomicile(@RequestParam(value = "domicileId", required = true) long domicileId, @RequestParam(value = "peutEditerDateFin", required = true) boolean peutEditerDateFin, Model model) {

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, domicileId);
		if (domicile == null) {
			throw new ObjectNotFoundException("Le domicile avec l'id = " + domicileId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(domicile.getEtablissement());
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de domiciles.");
		}
		controllerUtils.checkAccesDossierEnEcriture(domicile.getEtablissement().getNumero());
		return showEditDomicile(model, new DomicileView.Edit(domicile, peutEditerDateFin));
	}

	private String showEditDomicile(Model model, DomicileView.Edit view) {
		model.addAttribute("command", view);
		model.addAttribute("typesDomicileFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		return "donnees-civiles/edit-domicile";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/domicile/edit.do", method = RequestMethod.POST)
	public String editDomicile(@Valid @ModelAttribute("command") final DomicileView.Edit view, BindingResult result, Model model) throws TiersException {

		if (result.hasErrors()) {
			return showEditDomicile(model, view);
		}

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, view.getId());
		if (domicile == null) {
			throw new ObjectNotFoundException("Le domicile avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Etablissement etablissement = domicile.getEtablissement();
		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de domiciles.");
		}

		if (!domicile.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale()) || domicile.getDateFin() != view.getDateFin()) {

			final long ctbId = etablissement.getNumero();
			controllerUtils.checkAccesDossierEnEcriture(ctbId);

			final RegDate dateFermeture = view.getDateFin();
			if (dateFermeture == domicile.getDateFin()) {
				tiersService.updateDomicileFiscal(domicile, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());
			}
			else if (domicile.getDateFin() == null
					&& domicile.getTypeAutoriteFiscale().equals(view.getTypeAutoriteFiscale())
					&& domicile.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale())) {
				tiersService.closeDomicileEtablissement(domicile, dateFermeture);
			}
			else {
				tiersService.updateDomicileFiscal(domicile, view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale(), view.getDateFin());
			}
		}

		return "redirect:/civil/etablissement/edit.do?id=" + etablissement.getNumero();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/domicile/cancel.do", method = RequestMethod.POST)
	public String cancelDomicile(long domicileId) throws TiersException {

		final DomicileEtablissement domicile = hibernateTemplate.get(DomicileEtablissement.class, domicileId);
		if (domicile == null) {
			throw new ObjectNotFoundException("Le domicile avec l'id = " + domicileId + " n'existe pas.");
		}
		final Etablissement etablissement = domicile.getEtablissement();

		final Autorisations auth = getAutorisations(etablissement);
		if (!auth.isDonneesCiviles()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de suppression de domiciles.");
		}

		controllerUtils.checkAccesDossierEnEcriture(etablissement.getNumero());

		tiersService.annuleDomicileFiscal(domicile);

		return "redirect:/civil/etablissement/edit.do?id=" + etablissement.getId();
	}

}
