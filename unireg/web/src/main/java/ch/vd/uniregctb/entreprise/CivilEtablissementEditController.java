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
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.ContribuableInfosEntrepriseViewValidator;
import ch.vd.uniregctb.tiers.view.ContribuableInfosEntrepriseView;
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

	private static class CivilEditValidator extends DelegatingValidator {
		private CivilEditValidator() {
			addSubValidator(EtablissementView.class, new DummyValidator<>(EtablissementView.class));
			addSubValidator(AddRaisonSocialeView.class, new AddRaisonSocialeViewValidator());
			addSubValidator(EditRaisonEnseigneEtablissementView.class, new EditRaisonEnseigneEtablissementViewValidator());
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
		if (etablissement.isConnuAuCivil()) {
			throw new AccessDeniedException("Il n'est pas possible d'éditer les établissements connus au civil.");
		}
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
		if (tiers != null && tiers instanceof Etablissement) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
			}
			model.addAttribute("command", new EditRaisonEnseigneEtablissementView((Etablissement) tiers));
		} else {
			throw new TiersNotFoundException(tiersId);
		}

		return "donnees-civiles/edit-raison-enseigne";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/raisonenseigne/edit.do", method = RequestMethod.POST)
	public String editRaisonSociale(@Valid @ModelAttribute("command") final EditRaisonEnseigneEtablissementView view, BindingResult result, Model model) throws Exception {

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (tiers != null && tiers instanceof Etablissement) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
			}
			checkEditionAutorisee((Etablissement) tiers);
			if (result.hasErrors()) {
				return "donnees-civiles/edit-raison-enseigne";
			}
			Etablissement etablissement = (Etablissement) tiers;
			etablissement.setRaisonSociale(view.getRaisonSociale());
			etablissement.setEnseigne(view.getEnseigne());
		} else {
			throw new TiersNotFoundException(view.getTiersId());
		}

		return "redirect:/civil/etablissement/edit.do?id=" + tiers.getNumero();// + buildHighlightForParam(newFor);
	}


	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/ide/edit.do", method = RequestMethod.GET)
	public String editIdeEtablissement(Model model, @RequestParam(value = ID) long id) {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Etablissement) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
			}
			final ContribuableInfosEntrepriseView view = new ContribuableInfosEntrepriseView((Etablissement) tiers);
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
	public String editIdeEtablissement(@RequestParam(value = ID) long id, Model model, @Valid @ModelAttribute(DATA) ContribuableInfosEntrepriseView view, BindingResult bindingResult) throws
			TiersException {

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null && tiers instanceof Etablissement) {
			final Autorisations auth = getAutorisations(tiers);
			if (!auth.isDonneesCiviles() || !auth.isIdentificationEntreprise()) {      // FIXME: Et aussi IDE?
				throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition d'etablissements.");
			}
			if (bindingResult.hasErrors()) {
				model.addAttribute(DATA, view);
				model.addAttribute(TIERS_ID, id);
				return "/tiers/edition/civil/edit-ide";
			}
			tiersService.setIdentifiantEntreprise((Etablissement) tiers, StringUtils.trimToNull(view.getIde()));
		}
		else {
			throw new TiersNotFoundException(id);
		}

		return "redirect:/civil/etablissement/edit.do?id=" + id;
	}

}
