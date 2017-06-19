package ch.vd.uniregctb.rapport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
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
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportEntreTiersKey;
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
	private ServiceCivilCacheWarmer cacheWarmer;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

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

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	private Set<RapportEntreTiersKey> getAllowedTypes() {
		final Set<RapportEntreTiersKey> types;
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			types = RapportHelper.ALLOWED_VISU_LIMITEE;
		}
		else {
			types = RapportHelper.ALLOWED_VISU_COMPLETE;
		}
		return types;
	}

	private static Set<RapportEntreTiersKey> buildTypeSet(@Nullable TypeRapportEntreTiers askedFor, Set<RapportEntreTiersKey> allowed) {
		final Set<RapportEntreTiersKey> types = new HashSet<>(allowed);
		if (askedFor != null) {
			final Iterator<RapportEntreTiersKey> iterator = types.iterator();
			while (iterator.hasNext()) {
				final RapportEntreTiersKey key = iterator.next();
				if (key.getType() != askedFor) {
					iterator.remove();
				}
			}
		}
		return types.isEmpty() ? allowed : types;
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les rapports entre tiers d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final TypeRapportEntreTiers typeRapport = parseType(type);
		final Set<RapportEntreTiersKey> allowedKeys = getAllowedTypes();
		final Set<RapportEntreTiersKey> keys = buildTypeSet(typeRapport, allowedKeys);
		final int totalCount = getRapportsTotalCount(tiers, showHisto, keys);

		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		final ParamPagination pagination = new ParamPagination(page, pageSize, sortField, "ASC".equalsIgnoreCase(sortOrder));

		final Map<TypeRapportEntreTiers, String> allTypes = tiersMapHelper.getMapTypeRapportEntreTiers();
		final Map<TypeRapportEntreTiers, String> choosableTypes = new LinkedHashMap<>(allTypes);
		final Set<TypeRapportEntreTiers> allowedTypes = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : allowedKeys) {
			allowedTypes.add(key.getType());
		}
		choosableTypes.keySet().retainAll(allowedTypes);

		final List<RapportsPage.RapportView> views = getRapportViews(tiersId, showHisto, keys, pagination, false);
		return new RapportsPage(tiersId, views, showHisto, typeRapport, choosableTypes, page, totalCount, sortField, sortOrder);
	}

	/**
	 * Retourne les parentés d'un contribuable sous format JSON.
	 *
	 * @param tiersId   le numéro de tiers
	 * @return les informations nécessaire à l'affichage d'une page de parentés du contribuable.
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/parentes.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage parentes(@RequestParam("tiers") long tiersId) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les parentés d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final int totalCount = getRapportsTotalCount(tiers, true, RapportHelper.ALLOWED_PARENTES);

		final ParamPagination pagination = new ParamPagination(1, Integer.MAX_VALUE, "tiersId", true);
		final List<RapportsPage.RapportView> views = getRapportViews(tiersId, true, RapportHelper.ALLOWED_PARENTES, pagination, true);
		return new RapportsPage(tiersId, views, true, TypeRapportEntreTiers.PARENTE, null, 1, totalCount, null, null);
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		return getDebiteurViews(tiers);
	}

	/**
	 * Retourne toutes les établissements (principal et secondaire) associés à un contribuable sous format JSON.
	 *
	 * @param tiersId le numéro de tiers
	 * @return une liste de liens vers les établissements sous format JSON
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/etablissements.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage etablissements(@RequestParam("tiers") long tiersId,
	                             @RequestParam(value = "showHisto", required = false, defaultValue = "false") boolean showHisto,
	                             @RequestParam(value = "sortField", required = false) String sortField,
	                             @RequestParam(value = "sortOrder", required = false) String sortOrder,
	                             @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                             @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les établissements d'un tiers");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final int totalCount = getRapportsTotalCount(tiers, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS);

		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		final ParamPagination pagination = new ParamPagination(page, pageSize, ((StringUtils.isBlank(sortField)) ? null : sortField), "ASC".equalsIgnoreCase(sortOrder));

		// Si pas de tri défini, on met les annulés en queue de liste
		final List<RapportsPage.RapportView> views;
		if (StringUtils.isBlank(sortField)) {
			views = getRapportViews(tiersId, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS, pagination, true);
		}
		else {
			views = getRapportViews(tiersId, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS, pagination, false);
		}

		return new RapportsPage(tiersId, views, showHisto, null, null, page, totalCount, sortField, sortOrder);
	}

	private List<RapportsPage.RapportView> getRapportViews(long tiersId, boolean showHisto, Set<RapportEntreTiersKey> types, final ParamPagination pagination, boolean annulesEnDernier) {
		// Récupération de tous les rapports (triés)
		final List<RapportEntreTiers> rapports = rapportEntreTiersDAO.findBySujetAndObjet(tiersId, showHisto, types, pagination, true);

		if (annulesEnDernier) {
			rapports.sort(new Comparator<RapportEntreTiers>() {
				@Override
				public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
					// on garde l'ordre fourni par la base en plaçant les éléments annulés à la fin
					if (o1.isAnnule() != o2.isAnnule()) {
						return o1.isAnnule() ? 1 : -1;
					}
					else {
						// on ne change pas l'ordre donné (car la méthode Collections#sort est dite "stable")
						return 0;
					}
				}
			});
		}

		// On ne peuple dans la vue que les rapports concernés par la page courante
		List<RapportEntreTiers> currentRapports = new ArrayList<>(rapports);
		if (currentRapports.size() > pagination.getSqlMaxResults()) {
			int toIndex = (pagination.getSqlMaxResults()*pagination.getNumeroPage() > rapports.size()) ? rapports.size() : pagination.getSqlMaxResults()*pagination.getNumeroPage();
			currentRapports = rapports.subList(pagination.getSqlFirstResult(), toIndex);
		}

		if (currentRapports.size() > 5) {
			prechargeIndividus(currentRapports);
		}

		final List<RapportsPage.RapportView> views = new ArrayList<>(currentRapports.size());
		for (RapportEntreTiers r : currentRapports) {
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

		final List<DebiteurView> views = new ArrayList<>();

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

	private int getRapportsTotalCount(Tiers tiers, boolean showHisto, Set<RapportEntreTiersKey> types) {
		return rapportEntreTiersDAO.countBySujetAndObjet(tiers.getNumero(), showHisto, types);
	}

	private void prechargeIndividus(List<RapportEntreTiers> rapports) {
		if (cacheWarmer.isServiceWarmable()) {
			final Set<Long> tiersIds = new HashSet<>();
			for (RapportEntreTiers rapport : rapports) {
				tiersIds.add(rapport.getSujetId());
				tiersIds.add(rapport.getObjetId());
			}
			cacheWarmer.warmIndividusPourTiers(tiersIds, null, true, AttributeIndividu.ADRESSES);
		}
	}
}
