package ch.vd.uniregctb.fors;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
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
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.validation.fors.ForDebiteurPrestationImposableValidator;

@Controller
@RequestMapping(value = "/fors")
public class ForsController {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private MessageSourceAccessor messageSource;
	private Validator forsValidator;
	private SecurityProviderInterface securityProvider;
	private ParametreAppService paramService;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;

	private Map<MotifRattachement, String> motifsRattachementForPrincipal;
	private Map<MotifRattachement, String> motifsRattachementForSecondaire;
	private Map<MotifRattachement, String> motifsRattachementForAutreElementImposable;
	private Map<GenreImpot, String> genresImpotsForAutreImpot;

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

	public void setForsValidator(Validator forsValidator) {
		this.forsValidator = forsValidator;
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

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(forsValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	@RequestMapping(value = "/motifsOuverture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<MotifForView> motifsOuverture(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                                          @RequestParam(value = "genreImpot", required = true) GenreImpot genreImpot,
	                                          @RequestParam(value = "rattachement", required = false) MotifRattachement rattachement) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			return Collections.emptyList();
		}

		final NatureTiers natureTiers = tiers.getNatureTiers();
		final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, genreImpot, rattachement);

		final List<MotifForView> list = new ArrayList<>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsOuverture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelOuverture(motifFor)));
		}

		Collections.sort(list, new Comparator<MotifForView>() {
			@Override
			public int compare(MotifForView o1, MotifForView o2) {
				return o1.getLabel().compareTo(o2.getLabel());
			}
		});

		return list;
	}

	private String getLabelOuverture(MotifFor motifFor) {
		final String key = String.format("%s%s", ApplicationConfig.masterKeyMotifOuverture, motifFor.name());
		return messageSource.getMessage(key);
	}

	@RequestMapping(value = "/motifsFermeture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<MotifForView> motifsFermeture(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                                          @RequestParam(value = "genreImpot", required = true) GenreImpot genreImpot,
	                                          @RequestParam(value = "rattachement", required = false) MotifRattachement rattachement) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			return Collections.emptyList();
		}

		final NatureTiers natureTiers = tiers.getNatureTiers();
		final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, genreImpot, rattachement);

		final List<MotifForView> list = new ArrayList<>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsFermeture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelFermeture(motifFor)));
		}

		Collections.sort(list, new Comparator<MotifForView>() {
			@Override
			public int compare(MotifForView o1, MotifForView o2) {
				return o1.getLabel().compareTo(o2.getLabel());
			}
		});

		return list;
	}

	private String getLabelFermeture(MotifFor motifFor) {
		final String key = String.format("%s%s", ApplicationConfig.masterKeyMotifFermeture, motifFor.name());
		return messageSource.getMessage(key);
	}

	private Autorisations getAutorisations(Contribuable ctb) {
		return autorisationManager.getAutorisations(ctb, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	@RequestMapping(value = "/principal/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addPrincipal(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Contribuable ctb = (Contribuable) tiersDAO.get(tiersId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors principaux.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForPrincipal());
		model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("command", new AddForPrincipalView(tiersId));
		return "fors/principal/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/principal/add.do", method = RequestMethod.POST)
	public String addPrincipal(@Valid @ModelAttribute("command") final AddForPrincipalView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors principaux.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForPrincipal());
			model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
			return "fors/principal/add";
		}


		final ForFiscalPrincipal newFor = tiersService.addForPrincipal(ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getMotifRattachement(),
		                                                               view.getNoAutoriteFiscale(), view.getTypeAutoriteFiscale(), view.getModeImposition());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@RequestMapping(value = "/principal/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editPrincipal(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, forId);
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ffp.getTiers().getNumero());

		model.addAttribute("command", new EditForPrincipalView(ffp));
		return "fors/principal/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/principal/edit.do", method = RequestMethod.POST)
	public String editPrincipal(@Valid @ModelAttribute("command") final EditForPrincipalView view, BindingResult result, Model model) throws Exception {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, view.getId());
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		final long ctbId = ffp.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			return "fors/principal/edit";
		}

		final ForFiscalPrincipal newFor = tiersService.updateForPrincipal(ffp, view.getDateFin(), view.getMotifFin(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	private static String buildHighlightForParam(@Nullable ForFiscal newFor) {
		if (newFor == null) {
			return StringUtils.EMPTY;
		}
		return "&highlightFor=" + newFor.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/principal/cancel.do", method = RequestMethod.POST)
	public String cancelPrincipal(long forId) throws Exception {

		final ForFiscalPrincipal forFiscal = hibernateTemplate.get(ForFiscalPrincipal.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		final Autorisations auth = getAutorisations((Contribuable) tiers);
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fiscal/edit.do?id=" + tiers.getId() + "&highlightFor=" + forFiscal.getId();
	}

	@RequestMapping(value = "/principal/editModeImposition.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editModeImposition(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, forId);
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ffp.getTiers().getNumero());

		model.addAttribute("command", new EditModeImpositionView(ffp));
		model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
		return "fors/principal/editModeImposition";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/principal/editModeImposition.do", method = RequestMethod.POST)
	public String editModeImposition(@Valid @ModelAttribute("command") final EditModeImpositionView view, BindingResult result, Model model) throws Exception {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, view.getId());
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		final long ctbId = ffp.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
			return "fors/principal/editModeImposition";
		}

		final ForFiscalPrincipal newFor = tiersService.changeModeImposition((Contribuable) ffp.getTiers(), view.getDateChangement(), view.getModeImposition(), view.getMotifChangement());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@RequestMapping(value = "/secondaire/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addSecondaire(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Contribuable ctb = (Contribuable) tiersDAO.get(tiersId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsSecondaires()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors secondaires.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForSecondaire());
		model.addAttribute("command", new AddForSecondaireView(tiersId));
		return "fors/secondaire/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/secondaire/add.do", method = RequestMethod.POST)
	public String addSecondaire(@Valid @ModelAttribute("command") final AddForSecondaireView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsSecondaires()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors secondaires.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForSecondaire());
			return "fors/secondaire/add";
		}

		final ForFiscalSecondaire newFor = tiersService.addForSecondaire(ctb, view.getDateDebut(), view.getDateFin(), view.getMotifRattachement(), view.getNoAutoriteFiscale(),
		                                                                 view.getTypeAutoriteFiscale(), view.getMotifDebut(), view.getMotifFin());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@RequestMapping(value = "/secondaire/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editSecondaire(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalSecondaire ffs = hibernateTemplate.get(ForFiscalSecondaire.class, forId);
		if (ffs == null) {
			throw new ObjectNotFoundException("Le for secondaire avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffs.getTiers());
		if (!auth.isForsSecondaires()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors secondaires.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ffs.getTiers().getNumero());

		model.addAttribute("command", new EditForSecondaireView(ffs));
		return "fors/secondaire/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/secondaire/edit.do", method = RequestMethod.POST)
	public String editSecondaire(@Valid @ModelAttribute("command") final EditForSecondaireView view, BindingResult result, Model model) throws Exception {

		final ForFiscalSecondaire ffs = hibernateTemplate.get(ForFiscalSecondaire.class, view.getId());
		if (ffs == null) {
			throw new ObjectNotFoundException("Le for secondaire avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffs.getTiers());
		if (!auth.isForsSecondaires()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors secondaires.");
		}

		final long ctbId = ffs.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			return "fors/secondaire/edit";
		}

		final ForFiscalSecondaire newFor = tiersService.updateForSecondaire(ffs, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/secondaire/cancel.do", method = RequestMethod.POST)
	public String cancelSecondaire(long forId) throws Exception {

		final ForFiscalSecondaire forFiscal = hibernateTemplate.get(ForFiscalSecondaire.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		final Autorisations auth = getAutorisations((Contribuable) tiers);
		if (!auth.isForsSecondaires()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors secondaires.");
		}

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fiscal/edit.do?id=" + tiers.getId() + "&highlightFor=" + forFiscal.getId();
	}

	@RequestMapping(value = "/autreelementimposable/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addAutreElementImposable(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Contribuable ctb = (Contribuable) tiersDAO.get(tiersId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsAutresElementsImposables()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres éléments imposables.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForAutreElementImposable());
		model.addAttribute("command", new AddForAutreElementImposableView(tiersId));
		return "fors/autreelementimposable/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autreelementimposable/add.do", method = RequestMethod.POST)
	public String addAutreElementImposable(@Valid @ModelAttribute("command") final AddForAutreElementImposableView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsAutresElementsImposables()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres éléments imposables.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForAutreElementImposable());
			return "fors/autreelementimposable/add";
		}

		final ForFiscalAutreElementImposable newFor = tiersService.addForAutreElementImposable(ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(),
		                                                                                       view.getMotifRattachement(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@RequestMapping(value = "/autreelementimposable/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editAutreElementImposable(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalAutreElementImposable ffaei = hibernateTemplate.get(ForFiscalAutreElementImposable.class, forId);
		if (ffaei == null) {
			throw new ObjectNotFoundException("Le for autre élément imposable avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffaei.getTiers());
		if (!auth.isForsAutresElementsImposables()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres éléments imposables.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ffaei.getTiers().getNumero());

		model.addAttribute("command", new EditForAutreElementImposableView(ffaei));
		return "fors/autreelementimposable/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autreelementimposable/edit.do", method = RequestMethod.POST)
	public String editAutreElementImposable(@Valid @ModelAttribute("command") final EditForAutreElementImposableView view, BindingResult result, Model model) throws Exception {

		final ForFiscalAutreElementImposable ffaei = hibernateTemplate.get(ForFiscalAutreElementImposable.class, view.getId());
		if (ffaei == null) {
			throw new ObjectNotFoundException("Le for autre élément imposable avec l'id = " + view.getId() + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations((Contribuable) ffaei.getTiers());
		if (!auth.isForsAutresElementsImposables()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres éléments imposables.");
		}

		final long ctbId = ffaei.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			return "fors/autreelementimposable/edit";
		}

		final ForFiscalAutreElementImposable newFor = tiersService.updateForAutreElementImposable(ffaei, view.getDateFin(), view.getMotifFin(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autreelementimposable/cancel.do", method = RequestMethod.POST)
	public String cancelAutreElementImposable(long forId) throws Exception {

		final ForFiscalAutreElementImposable forFiscal = hibernateTemplate.get(ForFiscalAutreElementImposable.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		final Autorisations auth = getAutorisations((Contribuable) tiers);
		if (!auth.isForsAutresElementsImposables()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors autres éléments imposables.");
		}

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fiscal/edit.do?id=" + tiers.getId() + "&highlightFor=" + forFiscal.getId();
	}

	@RequestMapping(value = "/autreimpot/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addAutreImpot(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		final Contribuable ctb = (Contribuable) tiersDAO.get(tiersId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + tiersId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsAutresImpots()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres impôts.");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("genresImpot", getGenresImpotPourForAutreImpot());
		model.addAttribute("command", new AddForAutreImpotView(tiersId));
		return "fors/autreimpot/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autreimpot/add.do", method = RequestMethod.POST)
	public String addAutreImpot(@Valid @ModelAttribute("command") final AddForAutreImpotView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ctb);
		if (!auth.isForsAutresImpots()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec de création de fors autres impôts.");
		}

		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("genresImpot", getGenresImpotPourForAutreImpot());
			return "fors/autreimpot/add";
		}

		final ForFiscalAutreImpot newFor = tiersService.openForFiscalAutreImpot(ctb, view.getGenreImpot(), view.getDateEvenement(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autreimpot/cancel.do", method = RequestMethod.POST)
	public String cancelAutreImpot(long forId) throws Exception {

		final ForFiscalAutreImpot forFiscal = hibernateTemplate.get(ForFiscalAutreImpot.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		final Autorisations auth = getAutorisations((Contribuable) tiers);
		if (!auth.isForsAutresImpots()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors autres impôts.");
		}

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fiscal/edit.do?id=" + tiers.getId() + "&highlightFor=" + forFiscal.getId();
	}

	@RequestMapping(value = "/debiteur/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addDebiteur(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("anneeMinimaleForDebiteur", paramService.getAnneeMinimaleForDebiteur());
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscaleDPI());
		model.addAttribute("command", new AddForDebiteurView(tiersId));
		return "fors/debiteur/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/add.do", method = RequestMethod.POST)
	public String addDebiteur(@Valid @ModelAttribute("command") final AddForDebiteurView view, BindingResult result, Model model) throws Exception {

		final long dpiId = view.getTiersId();

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(dpiId);

		if (result.hasErrors()) {
			model.addAttribute("anneeMinimaleForDebiteur", paramService.getAnneeMinimaleForDebiteur());
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscaleDPI());
			return "fors/debiteur/add";
		}

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
		if (debiteur == null) {
			throw new ObjectNotFoundException("Le débiteur avec l'id=" + dpiId + " n'existe pas.");
		}

		tiersService.addForDebiteur(debiteur, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit-for-debiteur.do?id=" + dpiId;
	}

	@RequestMapping(value = "/debiteur/datesFermeture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<RegDate> datesFermetureForDebiteur(@RequestParam(value = "forId") long forId) {

		final ForDebiteurPrestationImposable fdpi = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (fdpi == null) {
			throw new ObjectNotFoundException("Le for débiteur avec l'id = " + forId + " n'existe pas.");
		}

		final Tiers tiers = fdpi.getTiers();
		controllerUtils.checkAccesDossierEnLecture(tiers.getNumero());

		// validation du type de tiers...
		if (!(tiers instanceof DebiteurPrestationImposable)) {
			throw new ObjectNotFoundException(String.format("Le for débiteur %d n'est pas associé à un débiteur de prestation imposable (%s)", forId, tiers));
		}
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
		return ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, fdpi, RegDate.get(RegDate.get().year(), 12, 31), false);
	}

	@RequestMapping(value = "/debiteur/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editDebiteur(@RequestParam(value = "forId", required = true) long forId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable fdpi = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (fdpi == null) {
			throw new ObjectNotFoundException("Le for débiteur avec l'id = " + forId + " n'existe pas.");
		}

		controllerUtils.checkAccesDossierEnEcriture(fdpi.getTiers().getNumero());

		model.addAttribute("command", new EditForDebiteurView(fdpi));
		return "fors/debiteur/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/edit.do", method = RequestMethod.POST)
	public String editDebiteur(@Valid @ModelAttribute("command") final EditForDebiteurView view, BindingResult result, Model model) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable fdpi = hibernateTemplate.get(ForDebiteurPrestationImposable.class, view.getId());
		if (fdpi == null) {
			throw new ObjectNotFoundException("Le for débiteur avec l'id = " + view.getId() + " n'existe pas.");
		}

		final long dpiId = fdpi.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(dpiId);

		if (result.hasErrors()) {
			return "fors/debiteur/edit";
		}

		tiersService.updateForDebiteur(fdpi, view.getDateFin(), view.getMotifFin());

		return "redirect:/fiscal/edit-for-debiteur.do?id=" + dpiId;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/cancel.do", method = RequestMethod.POST)
	public String cancelDebiteur(long forId) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable forFiscal = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fiscal/edit-for-debiteur.do?id=" + tiers.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/reopen.do", method = RequestMethod.POST)
	public String reopenDebiteur(long forId) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable forFiscal = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		tiersService.reouvrirForDebiteur(forFiscal);

		return "redirect:/fiscal/edit-for-debiteur.do?id=" + tiers.getId();
	}

	private Map<GenreImpot, String> getGenresImpotPourForAutreImpot() {
		if (genresImpotsForAutreImpot == null) {
			genresImpotsForAutreImpot = tiersMapHelper.getMapGenreImpot(GenreImpot.GAIN_IMMOBILIER, GenreImpot.DROIT_MUTATION, GenreImpot.PRESTATION_CAPITAL, GenreImpot.SUCCESSION, GenreImpot.FONCIER,
			                                                            GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, GenreImpot.DONATION, GenreImpot.CHIENS, GenreImpot.PATENTE_TABAC);
		}
		return genresImpotsForAutreImpot;
	}

	private Map<MotifRattachement, String> getMotifsRattachementPourForPrincipal() {
		if (motifsRattachementForPrincipal == null) {
			motifsRattachementForPrincipal = tiersMapHelper.getMapRattachement(MotifRattachement.DOMICILE, MotifRattachement.DIPLOMATE_SUISSE, MotifRattachement.DIPLOMATE_ETRANGER);
		}
		return motifsRattachementForPrincipal;
	}

	private Map<MotifRattachement, String> getMotifsRattachementPourForSecondaire() {
		if (motifsRattachementForSecondaire == null) {
			motifsRattachementForSecondaire =
					tiersMapHelper.getMapRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE);
		}
		return motifsRattachementForSecondaire;
	}

	private Map<MotifRattachement, String> getMotifsRattachementPourForAutreElementImposable() {
		if (motifsRattachementForAutreElementImposable == null) {
			motifsRattachementForAutreElementImposable =
					tiersMapHelper.getMapRattachement(MotifRattachement.ACTIVITE_LUCRATIVE_CAS, MotifRattachement.ADMINISTRATEUR, MotifRattachement.CREANCIER_HYPOTHECAIRE,
					                                  MotifRattachement.PRESTATION_PREVOYANCE, MotifRattachement.LOI_TRAVAIL_AU_NOIR, MotifRattachement.PARTICIPATIONS_HORS_SUISSE,
					                                  MotifRattachement.EFFEUILLEUSES);
		}
		return motifsRattachementForAutreElementImposable;
	}
}