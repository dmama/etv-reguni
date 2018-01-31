package ch.vd.uniregctb.annulation.deces;

import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.annulation.deces.manager.AnnulationDecesRecapManager;
import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.AbstractTiersController;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping("/annulation/deces")
@SessionAttributes(AnnulationDecesController. ANNULATION_DECES_CRITERIA)
public class AnnulationDecesController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour annuler un décès";
	public  static final String ANNULATION_DECES_CRITERIA = "AnnulationDecesCriteria";

	private AnnulationDecesRecapManager manager;
	private TiersCriteriaValidator validator;
	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;
	private ControllerUtils controllerUtils;

	public void setManager(AnnulationDecesRecapManager manager) {
		this.manager = manager;
	}

	public void setValidator(TiersCriteriaValidator validator) {
		this.validator = validator;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	@InitBinder(ANNULATION_DECES_CRITERIA)
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, "dateNaissanceInscriptionRC", new RegDateEditor(true, true, false));
		binder.registerCustomEditor(String.class, "numeroAVS", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				setValue(FormatNumeroHelper.removeSpaceAndDash(text));
			}

			@Override
			public String getAsText() {
				if (getValue() == null) {
					return "";
				}
				else {
					return super.getAsText();
				}
			}
		});
	}

	@ModelAttribute(ANNULATION_DECES_CRITERIA)
	public TiersCriteriaView newCriteresDeRechercheInitiaux() {
		TiersCriteriaView bean = new TiersCriteriaView();
		bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		return bean;
	}

	@ModelAttribute(AbstractTiersController.TYPE_RECHERCHE_NOM_MAP_NAME)
	public Map<TiersCriteria.TypeRecherche, String> referenceData () {
		return tiersMapHelper.getMapTypeRechercheNom();
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@SecurityCheck(rolesToCheck = {Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR},	accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String getListe(Model model, @ModelAttribute(ANNULATION_DECES_CRITERIA) TiersCriteriaView criteres, BindingResult result) {
		if (!criteres.isEmpty()) {
			try {
				final List<TiersIndexedDataView> list = searchDecedes(criteres);
				model.addAttribute("list", list);
			}
			catch (TooManyResultsIndexerException ee) {
				if (ee.getNbResults() > 0) {
					result.reject("error.preciser.recherche.trouves", new Object[] {String.valueOf(ee.getNbResults())}, null);
				}
				else {
					result.reject("error.preciser.recherche");
				}
			}
			catch (IndexerException e) {
				result.reject("error.recherche");
			}
		}
		return "annulation/deces/list";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	@Transactional(readOnly = true)
	@SecurityCheck(rolesToCheck = {Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR},	accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String postListe(@Valid @ModelAttribute(ANNULATION_DECES_CRITERIA) TiersCriteriaView criteresEnSession, BindingResult result) {
		if (result.hasErrors()) {
			return "annulation/deces/list";
		}
		return "redirect:list.do";
	}

	private List<TiersIndexedDataView> searchDecedes(TiersCriteriaView criteresEnSession) {

		final List<TiersIndexedData> results = tiersService.search(criteresEnSession.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			final Tiers tiers = tiersService.getTiers(d.getNumero());
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (tiersService.isDecede(pp) || tiersService.isVeuvageMarieSeul(pp)) {
					list.add(new TiersIndexedDataView(d));
				}
			}
		}
		return list;
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteresRechercheAnnulationDeces(Model model) {
		final TiersCriteriaView criteresDeRechercheInitiaux = newCriteresDeRechercheInitiaux();
		model.addAttribute(ANNULATION_DECES_CRITERIA, criteresDeRechercheInitiaux);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/recap.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String recap(ModelMap model, @RequestParam("numero") Long numero) {
		controllerUtils.checkAccesDossierEnLecture(numero);
		model.put("command", manager.get(numero));
		return "annulation/deces/recap";
	}

	@RequestMapping(value = "/recap.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String annuleDeces(@RequestParam("numero") Long numero) throws MetierServiceException {
		controllerUtils.checkAccesDossierEnEcriture(numero);
		controllerUtils.checkTraitementContribuableAvecDecisionAci(numero);
		final AnnulationDecesRecapView annulationDecesRecapView = manager.get(numero);
		manager.save(annulationDecesRecapView);
		return "redirect:/tiers/visu.do?id=" + annulationDecesRecapView.getPersonne().getNumero();
	}

}
