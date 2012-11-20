package ch.vd.uniregctb.fors;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.utils.RegDateEditor;

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

	private Map<MotifRattachement, String> motifsRattachementForPrincipal;
	private Map<MotifRattachement, String> motifsRattachementForSecondaire;
	private Map<MotifRattachement, String> motifsRattachementForAutreElementImposable;
	private Map<GenreImpot,String> genresImpotsForAutreImpot;

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

		final List<MotifForView> list = new ArrayList<MotifForView>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsOuverture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelOuverture(motifFor)));
		}

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

		final List<MotifForView> list = new ArrayList<MotifForView>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsFermeture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelFermeture(motifFor)));
		}

		return list;
	}

	private String getLabelFermeture(MotifFor motifFor) {
		final String key = String.format("%s%s", ApplicationConfig.masterKeyMotifFermeture, motifFor.name());
		return messageSource.getMessage(key);
	}

	@RequestMapping(value = "/addPrincipal.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addPrincipal(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForPrincipal());
		model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
		model.addAttribute("command", new AddForPrincipalView(tiersId));
		return "fors/addPrincipal";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/addPrincipal.do", method = RequestMethod.POST)
	public String addPrincipal(@Valid @ModelAttribute("command") final AddForPrincipalView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		// FIXME (msi) gérer la sécurité un peu plus sérieusement
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForPrincipal());
			model.addAttribute("modesImposition", tiersMapHelper.getMapModeImposition());
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscale());
			return "fors/addPrincipal";
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		tiersService.addForPrincipal(ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getMotifRattachement(), view.getNoAutoriteFiscale(),
		                             view.getTypeAutoriteFiscale(), view.getModeImposition());

		return "redirect:/fiscal/edit.do?id=" + ctbId;
	}

	@RequestMapping(value = "/addSecondaire.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addSecondaire(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForSecondaire());
		model.addAttribute("command", new AddForSecondaireView(tiersId));
		return "fors/addSecondaire";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/addSecondaire.do", method = RequestMethod.POST)
	public String addSecondaire(@Valid @ModelAttribute("command") final AddForSecondaireView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		// FIXME (msi) gérer la sécurité un peu plus sérieusement
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForSecondaire());
			return "fors/addSecondaire";
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		tiersService.addForSecondaire(ctb, view.getDateDebut(), view.getDateFin(), view.getMotifRattachement(), view.getNoAutoriteFiscale(), view.getTypeAutoriteFiscale(),
		                              view.getMotifDebut(), view.getMotifFin());

		return "redirect:/fiscal/edit.do?id=" + ctbId;
	}

	@RequestMapping(value = "/addAutreElementImposable.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addAutreElementImposable(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("rattachements", getMotifsRattachementPourForAutreElementImposable());
		model.addAttribute("command", new AddForAutreElementImposableView(tiersId));
		return "fors/addAutreElementImposable";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/addAutreElementImposable.do", method = RequestMethod.POST)
	public String addAutreElementImposable(@Valid @ModelAttribute("command") final AddForAutreElementImposableView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		// FIXME (msi) gérer la sécurité un peu plus sérieusement
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("rattachements", getMotifsRattachementPourForAutreElementImposable());
			return "fors/addAutreElementImposable";
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		tiersService.addForAutreElementImposable(ctb, view.getDateDebut(), view.getMotifDebut(), view.getDateFin(), view.getMotifFin(), view.getMotifRattachement(),
		                                         view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId;
	}

	@RequestMapping(value = "/addAutreImpot.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addAutreImpot(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("genresImpot", getGenresImpotPourForAutreImpot());
		model.addAttribute("command", new AddForAutreImpotView(tiersId));
		return "fors/addAutreImpot";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/addAutreImpot.do", method = RequestMethod.POST)
	public String addAutreImpot(@Valid @ModelAttribute("command") final AddForAutreImpotView view, BindingResult result, Model model) throws Exception {

		final long ctbId = view.getTiersId();

		// FIXME (msi) gérer la sécurité un peu plus sérieusement
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(ctbId);

		if (result.hasErrors()) {
			model.addAttribute("genresImpot", getGenresImpotPourForAutreImpot());
			return "fors/addAutreImpot";
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + ctbId + " n'existe pas.");
		}

		tiersService.openForFiscalAutreImpot(ctb, view.getGenreImpot(), view.getDateEvenement(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit.do?id=" + ctbId;
	}

	@RequestMapping(value = "/addDebiteur.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addDebiteur(@RequestParam(value = "tiersId", required = true) long tiersId, Model model) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("anneeMinimaleForDebiteur", paramService.getAnneeMinimaleForDebiteur());
		model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscaleDPI());
		model.addAttribute("command", new AddForDebiteurView(tiersId));
		return "fors/addDebiteur";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/addDebiteur.do", method = RequestMethod.POST)
	public String addDebiteur(@Valid @ModelAttribute("command") final AddForDebiteurView view, BindingResult result, Model model) throws Exception {

		final long dpiId = view.getTiersId();

		// FIXME (msi) gérer la sécurité un peu plus sérieusement
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des débiteurs de prestations imposables dans Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(dpiId);

		if (result.hasErrors()) {
			model.addAttribute("anneeMinimaleForDebiteur", paramService.getAnneeMinimaleForDebiteur());
			model.addAttribute("typesForFiscal", tiersMapHelper.getMapTypeAutoriteFiscaleDPI());
			return "fors/addDebiteur";
		}

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
		if (debiteur == null) {
			throw new ObjectNotFoundException("Le débiteur avec l'id=" + dpiId + " n'existe pas.");
		}

		tiersService.addForDebiteur(debiteur, view.getDateDebut(), view.getDateFin(), view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());

		return "redirect:/fiscal/edit-for-debiteur.do?id=" + dpiId;
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
					tiersMapHelper.getMapRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE, MotifRattachement.SEJOUR_SAISONNIER,
					                                  MotifRattachement.DIRIGEANT_SOCIETE);
		}
		return motifsRattachementForSecondaire;
	}

	private Map<MotifRattachement, String> getMotifsRattachementPourForAutreElementImposable() {
		if (motifsRattachementForAutreElementImposable == null) {
			motifsRattachementForAutreElementImposable =
					tiersMapHelper.getMapRattachement(MotifRattachement.ACTIVITE_LUCRATIVE_CAS, MotifRattachement.ADMINISTRATEUR, MotifRattachement.CREANCIER_HYPOTHECAIRE,
					                                  MotifRattachement.PRESTATION_PREVOYANCE, MotifRattachement.LOI_TRAVAIL_AU_NOIR);
		}
		return motifsRattachementForAutreElementImposable;
	}
}