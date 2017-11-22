package ch.vd.uniregctb.registrefoncier.communaute;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.PrincipalCommunauteRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.RegroupementCommunauteRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ModeleCommunauteRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.PrincipalCommunauteRFDAO;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.RegDateEditor;

import static ch.vd.uniregctb.tiers.AbstractTiersController.TYPE_RECHERCHE_NOM_MAP_NAME;

@RequestMapping(value = "/registrefoncier/communaute")
public class CommunauteRFController {

	private final Logger LOGGER = LoggerFactory.getLogger(CommunauteRFController.class);

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec d'élection du principal de communauté RF";

	private AyantDroitRFDAO ayantDroitDAO;
	private ModeleCommunauteRFDAO modeleCommunauteDAO;
	private PrincipalCommunauteRFDAO principalCommunauteRFDAO;
	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;
	private Validator criteriaValidator;
	private Validator addPrincipalViewValidator;
	private RegistreFoncierService registreFoncierService;


	@InitBinder(value = "criteria")
	public void initBinderForCriteria(WebDataBinder binder) {
		binder.setValidator(criteriaValidator);
		// le critère de recherche sur la date de naissance peut être une date partielle
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
		binder.registerCustomEditor(RegDate.class, "dateNaissanceInscriptionRC", new RegDateEditor(true, true, false));
	}

	@InitBinder(value = "addPrincipalView")
	public void initBinderForAddPrincipalView(WebDataBinder binder) {
		binder.setValidator(addPrincipalViewValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(false, false, false));
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetSearchCriteria(HttpSession session) {
		return "redirect:searchTiers.do";
	}

	/**
	 * Affiche l'écran de recherche d'une personne physique ou morale
	 */
	@RequestMapping(value = "/searchTiers.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String searchTiers(@Valid @ModelAttribute(value = "criteria") TiersCriteriaView criteria, BindingResult binding, Model model) throws Exception {

		model.addAttribute(TYPE_RECHERCHE_NOM_MAP_NAME, tiersMapHelper.getMapTypeRechercheNom());

		if (binding.hasErrors() || criteria.isEmpty()) {
			return "registrefoncier/communaute/searchTiers";
		}

		// on restreint la recherche aux personnes physiques et morales
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())) {
			criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(criteria.getNumeroAVS()));
		}
		criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, TiersCriteria.TypeTiers.ENTREPRISE));

		// on effectue la recherche
		try {
			final List<TiersIndexedDataWithCommunauteView> results = tiersService.search(criteria.asCore()).stream()
					.map(this::buildView)
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

		return "registrefoncier/communaute/searchTiers";
	}

	@RequestMapping(value = "/showTiers.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showTiers(@RequestParam(value = "id") long id, Model model) {

		final TiersWithCommunauteView view = buildView(id);
		model.addAttribute("tiers", view);

		return "registrefoncier/communaute/showTiers";
	}

	@RequestMapping(value = "/showModele.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showModele(@RequestParam(value = "id") long id, Model model) {

		final ModeleCommunauteRF modele = modeleCommunauteDAO.get(id);
		if (modele == null) {
			throw new ObjectNotFoundException("Le modèle de communauté avec l'id=[" + id + "] n'existe pas");
		}
		model.addAttribute("modele", new ModeleCommunauteView(modele, tiersService, registreFoncierService));

		return "registrefoncier/communaute/showModele";
	}

	@RequestMapping(value = "/addPrincipal.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String addPrincipal(@RequestParam(value = "modeleId") long modeleId, @RequestParam(value = "membreId") long membreId, Model model) {

		final ModeleCommunauteRF modele = modeleCommunauteDAO.get(modeleId);
		if (modele == null) {
			throw new ObjectNotFoundException("Le modèle de communauté avec l'id=[" + modeleId + "] n'existe pas");
		}

		final TiersRF membre = (TiersRF) ayantDroitDAO.get(membreId);
		if (membre == null) {
			throw new ObjectNotFoundException("L'ayant-droit avec l'id=[" + membreId + "] n'existe pas");
		}

		model.addAttribute("modele", new ModeleCommunauteView(modele, tiersService, registreFoncierService));
		model.addAttribute("membre", new MembreCommunauteView(membre, tiersService, registreFoncierService));
		model.addAttribute("addPrincipalView", new AddPrincipalView(modeleId, membreId, null));

		return "registrefoncier/communaute/addPrincipal";
	}

	@RequestMapping(value = "/addPrincipal.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String addPrincipal(@Valid @ModelAttribute AddPrincipalView view, BindingResult bindingResult, Model model) {

		final long modeleId = view.getModeleId();
		final long membreId = view.getMembreId();

		final ModeleCommunauteRF modele = modeleCommunauteDAO.get(modeleId);
		if (modele == null) {
			throw new ObjectNotFoundException("Le modèle de communauté avec l'id=[" + modeleId + "] n'existe pas");
		}

		final TiersRF membre = (TiersRF) ayantDroitDAO.get(membreId);
		if (membre == null) {
			throw new ObjectNotFoundException("L'ayant-droit avec l'id=[" + membreId + "] n'existe pas");
		}

		// validation supplémentaire pour éviter les doublons sur les périodes
		modele.getPrincipaux().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(p -> Objects.equals(p.getDateDebut().year(), view.getPeriodeDebut()))
				.findAny()
				.ifPresent(p -> bindingResult.rejectValue("periodeDebut", "error.principal.existe.deja.periode"));

		if (bindingResult.hasErrors()) {
			model.addAttribute("modele", new ModeleCommunauteView(modele, tiersService, registreFoncierService));
			model.addAttribute("membre", new MembreCommunauteView(membre, tiersService, registreFoncierService));
			return "registrefoncier/communaute/addPrincipal";
		}

		// on ajoute le principal
		registreFoncierService.addPrincipalToModeleCommunaute(membre, modele, RegDate.get(view.getPeriodeDebut(), 1, 1));

		final Contribuable ctb = membre.getCtbRapproche();
		Flash.message("Le membre n°" + (ctb == null ? 0 : ctb.getId()) + " a été ajouté comme principal depuis la période " + view.getPeriodeDebut());
		return "redirect:/registrefoncier/communaute/showModele.do?id=" + modeleId;
	}

	@RequestMapping(value = "/cancelPrincipal.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.ELECTION_PRINCIPAL_COMMUNAUTE_RF, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String cancelPrincipal(@RequestParam(value = "id") long id, Model model) {

		final PrincipalCommunauteRF principal = principalCommunauteRFDAO.get(id);
		if (principal == null) {
			throw new ObjectNotFoundException("Le principal de communauté avec l'id=[" + id + "] n'existe pas");
		}

		// on annule le principal
		registreFoncierService.cancelPrincipalCommunaute(principal);

		final Contribuable ctb = principal.getPrincipal().getCtbRapproche();
		Flash.message("Le principal n°" + (ctb == null ? 0 : ctb.getId()) + " a été annulé.");
		return "redirect:/registrefoncier/communaute/showModele.do?id=" + principal.getModeleCommunaute().getId();
	}

	@NotNull
	private TiersIndexedDataWithCommunauteView buildView(@NotNull TiersIndexedData data) {

		// on va chercher la liste des communautés du contribuable
		final Contribuable ctb = (Contribuable) tiersService.getTiers(data.getNumero());

		final long modeleCount;
		if (ctb == null) {
			modeleCount = 0;
		}
		else {
			// on détermine la liste des communautés dans lesquels apparaît le contribuable
			modeleCount = ctb.getRapprochementsRFNonAnnulesTries().stream()
					.map(RapprochementRF::getTiersRF)
					.map(AyantDroitRF::getDroitsPropriete)
					.flatMap(Collection::stream)
					.filter(DroitProprietePersonneRF.class::isInstance)
					.map(DroitProprietePersonneRF.class::cast)
					.map(DroitProprietePersonneRF::getCommunaute)
					.filter(Objects::nonNull)
					.filter(AnnulableHelper::nonAnnule)
					.map(CommunauteRF::getRegroupements)
					.flatMap(Collection::stream)
					.filter(AnnulableHelper::nonAnnule)
					.map(RegroupementCommunauteRF::getModele)
					.distinct()
					.count();

		}

		return new TiersIndexedDataWithCommunauteView(data, modeleCount);
	}

	@NotNull
	private TiersWithCommunauteView buildView(long ctbId) {

		// on va chercher la liste des communautés du contribuable
		final Contribuable ctb = (Contribuable) tiersService.getTiers(ctbId);
		if (ctb == null) {
			throw new TiersNotFoundException(ctbId);
		}

		// on détermine la liste des communautés dans lesquels apparaît le contribuable
		final List<CommunauteRF> communautes = ctb.getRapprochementsRFNonAnnulesTries().stream()
				.map(RapprochementRF::getTiersRF)
				.map(AyantDroitRF::getDroitsPropriete)
				.flatMap(Collection::stream)
				.filter(DroitProprietePersonneRF.class::isInstance)
				.map(DroitProprietePersonneRF.class::cast)
				.map(DroitProprietePersonneRF::getCommunaute)
				.filter(Objects::nonNull)
				.filter(AnnulableHelper::nonAnnule)
				.distinct()
				.collect(Collectors.toList());

		// on regroupe ces communautés par modèle de communauté
		final Map<Long, ModeleCommunauteForTiersView> map = new HashMap<>();
		communautes.stream()
				.map(CommunauteRF::getRegroupements)
				.flatMap(Collection::stream)
				.filter(AnnulableHelper::nonAnnule)
				.forEach(r -> {
					final ModeleCommunauteRF modele = r.getModele();
					final ModeleCommunauteForTiersView modeleView = map.computeIfAbsent(modele.getId(),
					                                                                    k -> new ModeleCommunauteForTiersView(ctb.getNumero(), modele, tiersService, registreFoncierService));
					modeleView.addRegroupement(new RegroupementRFView(r, registreFoncierService));
				});

		final List<ModeleCommunauteForTiersView> modeles = map.values().stream()
				.sorted(new ModeleCommunauteForTiersComparator())
				.collect(Collectors.toList());

		return new TiersWithCommunauteView(ctbId, modeles);
	}

	/**
	 * Trie les communautés par ordre croissant de date de début de valditié (basé sur les informations de regroupement).
	 */
	private class ModeleCommunauteForTiersComparator implements Comparator<ModeleCommunauteForTiersView> {
		@Override
		public int compare(@NotNull ModeleCommunauteForTiersView o1, @NotNull ModeleCommunauteForTiersView o2) {
			final RegDate dateDebut1 = getDateDebutValidite(o1);
			final RegDate dateDebut2 = getDateDebutValidite(o2);
			return NullDateBehavior.EARLIEST.compare(dateDebut1, dateDebut2);
		}

		private RegDate getDateDebutValidite(@NotNull ModeleCommunauteForTiersView o1) {
			RegDate dateDebut = RegDateHelper.getLateDate();
			for (RegroupementRFView r : o1.getRegroupements()) {
				dateDebut = RegDateHelper.minimum(dateDebut, r.getDateDebut(), NullDateBehavior.EARLIEST);
			}
			return dateDebut;
		}
	}

	public void setAyantDroitDAO(AyantDroitRFDAO ayantDroitDAO) {
		this.ayantDroitDAO = ayantDroitDAO;
	}

	public void setModeleCommunauteDAO(ModeleCommunauteRFDAO modeleCommunauteDAO) {
		this.modeleCommunauteDAO = modeleCommunauteDAO;
	}

	public void setPrincipalCommunauteRFDAO(PrincipalCommunauteRFDAO principalCommunauteRFDAO) {
		this.principalCommunauteRFDAO = principalCommunauteRFDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setCriteriaValidator(Validator criteriaValidator) {
		this.criteriaValidator = criteriaValidator;
	}

	public void setAddPrincipalViewValidator(Validator addPrincipalViewValidator) {
		this.addPrincipalViewValidator = addPrincipalViewValidator;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}
}
