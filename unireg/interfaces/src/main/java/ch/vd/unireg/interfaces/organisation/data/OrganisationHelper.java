package ch.vd.unireg.interfaces.organisation.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Quelques méthodes pratiques d'interprétation des données dans le cadre des organisations
 *
 * Règle générale: Toutes les dates sont optionnelles et en leur absence, la date du jour est utilisée.
 *
 * Utiliser la méthode defaultDate(RegDate date) pour gérer la date automatiquement.
 */
public abstract class OrganisationHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationHelper.class);

	/*
		Nombre de jour servant à calculer le seuil de proximité requis pour considérer une date d'inscription ou de
		radiation du RC comme étant liés à l'événement de création ou d'arrivée/départ en cours.

		Ex.: une date d'inscription au RC Suisse antérieur de plus de NB_JOURS_TOLERANCE_DE_DECALAGE_RC par rapport à la date
		     d'événement de nouvelle entreprise signale une entreprise existante, mais nouvellement connue de RCEnt.
	 */
	public static final int NB_JOURS_TOLERANCE_DE_DECALAGE_RC = 15;

	/**
	 * Détermine les valeurs en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param map
	 * @param date
	 * @param <K> Type de la clé identifiant la valeur
	 * @param <V> Type de la valeur
	 * @return La liste des valeurs pour la date fournie ou la date du jour si non fournie. null si map était null
	 */
	public static <K, V> List<V> valuesForDate(@Nullable Map<K, List<DateRanged<V>>> map, @Nullable RegDate date) {
		if (map == null) {
			return null;
		}
		List<V> na = new ArrayList<>();
		for (Map.Entry<K, List<DateRanged<V>>> entry : map.entrySet()) {
			final DateRanged<V> vDateRanged = DateRangeHelper.rangeAt(entry.getValue(), defaultDate(date));
			if (vDateRanged != null) {
				na.add(vDateRanged.getPayload());
			}
		}
		return na;
	}

	/**
	 * Détermine la valeur en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param list
	 * @param date Date de valeur
	 * @param <V> Type de la valeur
	 * @return La valeur pour la date fournie, ou null si pas de valeur à cette date.
	 */
	public static <V> V valueForDate(@Nullable List<DateRanged<V>> list, @Nullable RegDate date) {
		if (list == null) {
			return null;
		}
		final DateRanged<V> item = DateRangeHelper.rangeAt(list, defaultDate(date));
		return item != null ? item.getPayload() : null;
	}

	/**
	 * Détermine la valeur en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param list
	 * @param date Date de valeur
	 * @param <V> Type de la valeur
	 * @return La valeur pour la date fournie, ou null si pas de valeur à cette date.
	 */
	public static <V extends DateRange> V dateRangeForDate(@Nullable List<V> list, @Nullable RegDate date) {
		if (list == null) {
			return null;
		}
		return  DateRangeHelper.rangeAt(list, defaultDate(date));
	}

	/**
	 * @param identifiants map d'identifiants datés triés par une clé qui indique leur type (numéro IDE, identifiant cantonal...)
	 * @param cle la clé en question (CT.VD.PARTY pour l'identifiant cantonal, CH.IDE pour le numéro IDE)
	 * @return la liste historisée des valeurs de ce type
	 */
	@Nullable
	public static List<DateRanged<String>> extractIdentifiant(Map<String, List<DateRanged<String>>> identifiants, String cle) {
		final List<DateRanged<String>> extracted = identifiants == null ? null : identifiants.get(cle);
		return extracted == null || extracted.isEmpty() ? null : extracted;
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	public static FormeLegale getFormeLegale(Organisation organisation, RegDate date) {
		DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(organisation.getFormeLegale(), defaultDate(date));
		return formeLegaleRange != null ? formeLegaleRange.getPayload() : null;
	}

	/**
	 * Determine le siège principal en vigueur le jour précédant la date.
	 * @param organisation L'organisation concernée
	 * @param date La date de référence
	 * @return Le siège le jour précédant, ou null si aucun.
	 */
	public static Domicile siegePrincipalPrecedant(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getSiegesPrincipaux(), defaultDate(date).getOneDayBefore());
	}

	public static DateRanged<SiteOrganisation> getSitePrincipal(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getSitePrincipaux(), defaultDate(date));
	}

	/**
	 * Liste des sites principaux
	 * @return La liste des sites principaux
	 */
	public static List<DateRanged<SiteOrganisation>> getSitePrincipaux(Organisation organisation) {
		List<DateRanged<SiteOrganisation>> sitePrincipaux = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
					sitePrincipaux.add(new DateRanged<>(siteRange.getDateDebut(), siteRange.getDateFin(), site));
				}
			}
		}
		return sitePrincipaux;
	}

	/**
	 * Liste des sites secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des sites secondaires
	 * @return La liste des sites secondaire
	 */
	public static List<SiteOrganisation> getSitesSecondaires(Organisation organisation, @Nullable RegDate date) {
		List<SiteOrganisation> siteSecondaires = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_SECONDAIRE && siteRange.isValidAt(defaultDate(date))) {
					siteSecondaires.add(site);
				}
			}
		}
		return siteSecondaires;
	}

	private static List<Adresse> concat(@Nullable List<? extends Adresse> one, @Nullable List<? extends Adresse> two) {
		final List<Adresse> liste = new ArrayList<>((one != null ? one.size() : 0) + (two != null ? two.size() : 0));
		if (one != null) {
			liste.addAll(one);
		}
		if (two != null) {
			liste.addAll(two);
		}
		return liste;
	}

	public static List<Adresse> getAdresses(Map<Long, ? extends SiteOrganisation> donneesSites) {
		final List<AdresseLegaleRCEnt> rcLegale = extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<AdresseLegaleRCEnt>(), new SiteDataExtractor<List<AdresseLegaleRCEnt>>() {
			@Override
			public List<AdresseLegaleRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRC() == null ? null : site.getDonneesRC().getAdresseLegale();
			}
		});
		final List<AdresseEffectiveRCEnt> ideEffective = extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<AdresseEffectiveRCEnt>(), new SiteDataExtractor<List<AdresseEffectiveRCEnt>>() {
			@Override
			public List<AdresseEffectiveRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRegistreIDE() == null ? null : site.getDonneesRegistreIDE().getAdresseEffective();
			}
		});
		return concat(rcLegale, ideEffective);
	}

	public static List<Adresse> getAdressesPourSite(SiteOrganisation site) {
		final List<AdresseLegaleRCEnt> rcLegale = site.getDonneesRC() == null ? null : site.getDonneesRC().getAdresseLegale();
		final List<AdresseEffectiveRCEnt> ideEffective = site.getDonneesRegistreIDE() == null ? null : site.getDonneesRegistreIDE().getAdresseEffective();
		return concat(rcLegale, ideEffective);
	}

	/**
	 * Retourne une liste représantant la succession des valeurs de capital de l'entreprise.
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	public static List<Capital> getCapitaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<Capital>(), new SiteDataExtractor<List<Capital>>() {
			@Override
			public List<Capital> extractData(SiteOrganisation site) {
				return site.getDonneesRC() != null ? site.getDonneesRC().getCapital() : null;
			}
		});
	}

	public static Capital getCapital(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getCapitaux(), defaultDate(date));
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	public static List<Domicile> getSiegesPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<Domicile>(), new SiteDataExtractor<List<Domicile>>() {
			@Override
			public List<Domicile> extractData(SiteOrganisation site) {
				return site.getDomiciles();
			}
		});
	}

	/**
	 * Prepare une liste de plages représantant la succession des noms des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le nom qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages noms correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de nom.
	 */
	public static List<DateRanged<String>> getNomsPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractRangedDataFromSitesPrincipaux(donneesSites, new SiteDataExtractor<List<DateRanged<String>>>() {
			@Override
			public List<DateRanged<String>> extractData(SiteOrganisation site) {
				return site.getNom();
			}
		});
	}

	/**
	 * Prepare une liste de plages représantant la succession des noms additionnels des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le nom additionnel qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages noms additionnels correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de nom.
	 */
	public static List<DateRanged<String>> getNomsAdditionnelsPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractRangedDataFromSitesPrincipaux(donneesSites, new SiteDataExtractor<List<DateRanged<String>>>() {
			@Override
			public List<DateRanged<String>> extractData(SiteOrganisation site) {
				return site.getNomAdditionnel();
			}
		});
	}

	/**
	 * Prepare une liste de plages représantant la succession des formes legales des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la forme legale qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages formes legales correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de forme legale.
	 */
	public static List<DateRanged<FormeLegale>> getFormesLegalesPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractRangedDataFromSitesPrincipaux(donneesSites, new SiteDataExtractor<List<DateRanged<FormeLegale>>>() {
			@Override
			public List<DateRanged<FormeLegale>> extractData(SiteOrganisation site) {
				return site.getFormeLegale();
			}
		});
	}

	/**
	 * Prepare une liste de plages représantant la succession des numéros IDE des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le numéro IDE qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages numéros IDE correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information des numéros IDE.
	 */
	public static List<DateRanged<String>> getNumerosIDEPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites) {
		return extractRangedDataFromSitesPrincipaux(donneesSites, new SiteDataExtractor<List<DateRanged<String>>>() {
			@Override
			public List<DateRanged<String>> extractData(SiteOrganisation site) {
				return site.getNumeroIDE();
			}
		});
	}


	/**
	 * Extrait les entrées de journal publiées à la FOSC à la date correspondant.
	 * @param entrees liste d'entrées de journal avec publication FOSC
	 * @param date date de publication à la FOSC
	 * @return les entrées de journal correspondant à la date de publication
	 */
	@NotNull
	public static List<EntreeJournalRC> getEntreesJournalPourDatePublication(List<EntreeJournalRC> entrees, RegDate date) {
		if (entrees == null || entrees.isEmpty()) {
			return Collections.emptyList();
		}
		final List<EntreeJournalRC> entreesPourDate = new ArrayList<>();
		final RegDate dateEffective = defaultDate(date);
		for (EntreeJournalRC entreeJournalRC : entrees) {
			if (entreeJournalRC.getPublicationFOSC().getDate() == dateEffective) {
				entreesPourDate.add(entreeJournalRC);
			}
		}
		return entreesPourDate;
	}

	/**
	 * Détermine la date de premier snapshot du site. C'est à dire à partir de quand le site
	 * est connu au civil.
	 * @param site le site visé
	 * @return la date du premier snapshot
	 */
	public static RegDate connuAuCivilDepuis(SiteOrganisation site) {
		final List<DateRanged<String>> nom = site.getNom();
		Collections.sort(nom, new DateRangeComparator<>());
		return nom.get(0).getDateDebut();
	}

	/**
	 * Une organisation est réputée inscrite au RC à la date fournie si le statut de son site principal n'est ni INCONNU, ni NON_INSCRIT.
	 * (<i>inscrite</i> doit être comprise dans le sens de <i>possède une inscription</i>, quelle qu'elle soit)
 	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation au RC
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscriteAuRC(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);

		// il n'y a aucun site principal avant la date de chargement initial de RCEnt... mais parfois, la date demandée est elle-même
		// antérieure à cette date de chargement, il faut donc ruser un peu et regarder les dates...
		for (DateRanged<SiteOrganisation> sitePrincipal : organisation.getSitePrincipaux()) {
			if (isInscritAuRC(sitePrincipal.getPayload(), dateEffective)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Une organisation est "connue comme inscrite au RC" à une date si la donnée de l'inscription RC connue à cette date existe et la décrit comme inscrite
	 * (au sens de <i>possède une inscription</i>, quelle qu'elle soit)
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on se pose la question
	 * @return true si connue comme inscrite, false sinon
	 */
	public static boolean isConnueInscriteAuRC(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isConnuInscritAuRC(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Un site est réputé inscrit au RC à la date fournie si son statut n'est ni INCONNU, ni NON_INSCRIT.
	 * (<i>inscrit</i> doit être comprise dans le sens de <i>possède une inscription</i>, quelle qu'elle soit)
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation au RC
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscritAuRC(SiteOrganisation site, RegDate date) {
		final DonneesRC donneesRC = site.getDonneesRC();
		if (donneesRC == null || donneesRC.getInscription() == null) {
			return false;
		}

		// il n'y a aucune donnée civile avant la date de chargement initial de RCEnt... mais parfois, la date demandée est elle-même
		// antérieure à cette date de chargement, il faut donc ruser un peu et regarder les dates...
		final RegDate dateEffective = defaultDate(date);
		for (DateRanged<InscriptionRC> inscription : donneesRC.getInscription()) {
			final InscriptionRC inscriptionData = inscription.getPayload();
			if (inscriptionData.isInscrit()) {
				// une entreprise qui a un jour une inscription valide dans un RC sera considérée comme toujours
				// inscrite (pas forcément active, cependant, mais inscrite en tout cas) après la date d'inscription
				if (RegDateHelper.isBeforeOrEqual(inscriptionData.getDateInscriptionVD(), dateEffective, NullDateBehavior.LATEST)
						|| RegDateHelper.isBeforeOrEqual(inscriptionData.getDateInscriptionCH(), dateEffective, NullDateBehavior.LATEST)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Un établissement est "connu comme inscrit au RC" à une date si la donnée de l'inscription RC connue à cette date existe et le décrit comme inscrit
	 * (au sens de <i>possède une inscription</i>, quelle qu'elle soit)
	 * @param site le site
	 * @param date la date pour laquelle on se pose la question
	 * @return true si connue comme inscrite, false sinon
	 */
	public static boolean isConnuInscritAuRC(SiteOrganisation site, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DonneesRC donneesRC = site.getDonneesRC();
		if (donneesRC == null || donneesRC.getInscription() == null) {
			return false;
		}
		final InscriptionRC inscriptionConnue = donneesRC.getInscription(dateEffective);
		return inscriptionConnue != null && inscriptionConnue.isInscrit();
	}

	/**
	 * Une organisation est réputée inscrite à l'IDE à la date fournie si le statut de son site principal n'est ni AUTRE, ni ANNULE.
	 * (<i>inscrite</i> doit être comprise dans le sens de <i>possède une inscription</i>, quelle qu'elle soit)
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation à l'IDE
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscriteIDE(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isInscritIDE(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Un site est réputé inscrit à l'IDE à la date fournie si son statut n'est ni AUTRE, ni ANNULE.
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation à l'IDE
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscritIDE(SiteOrganisation site, RegDate date) {
		final DonneesRegistreIDE donneesIDE = site.getDonneesRegistreIDE();
		if (donneesIDE == null) {
			return false;
		}
		final StatusRegistreIDE statusInscription = donneesIDE.getStatus(defaultDate(date));
		return statusInscription != null && !(statusInscription == StatusRegistreIDE.AUTRE || statusInscription == StatusRegistreIDE.ANNULE);
	}

	/**
	 * Une organisation est réputée inscrite au REE à la date fournie si le statut de son site principal n'est pas vide.
	 * (<i>inscrite</i> doit être comprise dans le sens de <i>possède une inscription</i>, quelle qu'elle soit)
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation au REE
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscriteREE(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isInscritREE(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Un site est réputé inscrit au REE à la date fournie s'il a un statut non vide.
	 *
	 * Voir SIFISC-18739: INCONNU == inscrit dont le REE ne connais pas la situation exacte entre "actif" et "inactif", qui eux-mêmes
	 * concerne la présence ou non d'employés dans l'entité.
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation au l'REE
	 * @return true si inscrite, false sinon
	 */
	public static boolean isInscritREE(SiteOrganisation site, RegDate date) {
		final DonneesREE donneesREE = site.getDonneesREE();
		if (donneesREE == null || donneesREE.getInscriptionREE() == null) {
			return false;
		}

		// il n'y a aucune donnée civile avant la date de chargement initial de RCEnt... mais parfois, la date demandée est elle-même
		// antérieure à cette date de chargement, il faut donc ruser un peu et regarder les dates...
		final RegDate dateEffective = defaultDate(date);
		for (DateRanged<InscriptionREE> inscription : donneesREE.getInscriptionREE()) {
			final InscriptionREE inscriptionData = inscription.getPayload();
			if (inscriptionData.getStatus() != null && RegDateHelper.isBeforeOrEqual(inscriptionData.getDateInscription(), dateEffective, NullDateBehavior.LATEST)) {
				// une entreprise qui a un jour une inscription valide au REE sera considérée comme toujours
				// inscrite (pas forcément active, cependant, mais inscrite en tout cas) après la date d'inscription
				return true;
			}
		}
		return false;
	}

	public static boolean isConnuInscritREE(SiteOrganisation site, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DonneesREE donneesREE = site.getDonneesREE();
		if (donneesREE == null || donneesREE.getInscriptionREE() == null) {
			return false;
		}
		final InscriptionREE inscription = donneesREE.getInscriptionREE(dateEffective);
		return inscription != null && inscription.getStatus() != null && RegDateHelper.isBeforeOrEqual(inscription.getDateInscription(), dateEffective, NullDateBehavior.LATEST);
	}

	/**
	 * Un site est réputé être une succursale à la date fournie s'il est inscrit au RC, si son statut n'est
	 * ni INCONNU, ni NON_INSCRIT et qu'il n'est pas radié du RC.
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre l'état de succursale
	 * @return
	 */
	public static boolean isSuccursale(SiteOrganisation site, RegDate date) {
		return site.getTypeDeSite(date) == TypeDeSite.ETABLISSEMENT_SECONDAIRE && isConnuInscritAuRC(site, date) && !isRadieDuRC(site, date);
	}

	/**
	 * Une organisation est réputée radiée du RC à la date fournie si le statut de son site principal RADIE.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation au RC
	 * @return true si inscrite, false sinon
	 */
	public static boolean isRadieeDuRC(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isRadieDuRC(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Dire si un site est globallement actif, c'est à dire qu'il a une existence à la date fournie chez au
	 * moins un fournisseur primaire (RC, IDE). Etre actif signifie être inscrit et non radié.
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation du site
	 * @return true si actif, false sinon
	 */
	public static boolean isActif(SiteOrganisation site, RegDate date) {
		if (isConnuInscritAuRC(site, date) && !isRadieDuRC(site, date)) {
			return true;
		}
		else if (isInscritIDE(site, date) && !isRadieIDE(site, date)) {
			return true;
		}
		else if (isConnuInscritREE(site, date) && !isRadieREE(site, date)) {
			return true;
		}
		return false;
	}

	/**
	 * Un site est réputé radié du RC à la date fournie si son statut est RADIE
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation au RC
	 * @return true si radié, false sinon
	 */
	public static boolean isRadieDuRC(SiteOrganisation site, RegDate date) {
		final DonneesRC donneesRC = site.getDonneesRC();
		final InscriptionRC inscription = donneesRC.getInscription(defaultDate(date));
		return inscription != null && inscription.getStatus() == StatusInscriptionRC.RADIE;
	}

	/**
	 * Une organisation est réputée radiée de l'IDE à la date fournie si le statut de son site principal RADIE ou DEFINITIVEMENT_RADIE.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation à l'IDE
	 * @return true si radié, false sinon
	 */
	public static boolean isRadieeIDE(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isRadieIDE(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Un site est réputé radié de l'IDE à la date fournie si son statut est RADIE ou DEFINITIVEMENT_RADIE
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation à l'IDE
	 * @return true si radié, false sinon
	 */
	public static boolean isRadieIDE(SiteOrganisation site, RegDate date) {
		final DonneesRegistreIDE donneesIDE = site.getDonneesRegistreIDE();
		if (donneesIDE != null && donneesIDE.getStatus() != null && ! donneesIDE.getStatus().isEmpty()) {
			final RegDate dateEffective = defaultDate(date);
			final StatusRegistreIDE statusIde = donneesIDE.getStatus(dateEffective);
			return statusIde == StatusRegistreIDE.RADIE || donneesIDE.getStatus(dateEffective) == StatusRegistreIDE.DEFINITIVEMENT_RADIE;
		}
		return false;
	}

	/**
	 * Une organisation est réputée radiée du REE à la date fournie si le statut de son site principal RADIE ou TRANSFERE.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut connaitre la situation au REE
	 * @return true si radié, false sinon
	 */
	public static boolean isRadieREE(Organisation organisation, RegDate date) {
		final RegDate dateEffective = defaultDate(date);
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateEffective);
		return sitePrincipal != null && isRadieREE(sitePrincipal.getPayload(), dateEffective);
	}

	/**
	 * Un site est réputé radié du REE à la date fournie si son statut est RADIE ou TRANSFERE
	 *
	 * Repris SIFISC-18739 pour la documentation du statut REE (citation de Gabrielle Servoz, RCEnt):
	 *
	 * <ul>
	 *     <li>"actif REE" veut dire d'un point de vue du REE : "entité avec de l'emploi au sens de la statistique"</li>
	 *     <li>"inactif REE" veut dire : "pas d'emploi au sens de la statistique, entreprise boite au lettre, lieux d'activité sans personnel permanant ..." mais non radié.</li>
	 *     <li>"inconnu REE" veut dire "entité inscrite dans un registre mais dont le statut n'a pas encore été confirmé. En attente de réponse à l'enquête faite par le REE"</li>
	 *     <li>"radié REE" veut dire "établissement radié ou fermé"</li>
	 *     <li>"transféré REE" veut dire "transféré suite à fusion, scission, déménagement ..." => il y a forcément un autre établissement qui était déjà "actif" en remplacement.</li>
	 * </ul>
	 * => pour être binaire : un établissement est pour moi actif si son statut REE vaut "actif", "inactif" ou "inconnu". Un établissement est pour moi radié si son statut vaut "radié" ou "transféré".
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation au REE
	 * @return true si radié, false sinon
	 */
	public static boolean isRadieREE(SiteOrganisation site, RegDate date) {
		final DonneesREE donneesREE = site.getDonneesREE();
		if (donneesREE != null) {
			final InscriptionREE inscription = donneesREE.getInscriptionREE(defaultDate(date));
			if (inscription != null) {
				return inscription.getStatus() == StatusREE.RADIE || inscription.getStatus() == StatusREE.TRANSFERE;
			}
		}
		return false;
	}

	/**
	 * Détermine si l'organisation a son site principal (siège) domicilié sur Vaud.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut l'information
	 * @return true si le site principal est domicilié dans le canton de Vaud, false sinon
	 */
	public static boolean hasSitePrincipalVD(Organisation organisation, RegDate date) {
		Domicile siegePrincipal = organisation.getSiegePrincipal(defaultDate(date));
		return siegePrincipal != null && siegePrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	/**
	 * Renvoie la liste des sites domiciliés sur Vaud (principal ou secondaires) qui sont
	 * des succursales inscrites au RC et non radiées.
	 *
	 * Pour éviter les établissements REE, le critère est l'inscription au RC.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut l'information
	 * @return la liste des sites domiciliés sur Vaud inscrits au RC
	 */
	public static List<SiteOrganisation> getSuccursalesRCVD(Organisation organisation, RegDate date) {
		List<SiteOrganisation> sitesVD = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Domicile domicile = site.getDomicile(defaultDate(date));
			if (domicile != null &&
					domicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					site.isSuccursale(date)) {
				sitesVD.add(site);
			}
		}
		return sitesVD;
	}

	/**
	 * Détermine si l'organisation a un intérêt sur VD sous la forme d'un site principal ou secondaire
	 * domicilié sur Vaud.
	 *
	 * @param organisation l'organisation
	 * @param date la date pour laquelle on veut l'information
	 * @return true si un site est domicilié dans le canton de Vaud, false sinon
	 */
	public static boolean hasSiteVD(Organisation organisation, RegDate date) {
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Domicile domicile = site.getDomicile(defaultDate(date));
			if (domicile != null && domicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Donne l'historique des domiciles réels, c'est à dire qui se termine lorsque le site ferme.
	 */
	public static List<Domicile> getDomicilesReels(SiteOrganisation site, List<Domicile> domiciles) {
		final List<DateRange> activite = activite(site);
		if (domiciles == null || domiciles.isEmpty() || activite == null || activite.isEmpty()) {
			return Collections.emptyList();
		}
		final Domicile premierDomicile = domiciles.get(0);
		final RegDate debutDomiciles = premierDomicile.getDateDebut();
		final RegDate debutActivite = activite.get(0).getDateDebut();
		final Domicile[] domicilesDebutCorrige = domiciles.toArray(new Domicile[domiciles.size()]);
		if (debutActivite.isBefore(debutDomiciles) && !debutActivite.isBefore(debutDomiciles.addDays( - NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
			domicilesDebutCorrige[0] = new Domicile(debutActivite, premierDomicile.getDateFin(), premierDomicile.getTypeAutoriteFiscale(), premierDomicile.getNumeroOfsAutoriteFiscale());
		}

		final List<Domicile> domicilesResult = DateRangeHelper.extract(Arrays.asList(domicilesDebutCorrige), activite, new DateRangeHelper.AdapterCallback<Domicile>() {
			@Override
			public Domicile adapt(Domicile range, RegDate debut, RegDate fin) {
				return new Domicile(debut != null ? debut : range.getDateDebut(),
				                    fin != null ? fin : range.getDateFin(),
				                    range.getTypeAutoriteFiscale(),
				                    range.getNumeroOfsAutoriteFiscale());
			}
		});
		return DateRangeHelper.collate(domicilesResult);
	}

	/**
	 * <p>
	 *     Détermine la plage d'activité d'une organisation, c'est à dire la période à laquelle on la considère en
	 *     existence au sens de "non radiée" RC et/ou IDE.
	 * </p>
	 * <p>
	 *     NOTES:
	 *     <ul>
	 *         <li>
	 *             Les organisations radiées puis réinscrites sont considérées comme actives durant toutes la période ou elles
	 *             on été radiées.
	 *         </li>
	 *         <li>
	 *             Lorsque la radiation du RC est liée à celle de l'IDE (c'est à dire que l'IDE ne fait qu'enregistrer l'état de fait
	 *             au RC), c'est la date du RC qui est utilisée. Actuellement, le seuil {@link OrganisationHelper#NB_JOURS_TOLERANCE_DE_DECALAGE_RC} est
	 *             utilisé pour déterminer cela.
	 *         </li>
	 *         <li>
	 *             Le REE n'est pas encore supporté. Les éventuels établissements REE ne "meurent" pas.
	 *         </li>
	 *     </ul>
	 * </p>
	 *
	 * @param site le site concernée
	 * @return la période où le site a été en activité (sous forme de liste, mais il ne devrait y en avoir qu'une)
	 */
	public static List<DateRange> activite (SiteOrganisation site) {

		// trouvons la première information d'inscription
		InscriptionRC first = null;
		final List<DateRanged<InscriptionRC>> inscriptions = site.getDonneesRC().getInscription();
		if (inscriptions != null) {
			for (DateRanged<InscriptionRC> inscription : inscriptions) {
				if (inscription.getPayload().isInscrit()) {
					first = inscription.getPayload();
					break;
				}
			}
		}

		final RegDate dateCreation;
		if (first == null) {
			dateCreation = site.getNom().get(0).getDateDebut();
		}
		else if (first.getDateInscriptionVD() != null && first.getDateInscriptionCH() == null) {
			// Cas de la date RC vide quand la date RC VD est renseignée. Impossible de déterminer quoi que ce soit dans ce cas
			throw new RuntimeException(String.format("Impossible de trouver la date d'inscription au RC pour le site %s", site.getNumeroSite()));
		}
		else {
			dateCreation = RegDateHelper.minimum(site.getNom().get(0).getDateDebut(), first.getDateInscriptionCH(), NullDateBehavior.LATEST);
		}

		/* Une période théorique d'activité continue non terminée. */
		final DateRange activite = new DateRangeHelper.Range(dateCreation, null);

/*  == On peut omettre, car les inscrites au RC sont obligatoirement à l'IDE et la date est gérée avec ce dernier. ==

		final List<DateRange> datesRadiation = new ArrayList<>();
		List<DateRange> nonRadiation = Collections.emptyList();
		if (!site.getDonneesRC().getDateRadiation().isEmpty()) {
			for (DateRanged<RegDate> dateRadiation : site.getDonneesRC().getDateRadiation()) {
				final RegDate dateEffective = dateRadiation.getPayload();
				if (dateEffective != dateRadiation.getDateDebut()) {
					datesRadiation.add(new DateRangeHelper.Range(dateEffective, dateRadiation.getDateFin()));
				} else {
					datesRadiation.add(dateRadiation);
				}
			}
			nonRadiation = DateRangeHelper.subtract(activite, datesRadiation);
		}

		final List<DateRange> datesRadiationVd = new ArrayList<>();
		List<DateRange> nonRadiationVd = Collections.emptyList();
		if (!site.getDonneesRC().getDateRadiationVd().isEmpty()) {
			for (DateRanged<RegDate> dateRadiationVd : site.getDonneesRC().getDateRadiationVd()) {
				final RegDate dateEffectiveVd = dateRadiationVd.getPayload();
				if (dateEffectiveVd != dateRadiationVd.getDateDebut()) {
					datesRadiationVd.add(new DateRangeHelper.Range(dateEffectiveVd, dateRadiationVd.getDateFin()));
				} else {
					datesRadiationVd.add(dateRadiationVd);
				}
			}
			nonRadiationVd = DateRangeHelper.subtract(activite, datesRadiationVd);
		}
*/

		final List<DateRanged<StatusRegistreIDE>> periodesStatusIde = site.getDonneesRegistreIDE().getStatus();
		final List<DateRange> datesRadieIde = new ArrayList<>();
		List<DateRange> nonRadiationIde = Collections.emptyList();
		if (periodesStatusIde != null && !periodesStatusIde.isEmpty()) {
			final DateRanged<StatusRegistreIDE> dernierePeriode = CollectionsUtils.getLastElement(periodesStatusIde);
			final StatusRegistreIDE status = dernierePeriode.getPayload();
			if (dernierePeriode.getDateFin() == null && (status == StatusRegistreIDE.RADIE || status == StatusRegistreIDE.DEFINITIVEMENT_RADIE)) {
				final RegDate dateRadiationRC = site.getDateRadiationRC(dernierePeriode.getDateDebut());
				if (dateRadiationRC != null && !dateRadiationRC.isBefore(dernierePeriode.getDateDebut().addDays( - NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
					datesRadieIde.add(new DateRangeHelper.Range(dateRadiationRC.getOneDayAfter(), dernierePeriode.getDateFin()));
				} else {
					datesRadieIde.add(dernierePeriode);
				}
			}
			nonRadiationIde = DateRangeHelper.subtract(activite, datesRadieIde);
		}

		// TODO FIXME Inclure la notion de mort par transfert! Cas du changement de propriétaire remonté par Madame Viquerat.

		// Infrastructure nécessaire pour ajouter le support de la mort REE
		final List<DateRange> tousActivite = new ArrayList<>();
		if (nonRadiationIde.isEmpty()) {
			tousActivite.add(activite);
		} else {
			tousActivite.addAll(nonRadiationIde);
		}

		final List<DateRange> combinesNonRadies = DateRangeHelper.merge(tousActivite);

		if (combinesNonRadies.size() > 1) {
			LOGGER.warn(String.format("Le calcul de l'activité pour le site RCEnt %d a retourné plus d'une période.", site.getNumeroSite()));
		}
		return combinesNonRadies;
	}

	private static RegDate defaultDate(RegDate date) {
		return date != null ? date : RegDate.get();
	}

	private interface SiteDataExtractor<T> {
		T extractData(SiteOrganisation site);
	}

	private interface DateRangeLimitator<T extends DateRange> {
		T limitTo(T source, RegDate dateDebut, RegDate dateFin);
	}

	private static class DateRangeLimitatorImpl<T extends DateRange & DateRangeLimitable<T>> implements DateRangeLimitator<T> {
		public T limitTo(T source, RegDate dateDebut, RegDate dateFin) {
			return source.limitTo(dateDebut, dateFin);
		}
	}

	@NotNull
	private static <T> List<DateRanged<T>> extractRangedDataFromSitesPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites, SiteDataExtractor<List<DateRanged<T>>> extractor) {
		final List<DateRanged<T>> extracted = new ArrayList<>();
		for (Map.Entry<Long, ? extends SiteOrganisation> entry : donneesSites.entrySet()) {
			final SiteOrganisation site = entry.getValue();
			final List<DateRanged<T>> toExtract = extractor.extractData(site);
			if (toExtract != null && !toExtract.isEmpty()) {
				for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
					if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
						final List<DateRanged<T>> extractedData = DateRangeHelper.extract(toExtract,
						                                                                  type.getDateDebut(),
						                                                                  type.getDateFin(),
						                                                                  new DateRangeHelper.AdapterCallback<DateRanged<T>>() {
							                                                                  @Override
							                                                                  public DateRanged<T> adapt(DateRanged<T> range, RegDate debut, RegDate fin) {
								                                                                  return new DateRanged<>(debut != null ? debut : range.getDateDebut(),
								                                                                                          fin != null ? fin : range.getDateFin(),
								                                                                                          range.getPayload());
							                                                                  }
						                                                                  });
						extracted.addAll(extractedData);
					}
				}
			}
		}
		Collections.sort(extracted, new DateRangeComparator<>());
		return extracted;
	}

	@NotNull
	private static <T extends DateRange> List<T> extractDataFromSitesPrincipaux(Map<Long, ? extends SiteOrganisation> donneesSites, final DateRangeLimitator<T> limitator, SiteDataExtractor<List<T>> extractor) {
		final List<T> extracted = new ArrayList<>();
		for (Map.Entry<Long, ? extends SiteOrganisation> entry : donneesSites.entrySet()) {
			final SiteOrganisation site = entry.getValue();
			final List<T> toExtract = extractor.extractData(site);
			if (toExtract != null && !toExtract.isEmpty()) {
				for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
					if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
						final List<T> extractedData = DateRangeHelper.extract(toExtract,
						                                                      type.getDateDebut(),
						                                                      type.getDateFin(),
						                                                      buildAdapterCallbackFromLimitator(limitator));
						extracted.addAll(extractedData);
					}
				}
			}
		}
		Collections.sort(extracted, new DateRangeComparator<>());
		return extracted;
	}

	private static <T extends DateRange> DateRangeHelper.AdapterCallback<T> buildAdapterCallbackFromLimitator(final DateRangeLimitator<T> limitator) {
		return new DateRangeHelper.AdapterCallback<T>() {
			@Override
			public T adapt(T range, RegDate debut, RegDate fin) {
				if (debut == null && fin == null) {
					return range;
				}
				return limitator.limitTo(range,
				                         debut == null ? range.getDateDebut() : debut,
				                         fin == null ? range.getDateFin() : fin);
			}
		};
	}
}
