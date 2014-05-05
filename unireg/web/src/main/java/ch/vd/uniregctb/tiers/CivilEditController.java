package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.AutreCommunauteCivilViewValidator;
import ch.vd.uniregctb.tiers.validator.NonHabitantCivilViewValidator;
import ch.vd.uniregctb.tiers.view.AutreCommunauteCivilView;
import ch.vd.uniregctb.tiers.view.NonHabitantCivilView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/civil")
public class CivilEditController {

	private static final String ID = "id";

	private static final String FORME_JURIDIQUE_MAP_NAME = "formesJuridiques";
	private static final String SEXE_MAP_NAME = "sexes";
	private static final String CATEGORIE_ETRANGER_MAP_NAME = "categoriesEtrangers";
	private static final String TIERS_ID = "tiersId";
	private static final String DATA = "data";

	private TiersDAO tiersDAO;
	private TiersMapHelper tiersMapHelper;
	private ServiceInfrastructureService infraService;
	private SecurityProviderInterface securityProvider;
	private AutorisationManager autorisationManager;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	private static class CivilEditValidator extends DelegatingValidator {
		private CivilEditValidator() {
			addSubValidator(AutreCommunauteCivilView.class, new AutreCommunauteCivilViewValidator());
			addSubValidator(NonHabitantCivilView.class, new NonHabitantCivilViewValidator());
		}
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new CivilEditValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));      // la date de naissance doit supporter les dates partielles
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

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autrecommunaute/edit.do", method = RequestMethod.GET)
	public String editAutreCommunaute(Model model, @RequestParam(value = ID) long id) {
		if (!SecurityHelper.isGranted(securityProvider, Role.MODIF_AC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des tiers de ce type.");
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof AutreCommunaute) {
			checkDroitEditionDonneesCiviles(tiers);
			final AutreCommunauteCivilView view = new AutreCommunauteCivilView((AutreCommunaute) tiers);
			return showEditAutreCommunaute(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private String showEditAutreCommunaute(Model model, long id, AutreCommunauteCivilView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(FORME_JURIDIQUE_MAP_NAME, tiersMapHelper.getMapFormeJuridique());
		model.addAttribute(TIERS_ID, id);
		return "/tiers/edition/civil/edit-organisation";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autrecommunaute/edit.do", method = RequestMethod.POST)
	public String doEditAutreCommunaute(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) AutreCommunauteCivilView view, BindingResult bindingResult) {
		if (!SecurityHelper.isGranted(securityProvider, Role.MODIF_AC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des tiers de ce type.");
		}

		if (bindingResult.hasErrors()) {
			return showEditAutreCommunaute(model, id, view);
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof AutreCommunaute) {
			checkDroitEditionDonneesCiviles(tiers);

			final AutreCommunaute ac = (AutreCommunaute) tiers;
			ac.setFormeJuridique(view.getFormeJuridique());
			ac.setNom(view.getNom());

			final String ide = NumeroIDEHelper.normalize(view.getIde());
			if (StringUtils.isBlank(ide)) {
				ac.setIdentificationsEntreprise(Collections.<IdentificationEntreprise>emptySet());
			}
			else {
				final IdentificationEntreprise ie = new IdentificationEntreprise();
				ie.setNumeroIde(ide);
				ie.setCtb(ac);
				ac.setIdentificationsEntreprise(new HashSet<>(Arrays.<IdentificationEntreprise>asList(ie)));
			}
		}
		else {
			throw new TiersNotFoundException(id);
		}

		return "redirect:/tiers/visu.do?id=" + id;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/nonhabitant/edit.do", method = RequestMethod.GET)
	public String editNonHabitant(Model model, @RequestParam(value = ID) long id) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_HC_HS, Role.MODIF_NONHAB_DEBPUR, Role.MODIF_NONHAB_INACTIF)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des tiers de ce type.");
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof PersonnePhysique && !((PersonnePhysique) tiers).isHabitantVD()) {
			checkDroitEditionDonneesCiviles(tiers);
			final NonHabitantCivilView view = new NonHabitantCivilView(infraService, (PersonnePhysique) tiers);
			return showEditNonHabitant(model, id, view);
		}
		else {
			throw new TiersNotFoundException(id);
		}
	}

	private String showEditNonHabitant(Model model, long id, NonHabitantCivilView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(TIERS_ID, id);
		model.addAttribute(SEXE_MAP_NAME, tiersMapHelper.getMapSexe());
		model.addAttribute(CATEGORIE_ETRANGER_MAP_NAME, tiersMapHelper.getMapCategorieEtranger());
		return "/tiers/edition/civil/edit-non-habitant";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/nonhabitant/edit.do", method = RequestMethod.POST)
	public String doEditNonHabitant(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) NonHabitantCivilView view, BindingResult bindingResult) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_HC_HS, Role.MODIF_NONHAB_DEBPUR, Role.MODIF_NONHAB_INACTIF)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la modification des tiers de ce type.");
		}

		if (bindingResult.hasErrors()) {
			return showEditNonHabitant(model, id, view);
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof PersonnePhysique && !((PersonnePhysique) tiers).isHabitantVD()) {
			checkDroitEditionDonneesCiviles(tiers);

			final PersonnePhysique pp = (PersonnePhysique) tiers;
			pp.setNom(view.getNom());
			pp.setPrenom(view.getPrenom());
			pp.setNumeroAssureSocial(view.getNumeroAssureSocial());
			pp.setSexe(view.getSexe());
			pp.setDateNaissance(view.getDateNaissance());
			pp.setCategorieEtranger(view.getCategorieEtranger());
			pp.setDateDebutValiditeAutorisation(view.getDateDebutValiditeAutorisation());
			pp.setNumeroOfsNationalite(view.getNumeroOfsNationalite());
			pp.setLibelleCommuneOrigine(view.getLibelleCommuneOrigine());
			pp.setPrenomsPere(view.getPrenomsPere());
			pp.setNomPere(view.getNomPere());
			pp.setPrenomsMere(view.getPrenomsMere());
			pp.setNomMere(view.getNomMere());
		}
		else {
			throw new TiersNotFoundException(id);
		}

		return "redirect:/tiers/visu.do?id=" + id;
	}
}
