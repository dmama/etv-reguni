package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.ui.Model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class TiersSearchHelper {

	private static final Pattern POSITIVE_NUMBER_PATTERN = Pattern.compile("^[0-9]+$");

	private static final String TYPES_RECHERCHE_NOM_NAME = "typesRechercheNom";
	private static final String CATEGORIES_IMPOT_SOURCE_NAME = "categoriesImpotSourceEnum";
	private static final String MODES_IMPOSITION_NAME = "modesImpositionEnum";
	private static final String FORMES_JURIDIQUES_NAME = "formesJuridiquesEnum";
	private static final String CATEGORIES_ENTREPRISES_NAME = "categoriesEntreprisesEnum";

	private ServiceInfrastructureService infraService;
	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	/**
	 * Récupération des critères de recherche depuis la session ou création d'un nouvel ensemble de critère si la session est vide.
	 * @param session la session HTTP
	 * @param sessionAttributeName le nom de l'attribut dans la session
	 * @param numeroParam numéro de tiers
	 * @param nomRaisonParam nom/raison sociale du tiers
	 * @param localitePaysParam localité ou pays du tiers
	 * @param noOfsForParam numéro OFS du for fiscal du tiers
	 * @param dateNaissance date de naissance du tiers
	 * @param numeroAssureSocialParam numéro AVS (11 ou 13) du tiers
	 * @param typeTiers type de tiers recherché
	 * @param seulementForPrincipalActif critère sur le flag "for principal actif" du tiers
	 * @param typeVisualisation type de visualisation souhaitée
	 * @return une structure de critères de recherche
	 */
	public TiersCriteriaView getCriteria(HttpSession session, String sessionAttributeName,
	                                     @Nullable String numeroParam,
	                                     @Nullable String nomRaisonParam,
	                                     @Nullable String localitePaysParam,
	                                     @Nullable String noOfsForParam,
	                                     @Nullable RegDate dateNaissance,
	                                     @Nullable String numeroAssureSocialParam,
	                                     @Nullable TiersCriteria.TypeTiers typeTiers,
	                                     boolean seulementForPrincipalActif,
										 TiersCriteria.TypeVisualisation typeVisualisation) {

		final TiersCriteriaView known = (TiersCriteriaView) session.getAttribute(sessionAttributeName);
		if (known != null) {
			return known;
		}

		final TiersCriteriaView created = new TiersCriteriaView();
		created.setTypeTiers(typeTiers);
		created.setNumeroFormatte(StringUtils.trimToNull(numeroParam));
		created.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		created.setNomRaison(StringUtils.trimToNull(nomRaisonParam));
		created.setLocaliteOuPays(StringUtils.trimToNull(localitePaysParam));
		final String cleanNoOfsFor = StringUtils.trimToNull(noOfsForParam);
		created.setNoOfsFor(cleanNoOfsFor);
		if (cleanNoOfsFor != null && POSITIVE_NUMBER_PATTERN.matcher(cleanNoOfsFor).matches()) {
			final Commune commune = infraService.getCommuneByNumeroOfs(Integer.valueOf(cleanNoOfsFor), null);
			created.setForAll(commune.getNomOfficiel());
		}
		created.setDateNaissanceInscriptionRC(dateNaissance);
		created.setNumeroAVS(numeroAssureSocialParam);
		created.setTypeVisualisation(typeVisualisation);
		created.setForPrincipalActif(seulementForPrincipalActif);
		return created;
	}

	/**
	 * Effectue la recherche de tiers selon les critères donnés
	 * @param criteria critères de recherche
	 * @return la liste des données correspondant aux critères (peut être vide, mais pas <code>null</code>)
	 * @throws ch.vd.uniregctb.indexer.TooManyResultsIndexerException si le nombre de résultats trouvés est trop grand
	 * @throws ch.vd.uniregctb.indexer.EmptySearchCriteriaException si les critères donnés se résument à "aucun critère"
	 * @throws IndexerException en cas de problème
	 */
	public List<TiersIndexedDataView> search(TiersCriteriaView criteria) throws IndexerException {
		if (StringUtils.isNotBlank(criteria.getNumeroAVS())){
			criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(criteria.getNumeroAVS()));
		}

		final List<TiersIndexedData> results = tiersService.search(criteria.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}
		return list;
	}

	/**
	 * Remplissage du modèle avec les données nécessaire à l'affichage du formulaire de recherche des tiers (listes énumérées)
	 * @param model le modèle à compléter
	 * @param criteriaAttributeName nom de l'attribut auquel associé l'objet des critères dans le modèle
	 * @param criteria les critères de recherche pour le pré-remplissage des champs
	 */
	public void fillModelValuesForCriteria(Model model, String criteriaAttributeName, TiersCriteriaView criteria) {
		model.addAttribute(TYPES_RECHERCHE_NOM_NAME, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(CATEGORIES_IMPOT_SOURCE_NAME, tiersMapHelper.getMapCategorieImpotSource());
		model.addAttribute(MODES_IMPOSITION_NAME, tiersMapHelper.getMapModeImposition());
		model.addAttribute(FORMES_JURIDIQUES_NAME, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute(CATEGORIES_ENTREPRISES_NAME, tiersMapHelper.getMapCategoriesEntreprise());
		model.addAttribute(criteriaAttributeName, criteria);
	}
}
