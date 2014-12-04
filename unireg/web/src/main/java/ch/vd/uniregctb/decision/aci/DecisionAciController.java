package ch.vd.uniregctb.decision.aci;

import javax.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.DecisionAciDAO;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/decision-aci")
public class DecisionAciController {

	private TiersDAO tiersDAO;
	private DecisionAciDAO decisionAciDAO;
	private TiersService tiersService;
	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private Validator decisionAciValidator;
	private MessageSourceAccessor messageSource;
	private SecurityProviderInterface securityProvider;
	private ParametreAppService paramService;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;


	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = new MessageSourceAccessor(messageSource);
	}

	public void setDecisionAciDAO(DecisionAciDAO decisionAciDAO) {
		this.decisionAciDAO = decisionAciDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setParamService(ParametreAppService paramService) {
		this.paramService = paramService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setDecisionAciValidator(Validator decisionAciValidator) {
		this.decisionAciValidator = decisionAciValidator;
	}



	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(decisionAciValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}



	private Autorisations getAutorisations(Contribuable ctb) {
		return autorisationManager.getAutorisations(ctb, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Contribuable ctb = (Contribuable) tiersDAO.get(tiersId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isDecisionsAci()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de décisions ACI.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("command", new AddDecisionAciView(tiersId));
		return "decision-aci/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String add(@Valid @ModelAttribute("command") final AddDecisionAciView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isDecisionsAci()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de décisions ACI.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
			return "decision-aci/add";
		}


		final DecisionAci decisionAci = tiersService.addDecisionAci(ctb,view.getTypeAutoriteFiscale(),view.getNumeroAutoriteFiscale(),
										view.getDateDebut(),view.getDateFin(),view.getRemarque());
		return "redirect:/fiscal/edit.do?id=" + ctbId;
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam(value = "decisionId", required = true) long decisionId, Model model) {

		final DecisionAci decisionAci = hibernateTemplate.get(DecisionAci.class, decisionId);
		if (decisionAci == null) {
			throw new ObjectNotFoundException("La décision ACI avec l'id = " + decisionId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) decisionAci.getTiers());
		if (!auth.isDecisionsAci()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de décision ACI.");
		}

		final Long tiersId = decisionAci.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("command", new EditDecisionAciView(decisionAci));
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		return "decision-aci/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	public String edit(@Valid @ModelAttribute("command") final EditDecisionAciView view, BindingResult result, Model model) throws Exception {

		final DecisionAci decisionAci = hibernateTemplate.get(DecisionAci.class, view.getId());
		if (decisionAci == null) {
			throw new ObjectNotFoundException("La décision ACI avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(decisionAci.getTiers().getNumero());
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + view.getTiersId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) decisionAci.getTiers());
		if (!auth.isDecisionsAci()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de décision ACI.");
		}

		final Long numero = ctb.getNumero();
		controllerUtils.checkAccesDossierEnEcriture(numero);

		if (result.hasErrors()) {
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
			return "decision-aci/edit";
		}

		final DecisionAci decisionUpdated = tiersService.updateDecisionAci(decisionAci, view.getDateFin(), view.getRemarque(), view.getNumeroAutoriteFiscale());


		return "redirect:/fiscal/edit.do?id=" + numero;
	}
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancel(long decisionId) throws Exception {

		final DecisionAci decisionAci = hibernateTemplate.get(DecisionAci.class, decisionId);
		if (decisionAci == null) {
			throw new ObjectNotFoundException("La décision ACI  n°" + decisionId + " n'existe pas.");
		}
		final Tiers tiers = decisionAci.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		final Autorisations auth = getAutorisations((Contribuable) tiers);

		if (!auth.isDecisionsAci()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'annulation de décision ACI.");
		}

		//On annule l'ancienne décision
		decisionAci.setAnnule(true);
		decisionAciDAO.save(decisionAci);

		return "redirect:/fiscal/edit.do?id=" + tiers.getId();
	}




}