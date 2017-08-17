package ch.vd.uniregctb.registrefoncier.situation.surcharge;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.EntiteOFS;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.SituationRFDAO;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.view.CommuneView;

/**
 * Contrôleur de gestion des surcharges de communes sur les situations RF (cas des fractions de communes).
 */
@RequestMapping(value = "/registrefoncier/situation/surcharge")
public class SituationSurchargeRFController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec de gestion des fractions de communes RF";
	private static final String TABLE_SITUATIONS = "situation";
	private static final int SITUATION_PAGE_SIZE = 25;

	private ServiceInfrastructureService serviceInfrastructureService;
	private SituationRFDAO situationRFDAO;
	private RegistreFoncierService registreFoncierService;
	private String urlGeoVD;

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setSituationRFDAO(SituationRFDAO situationRFDAO) {
		this.situationRFDAO = situationRFDAO;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public void setUrlGeoVD(String urlGeoVD) {
		this.urlGeoVD = urlGeoVD;
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(Model model, HttpServletRequest request) {

		final List<Commune> faitieres = serviceInfrastructureService.getListeCommunesFaitieres();
		final List<Integer> noOfsCommunes = faitieres.stream()
				.map(EntiteOFS::getNoOFS)
				.collect(Collectors.toList());

		final WebParamPagination pagination = new WebParamPagination(request, TABLE_SITUATIONS, SITUATION_PAGE_SIZE);

		final List<SituationRF> situations = situationRFDAO.findSituationNonSurchargeesSurCommunes(noOfsCommunes, pagination);
		final List<SituationSummaryView> views = situations.stream()
				.map(situation -> new SituationSummaryView(situation, serviceInfrastructureService))
				.collect(Collectors.toList());
		final int count = situationRFDAO.countSituationsNonSurchargeesSurCommunes(noOfsCommunes);

		model.addAttribute("situations", views);
		model.addAttribute("count", count);
		model.addAttribute("pageSize", SITUATION_PAGE_SIZE);

		return "registrefoncier/situation/surcharge/list";
	}

	@RequestMapping(value = "/show.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String show(Model model, @RequestParam(value = "id") long idSituation) {

		final SituationRF situation = situationRFDAO.get(idSituation);
		if (situation == null) {
			throw new ObjectNotFoundException("La situation avec l'id=[" + idSituation + "] n'existe pas.");
		}
		initModelForCurrentSituation(model, situation);


		model.addAttribute("surcharge", new SituationSurchargeView(situation));

		return "registrefoncier/situation/surcharge/show";
	}

	private void initModelForCurrentSituation(@NotNull Model model, @NotNull SituationRF situation) {

		// on construit la map commune faîtière -> liste des fractions
		final List<Commune> fractions = serviceInfrastructureService.getListeFractionsCommunes();
		final Map<Integer, List<CommuneView>> mapFaitieresFractions = new HashMap<>();
		fractions.forEach(fraction -> mapFaitieresFractions.merge(fraction.getOfsCommuneMere(),
		                                                          Collections.singletonList(new CommuneView(fraction.getNoOFS(), fraction.getNomOfficiel())),
		                                                          ListUtils::union));

		// La situation courante
		final SituationFullView currentSituation = new SituationFullView(situation, registreFoncierService::getCapitastraURL);

		// Les autres situations
		final List<SituationSummaryView> otherSituations = situation.getImmeuble().getSituations().stream()
				.filter(s -> !s.getId().equals(situation.getId()))
				.map(s -> new SituationSummaryView(s, serviceInfrastructureService))
				.collect(Collectors.toList());

		model.addAttribute("currentSituation", currentSituation);
		model.addAttribute("otherSituations", otherSituations);
		model.addAttribute("mapFaitieresFractions", mapFaitieresFractions);
		model.addAttribute("urlGeoVD", urlGeoVD);
	}

	@InitBinder(value = "surcharge")
	public void initSearchCommandBinder(WebDataBinder binder) {
		binder.setValidator(new SituationSurchargeValidator());
	}

	@SecurityCheck(rolesToCheck = {Role.GEST_FRACTIONS_COMMUNE_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/show.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String apply(@Valid @ModelAttribute("surcharge") final SituationSurchargeView surcharge, BindingResult result, Model model) {

		final SituationRF situation = situationRFDAO.get(surcharge.getSituationId());
		if (situation == null) {
			throw new ObjectNotFoundException("La situation avec l'id=[" + surcharge.getSituationId() + "] n'existe pas.");
		}

		if (result.hasErrors()) {
			// erreurs : on réaffiche le formulaire
			initModelForCurrentSituation(model, situation);
			model.addAttribute("surcharge", surcharge);
			return "registrefoncier/situation/surcharge/show";
		}

		// on met-à-jour la situation
		situation.setNoOfsCommuneSurchargee(surcharge.getNoOfsSurcharge());
		Flash.message("La fraction de commune a bien été renseignée sur la parcelle n°" + situation.getNoParcelle() + " de la commune " + situation.getCommune().getNomRf(), 4000);

		return "redirect:/registrefoncier/situation/surcharge/list.do";
	}
}
