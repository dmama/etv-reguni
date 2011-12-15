package ch.vd.uniregctb.rapport;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.view.DebiteurView;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

@Controller
@RequestMapping(value = "/rapport")
public class RapportController {

	private TiersDAO tiersDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;

	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceCivilService serviceCivil;
	private ServiceCivilCacheWarmer cacheWarmer;
	private AutorisationManager autorisationManager;
	private TiersMapHelper tiersMapHelper;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheWarmer(ServiceCivilCacheWarmer cacheWarmer) {
		this.cacheWarmer = cacheWarmer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	/**
	 * Affiche les rapports d'un contribuable.
	 *
	 * @param tiersId   le numéro de tiers
	 * @param showHisto <b>vrai</b> s'il faut afficher les valeurs historiques; <b>faux</b> autrement.
	 * @param type      le type de rapport à afficher, ou <b>null</b> s'il faut afficher tous les types de rapports
	 * @param request   la requête http
	 * @param mav       le modèle sous-jacent
	 * @return le nom de la jsp qui affiche la liste retournée
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(@RequestParam("tiers") long tiersId, @RequestParam(value = "showHisto", required = false) Boolean showHisto,
	                   @RequestParam(value = "type", required = false) String type, HttpServletRequest request, Model mav) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.VISU_LIMITE) && !SecurityProvider.isGranted(Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
		}

		showHisto = (showHisto == null ? false : showHisto);
		final TypeRapportEntreTiers typeRapport = parseType(type);
		final WebParamPagination pagination = new WebParamPagination(request, "rapport", 10, "dateDebut", false);

		mav.addAttribute("tiersId", tiers.getNumero());
		mav.addAttribute("showHisto", showHisto);
		mav.addAttribute("rapportType", typeRapport == null ? null : typeRapport.name());
		mav.addAttribute("rapports", getRapportViews(tiers, showHisto, typeRapport, pagination));
		mav.addAttribute("rapportsTotalCount", getRapportsTotalCount(tiers, typeRapport, showHisto));
		mav.addAttribute("filiations", getFiliationViews(tiers));
		mav.addAttribute("debiteurs", getDebiteurViews(tiers));
		mav.addAttribute("typesRapportTiers", tiersMapHelper.getMapTypeRapportEntreTiers());
		mav.addAttribute("allowedOnglet", autorisationManager.getAutorisations(tiers));

		return "rapport/list";
	}

	private static TypeRapportEntreTiers parseType(String type) {
		if (StringUtils.isBlank(type)) {
			return null;
		}
		try {
			return TypeRapportEntreTiers.valueOf(type);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	private List<DebiteurView> getDebiteurViews(Tiers tiers) {

		if (!(tiers instanceof Contribuable)) {
			return null;
		}

		final List<DebiteurView> views = new ArrayList<DebiteurView>();

		final Contribuable contribuable = (Contribuable) tiers;
		final Set<RapportEntreTiers> rapports = contribuable.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (r instanceof ContactImpotSource) {
					final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(r.getObjetId());
					final DebiteurView view = new DebiteurView(dpi, (ContactImpotSource) r, adresseService);
					views.add(view);
				}
			}
		}

		return views;
	}

	private List<RapportView> getFiliationViews(Tiers tiers) {

		final List<RapportView> views = new ArrayList<RapportView>();

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.getNumeroIndividu() != null && pp.getNumeroIndividu() != 0) {
				final List<RapportView> rapportsFiliationView = getRapportsFiliation(pp);
				views.addAll(rapportsFiliationView);
			}
		}

		return views;
	}

	private int getRapportsTotalCount(Tiers tiers, TypeRapportEntreTiers type, boolean showHisto) {
		final boolean visuAll = SecurityProvider.isGranted(Role.VISU_ALL);
		return rapportEntreTiersDAO.countBySujetAndObjet(tiers.getNumero(), !visuAll, showHisto, type, tiers.getClass());
	}

	private List<RapportView> getRapportViews(Tiers tiers, boolean showHisto, TypeRapportEntreTiers type, WebParamPagination pagination) {

		final boolean visuAll = SecurityProvider.isGranted(Role.VISU_ALL);
		final Long tiersId = tiers.getNumero();

		final List<RapportView> views = new ArrayList<RapportView>();

		final List<RapportEntreTiers> rapports = rapportEntreTiersDAO.findBySujetAndObjet(tiersId, !visuAll, showHisto, type, tiers.getClass(), pagination);
		prechargeIndividus(rapports);

		for (RapportEntreTiers r : rapports) {
			if (r.getObjetId().equals(tiersId)) {
				views.add(new RapportView(r, SensRapportEntreTiers.OBJET, tiersService, adresseService));
			}
			else if (r.getSujetId().equals(tiersId)) {
				views.add(new RapportView(r, SensRapportEntreTiers.SUJET, tiersService, adresseService));
			}
		}

		return views;
	}

	private void prechargeIndividus(List<RapportEntreTiers> rapports) {
		if (serviceCivil.isWarmable()) {
			final Set<Long> tiersIds = new HashSet<Long>();
			for (RapportEntreTiers rapport : rapports) {
				tiersIds.add(rapport.getSujetId());
				tiersIds.add(rapport.getObjetId());
			}
			cacheWarmer.warmIndividusPourTiers(tiersIds, null, AttributeIndividu.ADRESSES);
		}
	}

	/**
	 * Recupère les rapports de filiation de type PARENT ou ENFANT
	 *
	 * @param habitant un habitant
	 * @return la liste des rapports de filiation trouvés
	 */
	private List<RapportView> getRapportsFiliation(PersonnePhysique habitant) {
		Assert.notNull(habitant.getNumeroIndividu(), "La personne physique n'a pas de numéro d'individu connu");

		final String nomInd = tiersService.getNomPrenom(habitant);

		final List<RapportView> list = new ArrayList<RapportView>();
		final List<RapportFiliation> filiations = tiersService.getRapportsFiliation(habitant);
		for (RapportFiliation filiation : filiations) {
			list.add(new RapportView(filiation, nomInd, tiersService));
		}

		return list;
	}
}
