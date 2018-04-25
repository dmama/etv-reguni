package ch.vd.unireg.complements;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;

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

	@RequestMapping(value = "/communications/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editCommunications(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);


		final ComplementsEditCommunicationsView view = new ComplementsEditCommunicationsView(tiers);
		model.addAttribute("command", view);

		return "complements/communications/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/communications/edit.do", method = RequestMethod.POST)
	public String editCommunications(@Valid @ModelAttribute("command") final ComplementsEditCommunicationsView view, BindingResult result, Model model) throws Exception {

		final long id = view.getId();
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements() || !autorisations.isComplementsCommunications()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des points de communication d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);

		if (result.hasErrors()) {
			model.addAttribute("modifie", true);
			return "complements/communications/edit";
		}

		// On met-à-jour les données.
		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			if (dpi.getContribuableId() == null) {
				dpi.setNom1(StringUtils.trimToNull(view.getNom1()));
				dpi.setNom2(StringUtils.trimToNull(view.getNom2()));
			}
		}
		tiers.setPersonneContact(StringUtils.trimToNull(view.getPersonneContact()));
		tiers.setComplementNom(StringUtils.trimToNull(view.getComplementNom()));
		tiers.setNumeroTelephonePrive(StringUtils.trimToNull(view.getNumeroTelephonePrive()));
		tiers.setNumeroTelephonePortable(StringUtils.trimToNull(view.getNumeroTelephonePortable()));
		tiers.setNumeroTelephoneProfessionnel(StringUtils.trimToNull(view.getNumeroTelephoneProfessionnel()));
		tiers.setNumeroTelecopie(StringUtils.trimToNull(view.getNumeroTelecopie()));
		tiers.setAdresseCourrierElectronique(StringUtils.trimToNull(view.getAdresseCourrierElectronique()));

		return "redirect:/tiers/visu.do?id=" + id;
	}

	@RequestMapping(value = "/coordfinancieres/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editCoordFinancieres(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);


		final ComplementsEditCoordonneesFinancieresView view = new ComplementsEditCoordonneesFinancieresView(tiers);
		model.addAttribute("command", view);

		return "complements/coordfinancieres/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/coordfinancieres/edit.do", method = RequestMethod.POST)
	public String editCoordFinancieres(@Valid @ModelAttribute("command") final ComplementsEditCoordonneesFinancieresView view, BindingResult result, Model model) throws Exception {

		final long id = view.getId();
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements() || !autorisations.isComplementsCoordonneesFinancieres()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des coordonnées financières d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(id);

		if (result.hasErrors()) {
			model.addAttribute("modifie", true);
			return "complements/coordfinancieres/edit";
		}

		// On met-à-jour les données.
		final String iban = IbanHelper.normalize(view.getIban());
		final String bicSwift = StringUtils.trimToNull(FormatNumeroHelper.removeSpaceAndDash(view.getAdresseBicSwift()));
// FIXME (msi) SIFISC-20035 : implémenter les écrans d'édition des coordonnées bancaires
//		if (iban != null || bicSwift != null) {
//			tiers.setCompteBancaire(new CompteBancaire(iban, bicSwift));
//		}
//		else {
//			tiers.setCompteBancaire(null);
//		}
//		tiers.setTitulaireCompteBancaire(StringUtils.trimToNull(view.getTitulaireCompteBancaire()));

		return "redirect:/tiers/visu.do?id=" + id;
	}
}
