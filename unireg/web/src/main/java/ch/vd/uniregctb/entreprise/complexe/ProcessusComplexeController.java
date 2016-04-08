package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
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
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/processuscomplexe")
public class ProcessusComplexeController implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessusComplexeController.class);

	public static final String FAILLITE_CRITERIA_NAME = "FailliteCriteria";

	private static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	private static final String LIST = "list";
	private static final String COMMAND = "command";
	private static final String ERROR_MESSAGE = "errorMessage";

	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;
	private MessageSource messageSource;
	private Validator validator;
	private PlatformTransactionManager transactionManager;
	private MetierServicePM metierService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setMetierService(MetierServicePM metierService) {
		this.metierService = metierService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private static void checkDroitAccessFaillite(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.FAILLITE_ENTREPRISE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe de faillite d'entreprise.");
		}
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	private List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteriaView) {
		final List<TiersIndexedData> results = tiersService.search(criteriaView.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}
		return list;
	}

	@RequestMapping(value = "/faillite/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheFaillite(Model model, HttpSession session) {
		checkDroitAccessFaillite(securityProvider);
		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(FAILLITE_CRITERIA_NAME);
		return showRechercheFaillite(model, criteria, false);
	}

	@RequestMapping(value = "/faillite/reset-search.do", method = RequestMethod.GET)
	public String resetCriteriaFaillite(HttpSession session) {
		checkDroitAccessFaillite(securityProvider);
		session.removeAttribute(FAILLITE_CRITERIA_NAME);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/faillite/list.do", method = RequestMethod.POST)
	public String doRechercheFaillite(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAccessFaillite(securityProvider);
		if (bindingResult.hasErrors()) {
			return showRechercheFaillite(model, view, true);
		}
		else {
			session.setAttribute(FAILLITE_CRITERIA_NAME, view);
		}
		return "redirect:list.do";
	}

	private String showRechercheFaillite(Model model, @Nullable TiersCriteriaView criteria, boolean error) {
		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro IDE
			if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
				criteria.setNumeroIDE(NumeroIDEHelper.normalize(criteria.getNumeroIDE()));
			}

			criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
			criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
			criteria.setEtatsEntrepriseCourantsInterdits(EnumSet.of(TypeEtatEntreprise.EN_FAILLITE));
			try {
				final List<TiersIndexedDataView> results = searchTiers(criteria);
				model.addAttribute(LIST, results);
			}
			catch (TooManyResultsIndexerException e) {
				LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
				final String msg;
				if (e.getNbResults() > 0) {
					msg = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(e.getNbResults())}, WebContextUtils.getDefaultLocale());
				}
				else {
					msg = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
				}
				model.addAttribute(ERROR_MESSAGE, msg);
			}
			catch (IndexerException e) {
				LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
				model.addAttribute(ERROR_MESSAGE, messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale()));
			}
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return "entreprise/faillite/list";
	}

	@RequestMapping(value = "/faillite/start.do", method = RequestMethod.GET)
	public String showStartProcessusFaillite(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAccessFaillite(securityProvider);
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStartProcessusFaillite(model, new FailliteView(idEntreprise));
	}

	private String showStartProcessusFaillite(Model model, FailliteView view) {
		model.addAttribute(COMMAND, view);
		return "entreprise/faillite/start";
	}

	@RequestMapping(value = "/faillite/start.do", method = RequestMethod.POST)
	public String doFaillite(Model model, @Valid @ModelAttribute(value = COMMAND) final FailliteView view, BindingResult bindingResult) throws Exception {
		checkDroitAccessFaillite(securityProvider);
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStartProcessusFaillite(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		try {
			try {
				template.execute(new TxCallbackWithoutResult() {
					@Override
					public void execute(TransactionStatus status) throws Exception {
						final Tiers tiers = tiersService.getTiers(view.getIdEntreprise());
						if (tiers == null || !(tiers instanceof Entreprise)) {
							throw new TiersNotFoundException(view.getIdEntreprise());
						}

						final Entreprise entreprise = (Entreprise) tiers;
						metierService.faillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
					}
				});
			}
			catch (TxCallbackException e) {
				try {
					throw e.getCause();
				}
				catch (Exception | Error ee) {
					throw ee;
				}
				catch (Throwable t) {
					throw new RuntimeException(t);
				}
			}
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
