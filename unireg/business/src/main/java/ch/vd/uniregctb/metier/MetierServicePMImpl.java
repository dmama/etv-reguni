package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MetierServicePMImpl implements MetierServicePM {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetierServicePMImpl.class);

	private TiersService tiersService;
	private AdresseService adresseService;
	private RemarqueDAO remarqueDAO;
	private EvenementFiscalService evenementFiscalService;
	private TacheService tacheService;

	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	/**
	 * Méthode qui parcoure les établissements de l'entreprise et qui ajuste les fors secondaires en prenant soin d'éviter
	 * les chevauchements. Elle crée les fors nécessaires, ferme ceux qui se terminent et annule ceux qui sont devenus redondants.
	 *
	 * La méthode gère les fors secondaires uniquement sur VD
	 *
	 * On peut passer une date de coupure qui "tranche" le début d'historique des fors secondaire à créer. Sert à faire démarrer le for secondaire d'une nouvelle entreprise à j + 1 comme
	 * le for principal. Laisser vide dans les cas non création.
	 *
	 * @param entreprise l'entreprise concernée
	 * @param dateAuPlusTot la date de coupure pour la création d'entreprise.
	 */
	@Override
	public AjustementForsSecondairesResult calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws MetierServiceException {

		List<DateRanged<Etablissement>> etablissements = tiersService.getEtablissementsSecondairesEntreprise(entreprise);

		// Les domiciles VD classés par commune
		final Map<Integer, List<DomicileHisto>> tousLesDomicilesVD = new HashMap<>();
		for (DateRanged<Etablissement> etablissement : etablissements) {
			final List<DomicileHisto> domiciles = tiersService.getDomiciles(etablissement.getPayload(), false);
			if (domiciles != null && !domiciles.isEmpty()) {
				for (DomicileHisto domicile : domiciles) {
					if (domicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						continue; // On ne crée des fors secondaires que pour VD
					}
					List<DomicileHisto> histoPourCommune = tousLesDomicilesVD.get(domicile.getNumeroOfsAutoriteFiscale());
					if (histoPourCommune == null) {
						histoPourCommune = new ArrayList<>();
						tousLesDomicilesVD.put(domicile.getNumeroOfsAutoriteFiscale(), histoPourCommune);
					}
					histoPourCommune.add(domicile);
				}
			}
		}

		// Charger les historiques de fors secondaire établissement stables existant pour chaque commune
		final Map<Integer, List<ForFiscalSecondaire>> tousLesForsFiscauxSecondairesParCommune =
				entreprise.getForsFiscauxSecondairesActifsSortedMapped(MotifRattachement.ETABLISSEMENT_STABLE);

		return AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, dateAuPlusTot);
	}

	@Override
	public RattachementOrganisationResult rattacheOrganisationEntreprise(Organisation organisation, Entreprise entreprise, RegDate date) throws MetierServiceException {

		if (organisation.getSitePrincipal(date) == null) {
			throw new MetierServiceException(String.format("L'organisation %d n'a pas de sitePrincipal principal à la date demandée %s.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}

		if (entreprise.getNumeroEntreprise() != null && entreprise.getNumeroEntreprise() != organisation.getNumeroOrganisation()) {
			throw new MetierServiceException(String.format("L'entreprise %d est déjà rattachée à une autre organisation!", entreprise.getNumero()));
		}

		// Rapprochement de l'entreprise
		entreprise.setNumeroEntreprise(organisation.getNumeroOrganisation());

		final RaisonSocialeFiscaleEntreprise raisonSociale = getAssertLast(entreprise.getRaisonsSocialesNonAnnuleesTriees(), date);
		if (raisonSociale != null && raisonSociale.getDateFin() == null) {
			tiersService.closeRaisonSocialeFiscale(raisonSociale, date.getOneDayBefore());
		}

		final FormeJuridiqueFiscaleEntreprise formeJuridique = getAssertLast(entreprise.getFormesJuridiquesNonAnnuleesTriees(), date);
		if (formeJuridique != null && formeJuridique.getDateFin() == null) {
			tiersService.closeFormeJuridiqueFiscale(formeJuridique, date.getOneDayBefore());
		}

		final CapitalFiscalEntreprise capital = getAssertLast(entreprise.getCapitauxNonAnnulesTries(), date);
		if (capital != null && capital.getDateFin() == null) {
			tiersService.closeCapitalFiscal(capital, date.getOneDayBefore());
		}

		RattachementOrganisationResult result = new RattachementOrganisationResult(entreprise);

		// Rapprochement de l'établissement principal
		SiteOrganisation sitePrincipal = organisation.getSitePrincipal(date).getPayload();
		final List<DateRanged<Etablissement>> etablissementsPrincipauxEntreprise = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
		if (etablissementsPrincipauxEntreprise.isEmpty() || CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise) == null) {
			throw new MetierServiceException(String.format("L'entreprise %s ne possède pas d'établissement principal!", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
		}
		DateRanged<Etablissement> etablissementPrincipalRange = CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise);
		if (etablissementPrincipalRange.getDateDebut().isAfter(date)) {
			throw new MetierServiceException(String.format("L'établissement principal %d commence à une date postérieure à la tentative de rapprochement du %s. Impossible de continuer.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}

		final Etablissement etablissementPrincipal = etablissementPrincipalRange.getPayload();

		final List<DomicileEtablissement> sortedDomiciles = etablissementPrincipal.getSortedDomiciles(false);
		if (sortedDomiciles.isEmpty()) {
			throw new MetierServiceException(String.format("L'établissement principal %d n'a pas de domicile en date du %s. Impossible de continuer le rapprochement.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}
		final DomicileEtablissement domicile = getAssertLast(sortedDomiciles, date);

		if (domicile != null && domicile.getNumeroOfsAutoriteFiscale().equals(organisation.getSiegePrincipal(date).getNoOfs())) {
			etablissementPrincipal.setNumeroEtablissement(sitePrincipal.getNumeroSite());
			tiersService.closeDomicileEtablissement(domicile, date.getOneDayBefore());
			result.addEtablissementRattache(etablissementPrincipal);
		} else {
			throw new MetierServiceException(String.format("L'établissement principal %s n'a pas de domicile ou celui-ci ne correspond pas avec celui que rapporte le régistre civil.", FormatNumeroHelper.numeroCTBToDisplay(etablissementPrincipal.getNumero())));
		}

		// Eliminer les établissements secondaires déjà rattachés des listes.
		Map<Long, SiteOrganisation> sitestoMatch = new HashMap<>();
		final List<Etablissement> etablissementsNonEncoreRattaches = new ArrayList<>();

		for (SiteOrganisation siteSecondaire : organisation.getSitesSecondaires(date)) {
			sitestoMatch.put(siteSecondaire.getNumeroSite(), siteSecondaire);
		}

		for (Etablissement etablissementSecondaire : tiersService.getEtablissementsSecondairesEntrepriseSansRange(entreprise)) {
			if (etablissementSecondaire.isConnuAuCivil()) {
				final Long numeroEtablissement = etablissementSecondaire.getNumeroEtablissement();
				if (sitestoMatch.get(numeroEtablissement) != null && sitestoMatch.remove(numeroEtablissement) != null) {
					result.addEtablissementRattache(etablissementSecondaire);
				}
				else {
					throw new MetierServiceException(String.format("L'établissement secondaire %s est censé être connu au civil mais son numéro ne correspond à aucun des établissements civil RCEnt de l'organisation %d!",
					                                               FormatNumeroHelper.numeroCTBToDisplay(numeroEtablissement), entreprise.getNumeroEntreprise()));
				}
			} else {
				etablissementsNonEncoreRattaches.add(etablissementSecondaire);
			}
		}

		/* Rapprochement des établissements secondaires à proprement parler */

		/* Pour les établissements et les sites, on construit des paires domicile-statut */
		Map<DomicileStatutKey, List<SiteOrganisation>> keyedSites = buildKeyedSites(date, sitestoMatch);
		Map<DomicileStatutKey, List<Etablissement>> keyedEtablissements = buildKeyedEtablissements(date, etablissementsNonEncoreRattaches);

		/* TODO: Corréler agressivement par no IDE si disponible? */

		/* On parcoure les listes pour établir les correspondances en ignorant les "à double", car impossible de les différencier
		   sans prendre de gros risques */
		for (Map.Entry<DomicileStatutKey, List<Etablissement>> etabEntry : keyedEtablissements.entrySet()) {
			// Entrée non unique
			if (etabEntry.getValue().size() != 1) {
				continue;
			}
			// Entrée nulle ou non unique
			List<SiteOrganisation> sitesForKey = keyedSites.get(etabEntry.getKey());
			if (sitesForKey == null || sitesForKey.size() != 1) {
				continue;
			}

			// On peut entrer en matière
			Etablissement etablissementForKey = etabEntry.getValue().get(0);
			SiteOrganisation siteForKey = sitesForKey.get(0);

			// Si on a un numéro IDE dans l'établissement et qu'il ne correspond pas à notre site candidat, c'est qu'il y a un soucis.
			final Set<IdentificationEntreprise> identificationsEntreprise = etablissementForKey.getIdentificationsEntreprise();
			if (identificationsEntreprise != null && !identificationsEntreprise.isEmpty()) {
				String noIdeEtablissement = identificationsEntreprise.iterator().next().getNumeroIde();
				final DateRanged<String> noIdeSiteRange = getAssertLast(siteForKey.getNumeroIDE(), date);
				if (noIdeEtablissement != null && noIdeSiteRange != null && !noIdeEtablissement.equals(noIdeSiteRange.getPayload())) {
					continue;
				}
			}

			// On a suffisament de certitudes pour rattacher l'établissement au site
			etablissementForKey.setNumeroEtablissement(siteForKey.getNumeroSite());

			// Il faut ventiler les participant dans les bonnes listes
			etablissementsNonEncoreRattaches.remove(etablissementForKey);
			result.addEtablissementRattache(etablissementForKey);
			sitestoMatch.remove(siteForKey.getNumeroSite());

			// Comme pour l'entreprise, il faut terminer les données civiles Unireg

			final DomicileEtablissement domicileEtablissement = extractDernierDomicileFiscal(etablissementForKey, date);
			if (domicileEtablissement != null && domicileEtablissement.getDateFin() == null) {
				tiersService.closeDomicileEtablissement(domicileEtablissement, date.getOneDayBefore());
			}
		}

		/* Finalisation du résultat */

		/* Ajout des établissements qu'on n'a vraiment pas pu rattacher. */
		for (Etablissement etabNonRattache : etablissementsNonEncoreRattaches) {
			result.addEtablissementNonRattache(etabNonRattache);
		}

		for (Map.Entry<Long, SiteOrganisation> entry : sitestoMatch.entrySet()) {
			result.addSiteNonRattache(entry.getValue());
		}

		return result;
	}

	private Map<DomicileStatutKey, List<Etablissement>> buildKeyedEtablissements(RegDate date, List<Etablissement> etablissements) throws MetierServiceException {
		Map<DomicileStatutKey, List<Etablissement>> map = new HashMap<>();
		for (Etablissement etablissement : etablissements) {
			DomicileStatutKey key = createEtablissementKey(date, etablissement);
			if (key == null) {
				continue;
			}
			if (map.get(key) == null) {
				map.put(key, new ArrayList<Etablissement>());
			}
			map.get(key).add(etablissement);
		}
		return map;
	}

	private Map<DomicileStatutKey, List<SiteOrganisation>> buildKeyedSites(RegDate date, Map<Long, SiteOrganisation> sites) {
		Map<DomicileStatutKey, List<SiteOrganisation>> map = new HashMap<>();
		for (SiteOrganisation site : sites.values()) {
			DomicileStatutKey key = createSiteKey(date, site);
			if (key == null) continue;
			if (map.get(key) == null) {
				map.put(key, new ArrayList<SiteOrganisation>());
			}
			map.get(key).add(site);
		}
		return map;
	}

	@Nullable
	private DomicileStatutKey createSiteKey(RegDate date, SiteOrganisation site) {
		final Domicile domicile = site.getDomicile(date);
		return domicile == null ? null : new DomicileStatutKey(domicile.getNoOfs(), domicile.getTypeAutoriteFiscale(), site.isActif(date));
	}

	private DomicileStatutKey createEtablissementKey(RegDate date, Etablissement etablissement) throws MetierServiceException {
		/* Déterminer le dernier domicile */
		final DomicileEtablissement domicileEtablissement = extractDernierDomicileFiscal(etablissement, date);
		if (domicileEtablissement == null) {
			return null;
		}

		/* Déterminer le active */
		final RapportEntreTiers rapport = etablissement.getRapportObjetValidAt(date, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
		boolean active = rapport.isValidAt(date);

		return new DomicileStatutKey(domicileEtablissement.getNumeroOfsAutoriteFiscale(), domicileEtablissement.getTypeAutoriteFiscale(), active);
	}

	/**
	 * Classe implémentant une clé représentant le couple domicile civil - activite de l'établissement
	 */
	private static class DomicileStatutKey {
		private int noOfsAutirteFiscale;
		private TypeAutoriteFiscale typeAutoriteFiscale;
		private boolean active;

		public DomicileStatutKey(int noOfsAutirteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, boolean active) {
			this.noOfsAutirteFiscale = noOfsAutirteFiscale;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
			this.active = active;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final DomicileStatutKey that = (DomicileStatutKey) o;

			if (getNoOfsAutirteFiscale() != that.getNoOfsAutirteFiscale()) return false;
			if (isActive() != that.isActive()) return false;
			return getTypeAutoriteFiscale() == that.getTypeAutoriteFiscale();

		}

		@Override
		public int hashCode() {
			int result = getNoOfsAutirteFiscale();
			result = 31 * result + getTypeAutoriteFiscale().hashCode();
			result = 31 * result + (isActive() ? 1 : 0);
			return result;
		}

		public int getNoOfsAutirteFiscale() {
			return noOfsAutirteFiscale;
		}

		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return typeAutoriteFiscale;
		}

		public boolean isActive() {
			return active;
		}
	}

	private static DomicileEtablissement extractDernierDomicileFiscal(Etablissement etablissement, RegDate date) throws MetierServiceException {
		final ArrayList<DomicileEtablissement> domicileEtablissements = new ArrayList<>();
		domicileEtablissements.addAll(etablissement.getDomiciles());
		return getAssertLast(domicileEtablissements, date);
	}

	/**
	 * Obtenir le dernier range, en s'assurant que la date fournie en fait partie. Cela permet de garantir qu'on
	 * ne travail pas dans le passé lorsqu'on traite, par exemple, un événement organisation. Si une ou des périodes existes
	 * ultérieurement à la date fournie, cela signifie que la situation a évolué depuis la date et qu'on ne peut plus opérer
	 * sur la base de celle-ci.
	 */
	private static <T extends DateRange> T getAssertLast(List<T> entites, RegDate date) throws MetierServiceException {
		if (entites != null && !entites.isEmpty()) {
			T lastRange = CollectionsUtils.getLastElement(entites);
			if (lastRange == null) {
				throw new MetierServiceException(String.format("Erreur de données: null trouvé dans une collection de périodes %s", entites.getClass()));
			}
			if (lastRange.getDateDebut() == null) {
				throw new MetierServiceException("Erreur de données: la date de début de la dernière période est nulle");
			}
			if (lastRange.getDateDebut().isAfter(date)) {
				throw new MetierServiceException(String.format("La période valide à la date demandée %s n'est pas la dernière de l'historique!", RegDateHelper.dateToDisplayString(date)));
			}
			return lastRange;
		}
		return null;
	}


	@Override
	public void faillite(Entreprise entreprise, RegDate datePrononceFaillite, String remarqueAssociee) throws MetierServiceException {

		// pas de for principal ouvert -> pas possible d'aller plus loin
		final ForsParTypeAt forsParType = entreprise.getForsParTypeAt(null, false);
		if (forsParType.principal == null) {
			throw new MetierServiceException("Tous les fors fiscaux de l'entreprise sont déjà fermés.");
		}

		// 1. on ferme tous les rapports entre tiers ouvert (mandats et établissements secondaires)

		for (RapportEntreTiers ret : CollectionsUtils.merged(entreprise.getRapportsSujet(), entreprise.getRapportsObjet())) {
			if (!ret.isAnnule() && ret.getDateFin() == null) {
				final boolean aFermer = (ret instanceof ActiviteEconomique && !((ActiviteEconomique) ret).isPrincipal()) || (ret instanceof Mandat);
				if (aFermer) {
					ret.setDateFin(datePrononceFaillite);
				}
			}
		}

		// 1'. [SIFISC-18810] on ferme également les adresses mandataires...
		for (AdresseMandataire adresse : entreprise.getAdressesMandataires()) {
			if (!adresse.isAnnule() && adresse.getDateFin() == null) {
				adresse.setDateFin(datePrononceFaillite);
			}
		}

		// 2. on ferme les fors ouverts avec le motif FAILLITE

		// on traite d'abord les fors secondaires, puis seulement ensuite le for principal
		boolean hasImmeuble = false;
		for (ForFiscalSecondaire fs : forsParType.secondaires) {
			hasImmeuble |= fs.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE;
			tiersService.closeForFiscalSecondaire(entreprise, fs, datePrononceFaillite, MotifFor.FAILLITE);
		}
		tiersService.closeForFiscalPrincipal(forsParType.principal, datePrononceFaillite, MotifFor.FAILLITE);

		// 3. si for immeuble fermé, on crée une tâche de contrôle de dossier

		if (hasImmeuble) {
			tacheService.genereTacheControleDossier(entreprise);
		}

		// 4. nouvel état fiscal

		entreprise.addEtat(new EtatEntreprise(datePrononceFaillite, TypeEtatEntreprise.EN_FAILLITE, TypeGenerationEtatEntreprise.MANUELLE));

		// 5. publication de l'événement fiscal de faillite et appel aux créanciers

		evenementFiscalService.publierEvenementFiscalInformationComplementaire(entreprise,
		                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS,
		                                                                       datePrononceFaillite);

		// 6. éventuellement ajout d'une remarque sur l'entreprise

		addRemarque(entreprise, remarqueAssociee);
	}

	@Override
	public void annuleFaillite(Entreprise entreprise, RegDate datePrononceFaillite, @Nullable String remarqueAssociee) throws MetierServiceException {

		// 1. état fiscal courant différent de "EN_FAILLITE" -> on saute !

		final EtatEntreprise etat = entreprise.getEtatActuel();
		if (etat == null || etat.getType() != TypeEtatEntreprise.EN_FAILLITE) {
			throw new MetierServiceException("L'entreprise n'est pas/plus en faillite.");
		}
		else if (etat.getDateObtention() != datePrononceFaillite) {
			throw new MetierServiceException("La date de prononcé de faillite fournie ne correspond pas à la date actuellement connue.");
		}

		// 2. annulation de l'état en question

		etat.setAnnule(true);

		// 3. ré-ouverture de tous les fors fiscaux fermés à la date du prononcé de faillite

		tiersService.reopenForsClosedAt(datePrononceFaillite, MotifFor.FAILLITE, entreprise);

		// 4. ré-ouverture des rapports entre tiers fermés à la date du prononcé de faillite

		tiersService.reopenRapportsEntreTiers(entreprise,
		                                      datePrononceFaillite,
		                                      EnumSet.of(TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE),
		                                      EnumSet.of(TypeRapportEntreTiers.MANDAT));

		// 4'. [SIFISC-18810] ré-ouverture des adresses mandataires fermées à la date du prononcé de faillite
		for (AdresseMandataire adresse : entreprise.getAdressesMandataires()) {
			if (!adresse.isAnnule() && adresse.getDateFin() == datePrononceFaillite) {
				final AdresseMandataire copie = adresse.duplicate();
				adresse.setAnnule(true);
				copie.setDateFin(null);
				entreprise.addAdresseMandataire(copie);
			}
		}

		// 5. éventuellement, ajoute une nouvelle remarque sur le tiers

		addRemarque(entreprise, remarqueAssociee);
	}

	@Override
	public void revoqueFaillite(Entreprise entreprise, RegDate datePrononceFaillite, @Nullable String remarqueAssociee) throws MetierServiceException {

		// 1. état fiscal courant différent de "EN_FAILLITE" -> on saute !

		final EtatEntreprise etat = entreprise.getEtatActuel();
		if (etat == null || etat.getType() != TypeEtatEntreprise.EN_FAILLITE) {
			throw new MetierServiceException("L'entreprise n'est pas/plus en faillite.");
		}
		else if (etat.getDateObtention() != datePrononceFaillite) {
			throw new MetierServiceException("La date de prononcé de faillite fournie ne correspond pas à la date actuellement connue.");
		}

		// 2. annulation de l'état en question

		etat.setAnnule(true);

		// 3. ré-ouverture du for fiscal principal fermé à la date du prononcé de faillite

		final ForFiscalPrincipal ffp = entreprise.getDernierForFiscalPrincipal();
		if (ffp == null) {
			throw new MetierServiceException("L'entreprise n'a aucun for principal non-annulé.");
		}
		else if (ffp.getDateFin() != datePrononceFaillite || ffp.getMotifFermeture() != MotifFor.FAILLITE) {
			throw new MetierServiceException("Le dernier for principal de l'entreprise n'est soit pas fermé à la date du prononcé de faillite, soit fermé avec un motif de fermeture incompatible avec une faillite.");
		}
		ffp.setAnnule(true);
		tiersService.reopenFor(ffp, entreprise);

		// 4. envoi d'un événement fiscal de révocation de faillite (+ annulation du for avant ré-ouverture)

		evenementFiscalService.publierEvenementFiscalAnnulationFor(ffp);
		evenementFiscalService.publierEvenementFiscalInformationComplementaire(entreprise,
		                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.REVOCATION_FAILLITE,
		                                                                       RegDate.get());

		// 5. éventuellement, ajoute une nouvelle remarque sur le tiers

		addRemarque(entreprise, remarqueAssociee);
	}

	@Override
	public void demenageSiege(Entreprise entreprise, RegDate dateDebutNouveauSiege, TypeAutoriteFiscale taf, int noOfs) throws MetierServiceException {

		// pas de for principal ouvert -> pas possible d'aller plus loin
		final ForsParTypeAt forsParType = entreprise.getForsParTypeAt(null, false);
		if (forsParType.principal == null) {
			throw new MetierServiceException("Tous les fors fiscaux de l'entreprise sont déjà fermés.");
		}

		// 1. on s'occupe de toute façon du for principal de l'entreprise

		final ForFiscalPrincipal forPrincipal = forsParType.principal;
		final TypeAutoriteFiscale tafCourant = forPrincipal.getTypeAutoriteFiscale();

		// le motif de changement
		final MotifFor motif;
		if (tafCourant == taf) {
			motif = MotifFor.DEMENAGEMENT_VD;
		}
		else if (taf == TypeAutoriteFiscale.PAYS_HS) {
			motif = MotifFor.DEPART_HS;
		}
		else if (tafCourant == TypeAutoriteFiscale.PAYS_HS) {
			motif = MotifFor.ARRIVEE_HS;
		}
		else if (taf == TypeAutoriteFiscale.COMMUNE_HC) {
			motif = MotifFor.DEPART_HC;
		}
		else {
			motif = MotifFor.ARRIVEE_HC;
		}
		tiersService.addForPrincipal(entreprise, dateDebutNouveauSiege, motif, null, null, forPrincipal.getMotifRattachement(), noOfs, taf, forPrincipal.getGenreImpot());

		// 2. si l'entreprise n'est pas liée au civil, il faut faire la même chose sur le domicile de l'établissement principal

		if (!entreprise.isConnueAuCivil()) {

			// récupération de l'établissement principal courant
			final List<DateRanged<Etablissement>> etbsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
			if (etbsPrincipaux != null && !etbsPrincipaux.isEmpty()) {
				final DateRanged<Etablissement> etbPrincipalCourant = CollectionsUtils.getLastElement(etbsPrincipaux);
				if (etbPrincipalCourant == null) {
					// pas d'établissement principal ?? bizarre, non ?
					throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
				}
				else if (etbPrincipalCourant.getDateFin() != null) {
					throw new MetierServiceException("Le lien vers l'établissement principal est fermé.");
				}

				// il faut trouver le dernier domicile de cet établissement
				final Etablissement etbPrincipal = etbPrincipalCourant.getPayload();
				final List<DomicileEtablissement> domiciles = etbPrincipal.getSortedDomiciles(false);
				if (domiciles.isEmpty()) {
					throw new MetierServiceException(String.format("Aucun domicile connu sur l'établissement principal de l'entreprise (%s)", FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}

				final DomicileEtablissement dernierDomicile = CollectionsUtils.getLastElement(domiciles);
				if (dernierDomicile.getDateFin() != null) {
					throw new MetierServiceException(String.format("Le dernier domicile connu sur l'établissement principal (%s) de l'entreprise est déjà fermé.",
					                                               FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}

				tiersService.closeDomicileEtablissement(dernierDomicile, dateDebutNouveauSiege.getOneDayBefore());
				tiersService.addDomicileEtablissement(etbPrincipal, taf, noOfs, dateDebutNouveauSiege, null);
			}
			else {
				// pas d'établissement principal ?? bizarre, non ?
				throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
			}
		}
	}

	@Override
	public void annuleDemenagement(Entreprise entreprise, RegDate dateDernierDemenagement) throws MetierServiceException {

		// le dernier for fiscal principal de l'entreprise doit être encore ouvert et démarrer à la date donnée
		final ForFiscalPrincipalPM dernierFor = entreprise.getDernierForFiscalPrincipal();
		if (dernierFor == null || dernierFor.getDateFin() != null || dernierFor.getDateDebut() != dateDernierDemenagement) {
			throw new MetierServiceException("Entreprise sans for principal actif, ou dont le for principal actif ne commence pas à la date annoncée.");
		}
		else if (entreprise.getForFiscalPrincipalAt(dateDernierDemenagement.getOneDayBefore()) == null) {
			throw new MetierServiceException("Entreprise sans for principal immédiatement précédant le for actif : nous ne sommes pas en présence d'un déménagement.");
		}

		// 1. on ré-ouvre le for principal précédent
		tiersService.annuleForFiscal(dernierFor);

		// 2. si l'entreprise n'est pas liée au civil, il faut faire la même chose sur le domicile de l'établissement principal
		if (!entreprise.isConnueAuCivil()) {

			// récupération de l'établissement principal courant
			final List<DateRanged<Etablissement>> etbsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
			if (etbsPrincipaux != null && !etbsPrincipaux.isEmpty()) {
				final DateRanged<Etablissement> etbPrincipalCourant = CollectionsUtils.getLastElement(etbsPrincipaux);
				if (etbPrincipalCourant == null) {
					// pas d'établissement principal ?? bizarre, non ?
					throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
				}
				else if (etbPrincipalCourant.getDateFin() != null) {
					throw new MetierServiceException("Le lien vers l'établissement principal est fermé.");
				}

				// il faut trouver le dernier domicile de cet établissement
				final Etablissement etbPrincipal = etbPrincipalCourant.getPayload();
				final List<DomicileEtablissement> domiciles = etbPrincipal.getSortedDomiciles(false);
				if (domiciles.isEmpty()) {
					throw new MetierServiceException(String.format("Aucun domicile connu sur l'établissement principal de l'entreprise (%s)", FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}

				final DomicileEtablissement dernierDomicile = CollectionsUtils.getLastElement(domiciles);
				if (dernierDomicile.getDateFin() != null) {
					throw new MetierServiceException(String.format("Le dernier domicile connu sur l'établissement principal (%s) de l'entreprise est déjà fermé.",
					                                               FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}
				else if (dernierDomicile.getDateDebut() != dateDernierDemenagement) {
					throw new MetierServiceException(String.format("Le dernier domicile connu sur l'établissement principal (%s) de l'entreprise ne débute pas à la date annoncée.",
					                                               FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}
				else {
					final DomicileEtablissement domicilePrecedent = DateRangeHelper.rangeAt(domiciles, dateDernierDemenagement.getOneDayBefore());
					if (domicilePrecedent == null) {
						throw new MetierServiceException(String.format("Aucun domicile connu sur l'établissement principal (%s) de l'entreprise juste avant le %s.",
						                                               FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
						                                               RegDateHelper.dateToDisplayString(dateDernierDemenagement)));
					}

					// copie + annulations
					dernierDomicile.setAnnule(true);
					domicilePrecedent.setAnnule(true);
					tiersService.addDomicileEtablissement(etbPrincipal, domicilePrecedent.getTypeAutoriteFiscale(), domicilePrecedent.getNumeroOfsAutoriteFiscale(), domicilePrecedent.getDateDebut(), null);
				}
			}
			else {
				// pas d'établissement principal ?? bizarre, non ?
				throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
			}
		}
	}

	@Override
	public void finActivite(Entreprise entreprise, RegDate dateFinActivite, String remarqueAssociee) throws MetierServiceException {

		// pas de for principal ouvert -> pas possible d'aller plus loin
		final ForsParTypeAt forsParType = entreprise.getForsParTypeAt(null, false);
		if (forsParType.principal == null) {
			throw new MetierServiceException("Tous les fors fiscaux de l'entreprise sont déjà fermés.");
		}

		// 1. on ferme tous les rapports entre tiers ouvert (mandats et établissements secondaires)

		for (RapportEntreTiers ret : CollectionsUtils.merged(entreprise.getRapportsSujet(), entreprise.getRapportsObjet())) {
			if (!ret.isAnnule() && ret.getDateFin() == null) {
				final boolean aFermer = (ret instanceof ActiviteEconomique && !((ActiviteEconomique) ret).isPrincipal()) || (ret instanceof Mandat);
				if (aFermer) {
					ret.setDateFin(dateFinActivite);
				}
			}
		}

		// 1'. [SIFISC-18810] on ferme également les adresses mandataires...
		for (AdresseMandataire adresse : entreprise.getAdressesMandataires()) {
			if (!adresse.isAnnule() && adresse.getDateFin() == null) {
				adresse.setDateFin(dateFinActivite);
			}
		}

		// 2. on ferme les fors ouverts avec le motif CESSATION_ACTIVITE

		// on traite d'abord les fors secondaires, puis seulement ensuite le for principal
		boolean hasImmeuble = false;
		for (ForFiscalSecondaire fs : forsParType.secondaires) {
			hasImmeuble |= fs.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE;
			tiersService.closeForFiscalSecondaire(entreprise, fs, dateFinActivite, MotifFor.CESSATION_ACTIVITE);
		}
		tiersService.closeForFiscalPrincipal(forsParType.principal, dateFinActivite, MotifFor.CESSATION_ACTIVITE);

		// 3. si for immeuble fermé, on crée une tâche de contrôle de dossier

		if (hasImmeuble) {
			tacheService.genereTacheControleDossier(entreprise);
		}

		// 4. nouvel état fiscal (seulement si l'entreprise n'est pas inscrite au RC)
		if (!tiersService.isInscriteRC(entreprise, dateFinActivite)) {
			entreprise.addEtat(new EtatEntreprise(dateFinActivite, TypeEtatEntreprise.DISSOUTE, TypeGenerationEtatEntreprise.MANUELLE));
		}

		// 6. éventuellement ajout d'une remarque sur l'entreprise

		addRemarque(entreprise, remarqueAssociee);
	}

	private void addRemarque(Tiers tiers, String texteRemarque) {
		if (StringUtils.isNotBlank(texteRemarque)) {
			final Remarque remarque = new Remarque();
			remarque.setTexte(StringUtils.abbreviate(texteRemarque, LengthConstants.TIERS_REMARQUE));
			remarque.setTiers(tiers);
			remarqueDAO.save(remarque);
		}
	}

	@Override
	public void fusionne(Entreprise absorbante, List<Entreprise> absorbees, RegDate dateContratFusion, RegDate dateBilanFusion) throws MetierServiceException {

		// création de rapports entre tiers de type fusion
		for (Entreprise absorbee : absorbees) {
			final FusionEntreprises fusion = new FusionEntreprises(dateBilanFusion, null, absorbee, absorbante);
			tiersService.addRapport(fusion, absorbee, absorbante);
		}

		// envoi d'un événement fiscal sur toutes les entreprises concernées
		for (Entreprise entreprise : CollectionsUtils.merged(Collections.singletonList(absorbante), absorbees)) {
			evenementFiscalService.publierEvenementFiscalInformationComplementaire(entreprise,
			                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.FUSION,
			                                                                       dateBilanFusion);
		}

		// récupération de l'adresse de la société absorbante pour répercussion sur les absorbées
		final AdresseEnvoiDetaillee adresseAbsorbante;
		try {
			adresseAbsorbante = adresseService.getAdresseEnvoi(absorbante, null, TypeAdresseFiscale.COURRIER, false);
		}
		catch (AdresseException e) {
			LOGGER.error(e.getMessage(), e);
			throw new MetierServiceException(e);
		}
		if (adresseAbsorbante == null) {
			throw new MetierServiceException("L'entreprise absorbante n'a pas d'adresse 'courrier' valide.");
		}

		// fermeture de tous les fors fiscaux des entreprises absorbées, surcharges d'adresse
		for (Entreprise absorbee : absorbees) {

			// on traite d'abord les fors secondaires, puis seulement ensuite le for principal
			final ForsParTypeAt forsParType = absorbee.getForsParTypeAt(null, false);
			boolean hasImmeuble = false;
			for (ForFiscalSecondaire fs : forsParType.secondaires) {
				hasImmeuble |= fs.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE;
				tiersService.closeForFiscalSecondaire(absorbee, fs, dateBilanFusion, MotifFor.FUSION_ENTREPRISES);
			}
			tiersService.closeForFiscalPrincipal(forsParType.principal, dateBilanFusion, MotifFor.FUSION_ENTREPRISES);

			// tâche de contrôle de dossier si propriétaire d'immeuble
			if (hasImmeuble) {
				tacheService.genereTacheControleDossier(absorbee);
			}

			// surcharge d'adresse
			final AdresseTiers surchargeAdresse = surchargeAdresseSuiteAFusion(adresseAbsorbante, dateContratFusion);
			adresseService.addAdresse(absorbee, surchargeAdresse);

			// ajout de l'état absorbé sur l'entreprise
			absorbee.addEtat(new EtatEntreprise(dateContratFusion, TypeEtatEntreprise.ABSORBEE, TypeGenerationEtatEntreprise.MANUELLE));
		}
	}

	/**
	 * @param adresse adresse de l'entreprise absorbante
	 * @param dateDebutValidite date de début de validité de la surcharge souhaitée
	 * @return une adresse courrier surchargée à placer sur l'entreprise absorbée
	 */
	private AdresseTiers surchargeAdresseSuiteAFusion(AdresseEnvoiDetaillee adresse, RegDate dateDebutValidite) {
		final AdresseSupplementaire adresseTiers;
		if (adresse.isSuisse()) {
			final AdresseSuisse adresseSuisse = new AdresseSuisse();
			adresseSuisse.setNumeroOrdrePoste(adresse.getNumeroOrdrePostal());
			if (adresse.getNumeroTechniqueRue() != null) {
				adresseSuisse.setNumeroRue(adresse.getNumeroTechniqueRue());
			}
			else {
				adresseSuisse.setRue(adresse.getRueEtNumero().getRue());
			}
			final CasePostale casePostale = adresse.getCasePostale();
			if (casePostale != null) {
				adresseSuisse.setNpaCasePostale(casePostale.getNpa());
			}

			adresseTiers = adresseSuisse;
		}
		else {
			final AdresseEtrangere adresseEtrangere = new AdresseEtrangere();
			adresseEtrangere.setNumeroOfsPays(adresse.getPays().getNoOFS());
			adresseEtrangere.setNumeroPostalLocalite(adresse.getNpaEtLocalite().toString());
			adresseTiers = adresseEtrangere;
		}

		final CasePostale casePostale = adresse.getCasePostale();
		if (casePostale != null) {
			adresseTiers.setTexteCasePostale(casePostale.getType());
			adresseTiers.setNumeroCasePostale(casePostale.getNumero());
		}

		adresseTiers.setDateDebut(dateDebutValidite);
		adresseTiers.setUsage(TypeAdresseTiers.COURRIER);
		adresseTiers.setComplement("p.a. " + CollectionsUtils.concat(adresse.getNomsPrenomsOuRaisonsSociales(), ", "));
		adresseTiers.setNumeroAppartement(adresse.getNumeroAppartement());
		adresseTiers.setNumeroMaison(adresse.getRueEtNumero().getNumero());
		adresseTiers.setPermanente(true);
		return adresseTiers;
	}

	@Override
	public void annuleFusionEntreprises(Entreprise absorbante, List<Entreprise> absorbees, RegDate dateContratFusion, RegDate dateBilanFusion) throws MetierServiceException {

		// vérification que toutes les entreprises absorbées ont bien connu une absorption par l'entreprise absorbante
		// avec les données fournies (date de contrat ET date de bilan de fusion)
		for (Entreprise absorbee : absorbees) {

			// recherche du bon rapport entre tiers
			boolean trouveBonRapportEntreTiers = false;
			for (RapportEntreTiers ret : absorbee.getRapportsSujet()) {
				if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.FUSION_ENTREPRISES && ret.getDateDebut() == dateBilanFusion && ret.getObjetId().equals(absorbante.getId())) {
					trouveBonRapportEntreTiers = true;
					break;
				}
			}
			if (!trouveBonRapportEntreTiers) {
				throw new MetierServiceException(String.format("L'entreprise %s n'est pas associée à une absorption par l'enteprise %s avec une date de bilan de fusion au %s.",
				                                               FormatNumeroHelper.numeroCTBToDisplay(absorbee.getNumero()),
				                                               FormatNumeroHelper.numeroCTBToDisplay(absorbante.getNumero()),
				                                               RegDateHelper.dateToDisplayString(dateBilanFusion)));
			}

			// recherche de l'état absorbé à la date de contrat de fusion
			final List<EtatEntreprise> etats = absorbee.getEtatsNonAnnulesTries();
			boolean trouveBonEtat = false;
			for (EtatEntreprise etat : CollectionsUtils.revertedOrder(etats)) {
				if (etat.getType() == TypeEtatEntreprise.ABSORBEE && etat.getDateObtention() == dateContratFusion) {
					trouveBonEtat = true;
					break;
				}
			}
			if (!trouveBonEtat) {
				throw new MetierServiceException(String.format("L'entreprise %s n'est pas associée à une absorption par l'entreprise %s avec une date de contrat de fusion au %s.",
				                                               FormatNumeroHelper.numeroCTBToDisplay(absorbee.getNumero()),
				                                               FormatNumeroHelper.numeroCTBToDisplay(absorbante.getNumero()),
				                                               RegDateHelper.dateToDisplayString(dateContratFusion)));
			}
		}

		// identifiants des entreprises absorbées
		final Set<Long> idsAbsorbees = new HashSet<>(absorbees.size());
		for (Entreprise absorbee : absorbees) {
			idsAbsorbees.add(absorbee.getNumero());
		}

		// annulation des rappors entre tiers entre l'absorbante et les absorbées à la date de bilan de fusion
		for (RapportEntreTiers ret : absorbante.getRapportsObjet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.FUSION_ENTREPRISES && ret.getDateDebut() == dateBilanFusion && idsAbsorbees.contains(ret.getSujetId())) {
				ret.setAnnule(true);
			}
		}

		// réouverture des fors, annulation de la surcharge d'adresse, de l'état absorbé...
		for (Entreprise absorbee : absorbees) {

			// réouverture des fors fermés
			tiersService.reopenForsClosedAt(dateBilanFusion, MotifFor.FUSION_ENTREPRISES, absorbee);

			// annulation de l'état absorbé à la date de contrat
			for (EtatEntreprise etat : absorbee.getEtats()) {
				if (!etat.isAnnule() && etat.getType() == TypeEtatEntreprise.ABSORBEE && etat.getDateObtention() == dateContratFusion) {
					etat.setAnnule(true);
				}
			}

			// annulation de la surcharge d'adresse courrier
			final List<AdresseTiers> surchargesCourrier = absorbee.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
			if (surchargesCourrier != null && !surchargesCourrier.isEmpty()) {
				final AdresseTiers derniereSurcharge = CollectionsUtils.getLastElement(surchargesCourrier);
				if (derniereSurcharge.getDateDebut() == dateContratFusion) {
					adresseService.annulerAdresse(derniereSurcharge);
				}
			}
		}
	}
}
