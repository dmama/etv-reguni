package ch.vd.uniregctb.contribuableAssocie;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieEditView;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieListView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.param.manager.ParamApplicationManager;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.RegDateEditor;

import static ch.vd.uniregctb.tiers.AbstractTiersController.PARAMETRES_APP;
import static ch.vd.uniregctb.tiers.AbstractTiersController.TYPE_RECHERCHE_NOM_MAP_NAME;

@Controller
@RequestMapping(value = "/contribuable-associe")
public class ContribuableAssocieController {

	protected final Logger LOGGER = LoggerFactory.getLogger(ContribuableAssocieController.class);

	private TiersService tiersService;
	private ParamApplicationManager paramApplicationManager;
	private Validator criteriaValidator;
	private TiersMapHelper tiersMapHelper;
	private ControllerUtils controllerUtils;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setParamApplicationManager(ParamApplicationManager paramApplicationManager) {
		this.paramApplicationManager = paramApplicationManager;
	}

	public void setCriteriaValidator(Validator criteriaValidator) {
		this.criteriaValidator = criteriaValidator;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	@InitBinder(value = "command")
	public void initBinderForCriteria(WebDataBinder binder) {
		binder.setValidator(criteriaValidator);
		// le critère de recherche sur la date de naissance peut être une date partielle
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
		binder.registerCustomEditor(RegDate.class, "dateNaissanceInscriptionRC", new RegDateEditor(true, true, false));
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String show(@Valid @ModelAttribute("command") ContribuableAssocieListView bean, BindingResult binding, Model model) {

		if (bean.getNumeroDpi() == 0) {
			throw new IllegalArgumentException("Le numéro de débiteur doit être renseigné.");
		}
		controllerUtils.checkAccesDossierEnLecture(bean.getNumeroDpi());

		// on restreint la recherche aux contribuables
		model.addAttribute(PARAMETRES_APP, paramApplicationManager.getForm());
		model.addAttribute(TYPE_RECHERCHE_NOM_MAP_NAME, tiersMapHelper.getMapTypeRechercheNom());

		if (binding.hasErrors() || bean.isEmpty()) {
			return "tiers/edition/contribuable-associe/list";
		}

		if (StringUtils.isNotBlank(bean.getNumeroAVS())) {
			bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
		}
		bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.CONTRIBUABLE);

		// on effectue la recherche
		try {
			final List<TiersIndexedDataView> results = tiersService.search(bean.asCore()).stream()
					.map(TiersIndexedDataView::new)
					.filter(d -> StringUtils.isBlank(d.getDateDeces()))
					.collect(Collectors.toList());

			model.addAttribute("list", results);
		}
		catch (TooManyResultsIndexerException ee) {
			LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
			if (ee.getNbResults() > 0) {
				binding.reject("error.preciser.recherche.trouves", new Object[]{String.valueOf(ee.getNbResults())}, null);
			}
			else {
				binding.reject("error.preciser.recherche");
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			binding.reject("error.recherche");
		}

		return "tiers/edition/contribuable-associe/list";
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String recap(@RequestParam("numeroDpi") long numeroDpi, @RequestParam("numeroContribuable") long numeroContribuable, Model model) {

		controllerUtils.checkAccesDossierEnLecture(numeroDpi);
		controllerUtils.checkAccesDossierEnLecture(numeroContribuable);

		model.addAttribute("recap", new ContribuableAssocieEditView(numeroDpi, numeroContribuable));
		return "tiers/edition/contribuable-associe/edit";
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String associate(@ModelAttribute("recap") ContribuableAssocieEditView view) {

		final long numeroDpi = view.getNumeroDpi();
		final long numeroContribuable = view.getNumeroContribuable();

		controllerUtils.checkAccesDossierEnEcriture(numeroDpi);
		controllerUtils.checkAccesDossierEnEcriture(numeroContribuable);

		// on associe le contribuable au débiteur
		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi);
		final Contribuable contribuable = (Contribuable) tiersService.getTiers(numeroContribuable);
		tiersService.addContactImpotSource(debiteur, contribuable);

		return "redirect:/tiers/visu.do?id=" + numeroContribuable;
	}
}
