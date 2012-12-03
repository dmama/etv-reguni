package ch.vd.uniregctb.complements;

import javax.validation.Valid;

import org.springframework.orm.hibernate3.HibernateTemplate;
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

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;

@Controller
@RequestMapping(value = "/complements")
public class ComplementsController {

	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;
	private ControllerUtils controllerUtils;
	private Validator validator;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);


		final ComplementsEditView view = new ComplementsEditView(tiers);
		model.addAttribute("command", view);

		return "complements/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	public String edit(@Valid @ModelAttribute("command") final ComplementsEditView view, BindingResult result, Model model) throws Exception {

		final long id = view.getId();
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);

		if (result.hasErrors()) {
			model.addAttribute("modifie", true);
			return "complements/edit";
		}

		// On met-à-jour les données.
		if (autorisations.isComplementsCommunications()) {
			if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				if (dpi.getContribuableId() == null) {
					dpi.setNom1(view.getNom1());
					dpi.setNom2(view.getNom2());
				}
			}
			tiers.setPersonneContact(view.getPersonneContact());
			tiers.setComplementNom(view.getComplementNom());
			tiers.setNumeroTelephonePrive(view.getNumeroTelephonePrive());
			tiers.setNumeroTelephonePortable(view.getNumeroTelephonePortable());
			tiers.setNumeroTelephoneProfessionnel(view.getNumeroTelephoneProfessionnel());
			tiers.setNumeroTelecopie(view.getNumeroTelecopie());
			tiers.setAdresseCourrierElectronique(view.getAdresseCourrierElectronique());
		}

		if (autorisations.isComplementsCoordonneesFinancieres()) {
			tiers.setNumeroCompteBancaire(view.getIban());
			tiers.setTitulaireCompteBancaire(view.getTitulaireCompteBancaire());
			tiers.setAdresseBicSwift(view.getAdresseBicSwift());
		}

		return "redirect:/tiers/visu.do?id=" + id;
	}
}
