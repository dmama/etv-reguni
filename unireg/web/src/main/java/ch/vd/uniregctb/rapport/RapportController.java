package ch.vd.uniregctb.rapport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
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
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;

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
	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Retourne les rapports d'un contribuable page par page et sous format JSON.
	 *
	 * @param tiersId   le numéro de tiers
	 * @param showHisto <b>vrai</b> s'il faut afficher les valeurs historiques; <b>faux</b> autrement.
	 * @param type      le type de rapport à afficher, ou <b>null</b> s'il faut afficher tous les types de rapports
	 * @param sortField le champ sur lequel le tri doit être fait
	 * @param sortOrder le sens du tri ('ASC' ou 'DESC')
	 * @param page      le numéro de page à retourner
	 * @param pageSize  la taille des pages
	 * @return les informations nécessaire à l'affichage d'une page de rapports du contribuable.
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/rapports.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage rapports(@RequestParam("tiers") long tiersId,
	                             @RequestParam(value = "showHisto", required = false, defaultValue = "false") boolean showHisto,
	                             @RequestParam(value = "type", required = false) String type,
	                             @RequestParam(value = "sortField", required = false) String sortField,
	                             @RequestParam(value = "sortOrder", required = false) String sortOrder,
	                             @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                             @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
		}

		final boolean excludeContactImpotSource = (tiers instanceof Contribuable);
		final boolean excludeRapportPrestationImposable = (tiers instanceof DebiteurPrestationImposable);
		final TypeRapportEntreTiers typeRapport = parseType(type);
		final int totalCount = getRapportsTotalCount(tiers, typeRapport, showHisto, excludeContactImpotSource);

		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		final ParamPagination pagination = new ParamPagination(page, pageSize, sortField, "ASC".equalsIgnoreCase(sortOrder));

		final Map<TypeRapportEntreTiers, String> typeRapportEntreTiers = tiersMapHelper.getMapTypeRapportEntreTiers();
		final List<RapportsPage.RapportView> views = getRapportViews(tiersId, showHisto, excludeContactImpotSource, excludeRapportPrestationImposable, typeRapport, pagination);

		return new RapportsPage(tiersId, views, showHisto, typeRapport, typeRapportEntreTiers, page, totalCount, sortField, sortOrder);
	}

	/**
	 * Retourne toutes les filiations (= liens vers les parents et les enfants) d'un contribuable sous format JSON.
	 *
	 * @param tiersId le numéro de tiers
	 * @return une liste de filiations sous format JSON; ou un message d'erreur en cas de doublon sur les individus.
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/filiations.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public Object filiations(@RequestParam("tiers") long tiersId) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
		}

		final List<FiliationView> views = new ArrayList<FiliationView>();

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isConnuAuCivil()) {
				try {
					final String nomInd = tiersService.getNomPrenom(pp);

					final List<RapportFiliation> filiations = tiersService.getRapportsFiliation(pp);
					for (RapportFiliation filiation : filiations) {
						views.add(new FiliationView(filiation, nomInd, tiersService));
					}
				}
				catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
					return e.getMessage();
				}
			}
		}

		return views;
	}

	/**
	 * Retourne toutes les débiteurs associés à un contribuable sous format JSON.
	 *
	 * @param tiersId le numéro de tiers
	 * @return une liste de liens vers les débiteurs associés sous format JSON
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/debiteurs.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public List<DebiteurView> debiteurs(@RequestParam("tiers") long tiersId) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
		}

		return getDebiteurViews(tiers);
	}

	private List<RapportsPage.RapportView> getRapportViews(long tiersId, boolean showHisto, boolean excludeContactImpotSource, boolean excludeRapportPrestationImposable,
	                                                       TypeRapportEntreTiers typeRapport, ParamPagination pagination) {
		final boolean visuAll = SecurityProvider.isGranted(Role.VISU_ALL);

		final List<RapportsPage.RapportView> views = new ArrayList<RapportsPage.RapportView>();

		final List<RapportEntreTiers> rapports =
				rapportEntreTiersDAO.findBySujetAndObjet(tiersId, !visuAll, showHisto, typeRapport, pagination, excludeRapportPrestationImposable, excludeContactImpotSource);
		if (rapports.size() > 5) {
			prechargeIndividus(rapports);
		}

		for (RapportEntreTiers r : rapports) {
			if (r.getObjetId().equals(tiersId)) {
				views.add(new RapportsPage.RapportView(r, SensRapportEntreTiers.OBJET, tiersService, adresseService, messageSource));
			}
			else if (r.getSujetId().equals(tiersId)) {
				views.add(new RapportsPage.RapportView(r, SensRapportEntreTiers.SUJET, tiersService, adresseService, messageSource));
			}
		}
		return views;
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
					final DebiteurView view = new DebiteurView(dpi, (ContactImpotSource) r, adresseService, messageSource);
					views.add(view);
				}
			}
		}

		return views;
	}

	private int getRapportsTotalCount(Tiers tiers, TypeRapportEntreTiers type, boolean showHisto, final boolean excludeContactImpotSource) {
		final boolean visuAll = SecurityProvider.isGranted(Role.VISU_ALL);
		final boolean excludePrestationImposable = DebiteurPrestationImposable.class.equals(tiers.getClass());
		return rapportEntreTiersDAO.countBySujetAndObjet(tiers.getNumero(), !visuAll, showHisto, type, excludePrestationImposable,
				excludeContactImpotSource);
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
}
