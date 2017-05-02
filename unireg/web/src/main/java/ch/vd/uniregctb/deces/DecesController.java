package ch.vd.uniregctb.deces;

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
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.deces.view.DecesRecapView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.RegDateEditor;

import static ch.vd.uniregctb.tiers.AbstractTiersController.TYPE_RECHERCHE_NOM_MAP_NAME;

@Controller
@RequestMapping(value = "/deces")
public class DecesController {

	protected final Logger LOGGER = LoggerFactory.getLogger(DecesController.class);

	public static final String DECES_LIST_ATTRIBUTE_NAME = "list";

	private TiersService tiersService;
	private MetierService metierService;
	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private Validator criteriaValidator;
	private Validator recapValidator;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setCriteriaValidator(Validator criteriaValidator) {
		this.criteriaValidator = criteriaValidator;
	}

	public void setRecapValidator(Validator recapValidator) {
		this.recapValidator = recapValidator;
	}

	@InitBinder(value = "criteria")
	public void initBinderForCriteria(WebDataBinder binder) {
		binder.setValidator(criteriaValidator);
		// le critère de recherche sur la date de naissance peut être une date partielle
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
		binder.registerCustomEditor(RegDate.class, "dateNaissanceInscriptionRC", new RegDateEditor(true, true, false));
	}

	/**
	 * Affiche l'écran de recherche d''une personne physique
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String search(@Valid @ModelAttribute(value = "criteria") TiersCriteriaView criteria, BindingResult binding, Model model) throws Exception {

		model.addAttribute(TYPE_RECHERCHE_NOM_MAP_NAME, tiersMapHelper.getMapTypeRechercheNom());

		if (binding.hasErrors() || criteria.isEmpty()) {
			return "tiers/edition/deces/list";
		}

		// on restreint la recherche aux personnes physiques
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())) {
			criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(criteria.getNumeroAVS()));
		}
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);

		// on effectue la recherche
		try {
			final List<TiersIndexedDataView> results = tiersService.search(criteria.asCore()).stream()
					.map(TiersIndexedDataView::new)
					.filter(d -> StringUtils.isBlank(d.getDateDeces()))
					.collect(Collectors.toList());

			model.addAttribute(DECES_LIST_ATTRIBUTE_NAME, results);
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

		return "tiers/edition/deces/list";
	}

	/**
	 * Affiche l'écran de récapitulation avant validation du décès
	 */
	@RequestMapping(value = "/recap.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String recap(@RequestParam("numero") long tiersId, Model model) throws Exception {

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final DecesRecapView view = new DecesRecapView(tiersId, tiersService);
		model.addAttribute("recap", view);
		return "tiers/edition/deces/recap";
	}

	@InitBinder(value = "recap")
	public void initBinderForRecap(WebDataBinder binder) {
		binder.setValidator(recapValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Enregistre le décès
	 */
	@RequestMapping(value = "/recap.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String recap(@Valid @ModelAttribute("recap") DecesRecapView view, BindingResult binding) throws Exception {

		final Long tiersId = view.getTiersId();

		if (binding.hasErrors()) {
			return "tiers/edition/deces/recap";
		}

		// préconditions
		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		controllerUtils.checkTraitementContribuableAvecDecisionAci(tiersId);

		// on enregistre le décès
		final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersId);
		if (view.isMarieSeul() && view.isVeuf()) {
			metierService.veuvage(pp, RegDateHelper.get(view.getDateDeces()), view.getRemarque(), null);
			Flash.message("Le veuvage du tiers n°" + tiersId + " a bien été saisi.");
		}
		else {
			metierService.deces(pp, RegDateHelper.get(view.getDateDeces()), view.getRemarque(), null);
			Flash.message("Le décès du tiers n°" + tiersId + " a bien été saisi.");
		}

		return "redirect:/tiers/visu.do?id=" + tiersId;
	}
}