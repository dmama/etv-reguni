package ch.vd.unireg.fors;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ApplicationConfig;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.tiers.validator.MotifsForHelper;
import ch.vd.unireg.tiers.view.ForFiscalView;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.validation.fors.ForDebiteurPrestationImposableValidator;

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
	private ServiceInfrastructureService infrastructureService;
	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;

	private Map<MotifRattachement, String> motifsRattachementForPrincipalPP;
	private Map<MotifRattachement, String> motifsRattachementForPrincipalPM;
	private Map<MotifRattachement, String> motifsRattachementForSecondairePP;
	private Map<MotifRattachement, String> motifsRattachementForSecondairePM;
	private Map<MotifRattachement, String> motifsRattachementForAutreElementImposable;
	private Map<GenreImpot, String> genresImpotForPrincipalOuSecondairePP;
	private Map<GenreImpot, String> genresImpotForPrincipalPM;
	private Map<GenreImpot, String> genresImpotForAutreImpot;

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

	public void setInfrastructureService(ServiceInfrastructureService infrastructureService) { this.infrastructureService = infrastructureService; }

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

		final Set<MotifFor> motifsOuverture = MotifsForHelper.getMotifsOuverture(typeFor);
		final List<MotifForView> list = new ArrayList<>(motifsOuverture.size());
		for (MotifFor motifFor : motifsOuverture) {
			list.add(new MotifForView(motifFor, getLabelOuverture(motifFor)));
		}

		list.sort(new Comparator<MotifForView>() {
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
	                                          @RequestParam(value = "rattachement", required = false) MotifRattachement rattachement,
	                                          @RequestParam(value = "oldMotif", required = false) MotifFor oldMotif) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			return Collections.emptyList();
		}

		final NatureTiers natureTiers = tiers.getNatureTiers();
		final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, genreImpot, rattachement);

		final Set<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(typeFor);
		final List<MotifForView> list = new ArrayList<>(motifsFermeture.size() + 1);
		for (MotifFor motifFor : motifsFermeture) {
			list.add(new MotifForView(motifFor, getLabelFermeture(motifFor)));
		}

		// [SIFISC-14390] on veut pouvoir conserver le motif de fermeture déjà présent sur le for, même si celui-ci n'est normalement pas accessible à la main
		if (oldMotif != null && !motifsFermeture.contains(oldMotif)) {
			list.add(new MotifForView(oldMotif, getLabelFermeture(oldMotif)));
		}

		list.sort(new Comparator<MotifForView>() {
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

	@Nullable
	private GenreImpot getDefaultGenreImpotForsPrincipauxEtSecondaires(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return GenreImpot.REVENU_FORTUNE;
		}
		if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			final CategorieEntreprise categorieCourante = tiersService.getCategorieEntreprise((Entreprise) ctb, null);
			if (categorieCourante == CategorieEntreprise.SP) {
				return GenreImpot.REVENU_FORTUNE;
			}
			else {
				return GenreImpot.BENEFICE_CAPITAL;
			}
		}
		return null;
	}

	@NotNull
	private Map<GenreImpot, String> getMapGenresImpotForsPrincipaux(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return getGenresImpotPourForPrincipalOuSecondairePP();
		}
		if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			return getGenresImpotPourForPrincipalOuSecondairePM();
		}
		return Collections.emptyMap();
	}

	@NotNull
	private Map<GenreImpot, String> getMapGenresImpotForsSecondaires(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return getGenresImpotPourForPrincipalOuSecondairePP();
		}
		if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			return getGenresImpotPourForPrincipalOuSecondairePM();
		}
		return Collections.emptyMap();
	}

	@NotNull
	private Map<MotifRattachement, String> getMapMotifsRattachementForsPrincipaux(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return getMotifsRattachementForPrincipalPP();
		}
		else if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			return getMotifsRattachementForPrincipalPM();
		}
		return Collections.emptyMap();
	}

	@NotNull
	private Map<MotifRattachement, String> getMapMotifsRattachementForsSecondaires(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return getMotifsRattachementForSecondairePP();
		}
		else if (ctb instanceof ContribuableImpositionPersonnesMorales) {
			return getMotifsRattachementForSecondairePM();
		}
		return Collections.emptyMap();
	}

	@NotNull
	private Map<ModeImposition, String> getMapModesImposition(Contribuable ctb) {
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			return tiersMapHelper.getMapModeImposition();
		}
		return Collections.emptyMap();
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

		model.addAttribute("rattachements", getMapMotifsRattachementForsPrincipaux(ctb));
		model.addAttribute("modesImposition", getMapModesImposition(ctb));
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("genresImpot", getMapGenresImpotForsPrincipaux(ctb));
		model.addAttribute("command", new AddForPrincipalView(tiersId, getDefaultGenreImpotForsPrincipauxEtSecondaires(ctb)));
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
			model.addAttribute("rattachements", getMapMotifsRattachementForsPrincipaux(ctb));
			model.addAttribute("modesImposition", getMapModesImposition(ctb));
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
			model.addAttribute("genresImpot", getMapGenresImpotForsPrincipaux(ctb));
			// [SIFISC-27087] récupération du nom de l'autorité fiscale à partir de son numéro
			if(view.getNoAutoriteFiscale() != null) {
				if(TypeAutoriteFiscale.PAYS_HS.equals(view.getTypeAutoriteFiscale())) {
					Pays pays = infrastructureService.getPays(view.getNoAutoriteFiscale(), null);
					if(pays != null){
						view.setNomAutoriteFiscale(pays.getNomCourt());
					}
				} else {
					Commune commune = infrastructureService.getCommuneByNumeroOfs(view.getNoAutoriteFiscale(), null);
					if(commune != null) {
						view.setNomAutoriteFiscale(commune.getNomOfficiel());
					}
				}
			}

			return "fors/principal/add";
		}

		final ForFiscalPrincipal newFor;
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			newFor = tiersService.addForPrincipal((ContribuableImpositionPersonnesPhysiques) ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(),
			                                      view.getMotifRattachement(), view.getNoAutoriteFiscale(), view.getTypeAutoriteFiscale(), view.getModeImposition());
		}
		else if (ctb instanceof ContribuableImpositionPersonnesMorales) {

			// [SIFISC-18594] si le nouveau for (IBC) est antérieur à tous les régimes fiscaux de l'entreprise, alors il faut
			// agrandir la couverture de ces régimes fiscaux pour couvrir ce nouveau for
			if (ctb instanceof Entreprise && view.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {
				final Entreprise entreprise = (Entreprise) ctb;
				final List<RegimeFiscal> vd = entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD);
				final List<RegimeFiscal> ch = entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.CH);
				if (!vd.isEmpty() && !ch.isEmpty()) {

					// on limite la zone de couverture agrandie à la période effectivement prise en compte par le validateur
					final DateRange zoneCouvertureValidee = new DateRangeHelper.Range(RegDate.get(paramService.getPremierePeriodeFiscalePersonnesMorales(), 1, 1), null);

					// commes les régimes fiscaux sont triés, seuls les tous premiers de chaque portée sont intéressants
					for (RegimeFiscal rf : Arrays.asList(vd.get(0), ch.get(0))) {
						if (view.getDateDebut().isBefore(rf.getDateDebut())) {
							final DateRange nouvelleCouvertureTheorique = new DateRangeHelper.Range(view.getDateDebut(), rf.getDateDebut().getOneDayBefore());
							final DateRange nouvelleCouverture = DateRangeHelper.intersection(nouvelleCouvertureTheorique, zoneCouvertureValidee);
							if (nouvelleCouverture != null) {
								// ce régime fiscal doit être rallongé vers le passé
								final RegimeFiscal rallonge = rf.duplicate();
								rallonge.setDateDebut(nouvelleCouverture.getDateDebut());
								rf.setAnnule(true);
								entreprise.addRegimeFiscal(rallonge);
							}
						}
					}
				}
			}

			newFor = tiersService.addForPrincipal((ContribuableImpositionPersonnesMorales) ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(),
			                                      view.getMotifRattachement(), view.getNoAutoriteFiscale(), view.getTypeAutoriteFiscale(), view.getGenreImpot());
		}
		else {
			throw new ActionException("Le contribuable n'est ni un contribuable PP ni un contribuable PM.");
		}

		return "redirect:/fiscal/edit.do?id=" + ctbId + buildHighlightForParam(newFor);
	}

	@RequestMapping(value = "/principal/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editPrincipal(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, forId);
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ffp.getTiers());
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

		final Autorisations auth = getAutorisations(ffp.getTiers());
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

		// [SIFISC-19956] si le tiers est maintenant "désactivé", le droit de modification des fors n'est plus
		// -> rien ne sert alors d'aller sur la page d'édition des fors, cela ne mène à rien...
		if (tiers.isDesactive(null)) {
			return "redirect:/tiers/visu.do?id=" + tiers.getId();
		}
		else {
			return "redirect:/fiscal/edit.do?id=" + tiers.getId() + buildHighlightForParam(forFiscal);
		}
	}

	@RequestMapping(value = "/principal/editModeImposition.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String editModeImposition(@RequestParam(value = "forId", required = true) long forId, Model model) {

		final ForFiscalPrincipalPP ffp = hibernateTemplate.get(ForFiscalPrincipalPP.class, forId);
		if (ffp == null) {
			throw new ObjectNotFoundException("Le for principal PP avec l'id = " + forId + " n'existe pas.");
		}

		final Autorisations auth = getAutorisations(ffp.getTiers());
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

		final Autorisations auth = getAutorisations(ffp.getTiers());
		if (!auth.isForsPrincipaux()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition de fors principaux.");
		}

		final long ctbId = ffp.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
			return "fors/principal/editModeImposition";
		}

		final ForFiscalPrincipal newFor = tiersService.changeModeImposition((ContribuableImpositionPersonnesPhysiques) ffp.getTiers(), view.getDateChangement(), view.getModeImposition(), view.getMotifChangement());

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

		model.addAttribute("rattachements", getMapMotifsRattachementForsSecondaires(ctb));
		model.addAttribute("genresImpot", getMapGenresImpotForsSecondaires(ctb));
		model.addAttribute("command", new AddForSecondaireView(tiersId, getDefaultGenreImpotForsPrincipauxEtSecondaires(ctb)));
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
			model.addAttribute("rattachements", getMapMotifsRattachementForsSecondaires(ctb));
			model.addAttribute("genresImpot", getMapGenresImpotForsSecondaires(ctb));
			// [SIFISC-27087] récupération du nom de l'autorité fiscale à partir de son numéro
			if(view.getNoAutoriteFiscale() != null) {
				Commune commune = infrastructureService.getCommuneByNumeroOfs(view.getNoAutoriteFiscale(), null);
				if(commune != null) {
					view.setNomAutoriteFiscale(commune.getNomOfficiel());
				}
			}

			return "fors/secondaire/add";
		}

		final ForFiscalSecondaire newFor = tiersService.addForSecondaire(ctb, view.getDateDebut(), view.getDateFin(), view.getMotifRattachement(), view.getNoAutoriteFiscale(),
		                                                                 view.getTypeAutoriteFiscale(), view.getMotifDebut(), view.getMotifFin(), view.getGenreImpot());

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

			// [SIFISC-27087] récupération du nom de l'autorité fiscale à partir de son numéro
			if(view.getNoAutoriteFiscale() != null) {
				if(TypeAutoriteFiscale.PAYS_HS.equals(view.getTypeAutoriteFiscale())) {
					Pays pays = infrastructureService.getPays(view.getNoAutoriteFiscale(), null);
					if(pays != null){
						view.setNomAutoriteFiscale(pays.getNomCourt());
					}
				} else {
					Commune commune = infrastructureService.getCommuneByNumeroOfs(view.getNoAutoriteFiscale(), null);
					if(commune != null) {
						view.setNomAutoriteFiscale(commune.getNomOfficiel());
					}
				}
			}

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

			// [SIFISC-27087] récupération du nom de l'autorité fiscale à partir de son numéro
			if(view.getNoAutoriteFiscale() != null) {
				if(TypeAutoriteFiscale.PAYS_HS.equals(view.getTypeAutoriteFiscale())) {
					Pays pays = infrastructureService.getPays(view.getNoAutoriteFiscale(), null);
					if(pays != null){
						view.setNomAutoriteFiscale(pays.getNomCourt());
					}
				} else {
					Commune commune = infrastructureService.getCommuneByNumeroOfs(view.getNoAutoriteFiscale(), null);
					if(commune != null) {
						view.setNomAutoriteFiscale(commune.getNomOfficiel());
					}
				}
			}

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

	@RequestMapping(value = "/debiteur/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String listDebiteur(@RequestParam("id") long id, Model model) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les fors fiscaux d'un débiteur");
		}

		controllerUtils.checkAccesDossierEnLecture(id);

		final DebiteurPrestationImposable dpi = getDebiteurPrestationImposable(id);
		final List<ForFiscalView> forsView = ForFiscalView.getList(dpi, null);
		model.addAttribute("id", id);
		model.addAttribute("fors", forsView);

		return "fors/debiteur/list";
	}

	@NotNull
	private DebiteurPrestationImposable getDebiteurPrestationImposable(long id) {
		final Tiers tiers = tiersDAO.get(id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}
		if (!(tiers instanceof DebiteurPrestationImposable)) {
			throw new ObjectNotFoundException("Le tiers n°" + id + " n'est pas un débiteur de prestations imposables.");
		}

		return (DebiteurPrestationImposable) tiers;
	}

	@RequestMapping(value = "/debiteur/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addDebiteur(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(dpiId);

		if (result.hasErrors()) {
			model.addAttribute("anneeMinimaleForDebiteur", paramService.getAnneeMinimaleForDebiteur());
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscaleDPI());

			// [SIFISC-27087] récupération du nom de l'autorité fiscale à partir de son numéro
			if(view.getNoAutoriteFiscale() != null) {
				if(TypeAutoriteFiscale.PAYS_HS.equals(view.getTypeAutoriteFiscale())) {
					Pays pays = infrastructureService.getPays(view.getNoAutoriteFiscale(), null);
					if(pays != null){
						view.setNomAutoriteFiscale(pays.getNomCourt());
					}
				} else {
					Commune commune = infrastructureService.getCommuneByNumeroOfs(view.getNoAutoriteFiscale(), null);
					if(commune != null) {
						view.setNomAutoriteFiscale(commune.getNomOfficiel());
					}
				}
			}

			return "fors/debiteur/add";
		}

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
		if (debiteur == null) {
			throw new ObjectNotFoundException("Le débiteur avec l'id=" + dpiId + " n'existe pas.");
		}

		tiersService.addForDebiteur(debiteur, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());

		return "redirect:/fors/debiteur/list.do?id=" + dpiId;
	}

	@RequestMapping(value = "/debiteur/datesFermeture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public Set<RegDate> datesFermetureForDebiteur(@RequestParam(value = "forId") long forId) {

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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
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

		return "redirect:/fors/debiteur/list.do?id=" + dpiId;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/cancel.do", method = RequestMethod.POST)
	public String cancelDebiteur(long forId) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable forFiscal = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		tiersService.annuleForFiscal(forFiscal);

		return "redirect:/fors/debiteur/list.do?id=" + tiers.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/reopen.do", method = RequestMethod.POST)
	public String reopenDebiteur(long forId) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI, Role.MODIF_FISCAL_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}

		final ForDebiteurPrestationImposable forFiscal = hibernateTemplate.get(ForDebiteurPrestationImposable.class, forId);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + forId + " n'existe pas.");
		}
		final Tiers tiers = forFiscal.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

		tiersService.reouvrirForDebiteur(forFiscal);

		return "redirect:/fors/debiteur/list.do?id=" + tiers.getId();
	}

	private Map<GenreImpot, String> getGenresImpotPourForAutreImpot() {
		if (genresImpotForAutreImpot == null) {
			genresImpotForAutreImpot = tiersMapHelper.getMapGenreImpot(GenreImpot.GAIN_IMMOBILIER, GenreImpot.DROIT_MUTATION, GenreImpot.PRESTATION_CAPITAL, GenreImpot.SUCCESSION, GenreImpot.FONCIER,
			                                                           GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, GenreImpot.DONATION, GenreImpot.CHIENS, GenreImpot.PATENTE_TABAC);
		}
		return genresImpotForAutreImpot;
	}

	private Map<GenreImpot, String> getGenresImpotPourForPrincipalOuSecondairePP() {
		if (genresImpotForPrincipalOuSecondairePP == null) {
			genresImpotForPrincipalOuSecondairePP = tiersMapHelper.getMapGenreImpot(GenreImpot.REVENU_FORTUNE);
		}
		return genresImpotForPrincipalOuSecondairePP;
	}

	private Map<GenreImpot, String> getGenresImpotPourForPrincipalOuSecondairePM() {
		if (genresImpotForPrincipalPM == null) {
			genresImpotForPrincipalPM = tiersMapHelper.getMapGenreImpot(GenreImpot.REVENU_FORTUNE, GenreImpot.BENEFICE_CAPITAL);
		}
		return genresImpotForPrincipalPM;
	}

	private Map<MotifRattachement, String> getMotifsRattachementForPrincipalPP() {
		if (motifsRattachementForPrincipalPP == null) {
			motifsRattachementForPrincipalPP = tiersMapHelper.getMapRattachement(MotifRattachement.DOMICILE, MotifRattachement.DIPLOMATE_SUISSE, MotifRattachement.DIPLOMATE_ETRANGER);
		}
		return motifsRattachementForPrincipalPP;
	}

	private Map<MotifRattachement, String> getMotifsRattachementForSecondairePP() {
		if (motifsRattachementForSecondairePP == null) {
			motifsRattachementForSecondairePP = tiersMapHelper.getMapRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE);
		}
		return motifsRattachementForSecondairePP;
	}

	private Map<MotifRattachement, String> getMotifsRattachementForPrincipalPM() {
		if (motifsRattachementForPrincipalPM == null) {
			motifsRattachementForPrincipalPM = tiersMapHelper.getMapRattachement(MotifRattachement.DOMICILE);
		}
		return motifsRattachementForPrincipalPM;
	}

	private Map<MotifRattachement, String> getMotifsRattachementForSecondairePM() {
		if (motifsRattachementForSecondairePM == null) {
			motifsRattachementForSecondairePM = tiersMapHelper.getMapRattachement(MotifRattachement.ETABLISSEMENT_STABLE, MotifRattachement.IMMEUBLE_PRIVE);
		}
		return motifsRattachementForSecondairePM;
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