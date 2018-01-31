package ch.vd.uniregctb.tiers;

import javax.validation.Valid;

import org.jetbrains.annotations.NotNull;
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
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.CloseActiviteEconomiqueValidator;
import ch.vd.uniregctb.tiers.view.CloseActiviteEconomiqueView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping("/dossiers-apparentes/activite-economique")
public class ActiviteEconomiqueEditController {

	private TiersService tiersService;
	private AdresseService adresseService;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	@InitBinder("command")
	public void initCommandBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new CloseActiviteEconomiqueValidator());
	}

	private Autorisations getAutorisations(Tiers tiers) {
		return autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	private void checkDroitEditionActiviteEconomique(Tiers tiers) {
		final Autorisations auth = getAutorisations(tiers);
		if (!auth.isEditable() || !auth.isRapportsEtablissements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour modifier les rapports entre tiers de type 'activité économique'.");
		}
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "close.do", method = RequestMethod.GET)
	public String showClose(Model model, @RequestParam("id") long idRapportEntreTiers) {
		final ActiviteEconomique ae = getActiviteEconomiqueSecondaireOuverte(idRapportEntreTiers);
		final Tiers sujet = tiersService.getTiers(ae.getSujetId());
		checkDroitEditionActiviteEconomique(sujet);
		return showClose(model,
		                 new RapportView(ae, SensRapportEntreTiers.SUJET, tiersService, adresseService),
		                 new CloseActiviteEconomiqueView(ae));
	}

	private String showClose(Model model, RapportView data, CloseActiviteEconomiqueView view) {
		data.setViewRetour("../edit.do?id=" + data.getNumeroCourant());
		model.addAttribute("data", data);
		model.addAttribute("command", view);
		return "tiers/edition/rapport/rapport-close";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "close.do", method = RequestMethod.POST)
	public String doClose(Model model, @Valid @ModelAttribute("command") CloseActiviteEconomiqueView view, BindingResult bindingResult) {
		final ActiviteEconomique ae = getActiviteEconomiqueSecondaireOuverte(view.getIdRapportActiviteEconomique());
		if (bindingResult.hasErrors()) {
			return showClose(model,
			                 new RapportView(ae, SensRapportEntreTiers.SUJET, tiersService, adresseService),
			                 view);
		}

		final Tiers sujet = tiersService.getTiers(ae.getSujetId());
		checkDroitEditionActiviteEconomique(sujet);

		ae.setDateFin(view.getDateFin());
		return "redirect:../edit.do?id=" + ae.getSujetId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "reopen.do", method = RequestMethod.POST)
	public String doReopen(@RequestParam("idRapport") long idRapportEntreTiers) {
		final ActiviteEconomique ae = getActiviteEconomiqueSecondaireFermee(idRapportEntreTiers);

		final Tiers sujet = tiersService.getTiers(ae.getSujetId());
		checkDroitEditionActiviteEconomique(sujet);

		// TODO vérifier qu'il faut bien passer par une mécanique de duplication/annulation
		final RapportEntreTiers reouvert = ae.duplicate();
		reouvert.setDateFin(null);
		ae.setAnnule(true);

		final Tiers objet = tiersService.getTiers(ae.getObjetId());
		tiersService.addRapport(reouvert, sujet, objet);

		return "redirect:../edit.do?id=" + ae.getSujetId();
	}

	@NotNull
	private ActiviteEconomique getActiviteEconomiqueSecondaireOuverte(long idRapportEntreTiers) throws ObjectNotFoundException, ActionException {
		final ActiviteEconomique ae = getActiviteEconomiqueSecondaireNonAnnulee(idRapportEntreTiers);
		if (ae.getDateFin() != null) {
			throw new ActionException("Le lien d'activité économique est déjà fermé.");
		}
		return ae;
	}

	@NotNull
	private ActiviteEconomique getActiviteEconomiqueSecondaireFermee(long idRapportEntreTiers) throws ObjectNotFoundException, ActionException {
		final ActiviteEconomique ae = getActiviteEconomiqueSecondaireNonAnnulee(idRapportEntreTiers);
		if (ae.getDateFin() == null) {
			throw new ActionException("Le lien d'activité économique n'est pas fermé.");
		}
		return ae;
	}

	@NotNull
	private ActiviteEconomique getActiviteEconomiqueSecondaireNonAnnulee(long id) throws ObjectNotFoundException {
		final ActiviteEconomique ae = hibernateTemplate.get(ActiviteEconomique.class, id);
		if (ae == null) {
			throw new ObjectNotFoundException("Aucun lien d'activité économique trouvé avec l'identifiant " + id);
		}
		if (ae.isAnnule()) {
			throw new ActionException("Le lien d'activité économique est déjà annulé.");
		}
		if (ae.isPrincipal()) {
			throw new ActionException("Action non-autorisée sur un lien d'activité économique principale.");
		}
		return ae;
	}
}
