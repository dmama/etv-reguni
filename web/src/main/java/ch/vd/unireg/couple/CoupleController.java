package ch.vd.unireg.couple;

import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.TiersNumberEditor;

@Controller
@RequestMapping(value = "/couple")
public class CoupleController {

	private CoupleManager coupleManager;
	private Validator coupleValidator;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCoupleManager(CoupleManager coupleManager) {
		this.coupleManager = coupleManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCoupleValidator(Validator coupleValidator) {
		this.coupleValidator = coupleValidator;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@RequestMapping(value = "/create.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String create(@RequestParam(value = "pp1", required = false) Long pp1Id, Model model) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits de consultation pour l'application Unireg");
		}

		final CoupleView view = new CoupleView();
		view.setPp1Id(pp1Id);
		model.addAttribute("command", view);

		return "couple/create";
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(coupleValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(Long.class, "pp1Id", new TiersNumberEditor(true));
		binder.registerCustomEditor(Long.class, "pp2Id", new TiersNumberEditor(true));
		binder.registerCustomEditor(Long.class, "mcId", new TiersNumberEditor(true));
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/create.do", method = RequestMethod.POST)
	public String create(@Valid @ModelAttribute("command") final CoupleView view, BindingResult result) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits de consultation pour l'application Unireg");
		}

		if (result.hasErrors()) {
			return "couple/create";
		}

		// on détermine quelques informations sur le futur ménage commun
		final CoupleManager.CoupleInfo info = coupleManager.determineInfoFuturCouple(view.getPp1Id(), view.getPp2Id(), view.getMcId());

		// on récupère les paramètres de la requête, en forçant si nécessaire les valeurs qui vont bien
		final Long pp1Id = view.getPp1Id();
		final Long pp2Id = view.getPp2Id();
		final Long mcId = (info.getForceMcId() == null ? view.getMcId() : info.getForceMcId());
		final RegDate dateDebut = (info.getForceDateDebut() == null ? view.getDateDebut() : info.getForceDateDebut());

		controllerUtils.checkAccesDossierEnEcriture(pp1Id);
		controllerUtils.checkAccesDossierEnEcriture(pp2Id);
		controllerUtils.checkAccesDossierEnEcriture(mcId);

		//Vérification droits Decision ACI
		controllerUtils.checkTraitementContribuableAvecDecisionAci(pp1Id);
		controllerUtils.checkTraitementContribuableAvecDecisionAci(pp2Id);
		controllerUtils.checkTraitementContribuableAvecDecisionAci(mcId);

		final MenageCommun menage = coupleManager.sauverCouple(pp1Id, pp2Id, mcId, dateDebut, info.getType(), info.getEtatCivil(), view.getRemarque());
		final Long menageId = menage.getId();

		return "redirect:/tiers/visu.do?id=" + menageId;
	}

	/**
	 * Analyse les personnes physiques et le ménage-commun passés en paramètres et détermine le type d'union et une éventuelle date de début.
	 *
	 * @param pp1Id l'id de la première personne physique
	 * @param pp2Id l'id de la seconde personne physique (optionel)
	 * @param mcId  l'id du ménage-commun résultant (optionel)
	 * @return le type d'union + la date de début
	 */
	@RequestMapping(value = "/info.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public CoupleInfoView info(@RequestParam(value = "pp1Id", required = false) Long pp1Id, @RequestParam(value = "pp2Id", required = false) Long pp2Id,
	                           @RequestParam(value = "mcId", required = false) Long mcId) {

		return new CoupleInfoView(coupleManager.determineInfoFuturCouple(pp1Id, pp2Id, mcId));
	}
}
