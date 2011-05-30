package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.EntrepriseIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.webservices.tiers3.Adresse;
import ch.vd.uniregctb.webservices.tiers3.AdresseAutreTiers;
import ch.vd.uniregctb.webservices.tiers3.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers3.AdresseEnvoiAutreTiers;
import ch.vd.uniregctb.webservices.tiers3.Date;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.TiersInfo;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.AdresseBuilder;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'web'.
 * <p/>
 * De manière naturelle, ces méthodes auraient dû se trouver dans les classes 'web' correspondantes, mais cela provoque des erreurs (les classes 'core' sont aussi inspectées et le fichier se retrouve
 * avec des structures ayant le même nom définies plusieurs fois) lors la génération du WSDL par CXF.
 */
public class DataHelper {

	//private static final Logger LOGGER = Logger.getLogger(DataHelper.class);

	public static boolean coreToWeb(Boolean value) {
		return value != null && value;
	}

	public static Date coreToWeb(java.util.Date date) {
		if (date == null) {
			return null;
		}
		else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			final Date d = new Date();
			d.setYear(cal.get(Calendar.YEAR));
			d.setMonth(cal.get(Calendar.MONTH) + 1);
			d.setDay(cal.get(Calendar.DAY_OF_MONTH));
			return d;
		}
	}

	public static Date coreToWeb(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			final Date d = new Date();
			d.setYear(date.year());
			d.setMonth(date.month());
			d.setDay(date.day());
			return d;
		}
	}

	public static RegDate webToCore(Date date) {
		if (date == null) {
			return null;
		}
		else {
			return RegDate.get(date.getYear(), date.getMonth(), date.getDay());
		}
	}

	public static List<Adresse> coreToWeb(List<AdresseGenerique> adresses,
	                                      @org.jetbrains.annotations.Nullable DateRangeHelper.Range range,
	                                      ServiceInfrastructureService serviceInfra) throws WebServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<Adresse> list = new ArrayList<Adresse>();
		for (AdresseGenerique a : adresses) {
			if (a.isAnnule()) {
				continue;
			}
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AdresseBuilder.newAdresse(a, serviceInfra));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<AdresseAutreTiers> coreToWebAT(List<AdresseGenerique> adresses,
	                                                  @Nullable DateRangeHelper.Range range,
	                                                  ServiceInfrastructureService serviceInfra) throws WebServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<AdresseAutreTiers> list = new ArrayList<AdresseAutreTiers>();
		for (AdresseGenerique a : adresses) {
			if (a.isAnnule()) {
				continue;
			}
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AdresseBuilder.newAdresseAutreTiers(a, serviceInfra));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<TiersCriteria> webToCore(SearchTiersRequest criteria) {
		if (criteria == null) {
			return null;
		}

		final List<TiersCriteria> list = new ArrayList<TiersCriteria>();

		if (criteria.getNumero() == null || criteria.getNumero().length() == 0) {
			/*
			 * Si le numéro est nul, on fait une recherche normale
			 */
			final TiersCriteria coreCriteria = new TiersCriteria();
			coreCriteria.setNumero(null);

			final TiersCriteria.TypeRecherche type = EnumHelper.webToCore(criteria.getTypeRecherche());
			coreCriteria.setTypeRechercheDuNom(type == null ? TiersCriteria.TypeRecherche.CONTIENT : type);

			coreCriteria.setLocaliteOuPays(criteria.getLocaliteOuPays());
			coreCriteria.setNomRaison(criteria.getNomCourrier());
			coreCriteria.setNumeroAVS(criteria.getNumeroAVS());
			coreCriteria.setDateNaissance(DataHelper.webToCore(criteria.getDateNaissance()));
			if (criteria.getNoOfsFor() != null) {
				coreCriteria.setNoOfsFor(criteria.getNoOfsFor().toString());
			}
			if (criteria.isForPrincipalActif() != null) {
				coreCriteria.setForPrincipalActif(criteria.isForPrincipalActif());
			}
			if (criteria.getTypeTiers() != null) {
				coreCriteria.setTypesTiers(webToCore(criteria.getTypeTiers()));
			}
			if (criteria.getCategorieDebiteur() != null) {
				coreCriteria.setCategorieDebiteurIs(CategorieImpotSource.valueOf(criteria.getCategorieDebiteur().name()));
			}

			coreCriteria.setTiersActif(criteria.isTiersActif());

			list.add(coreCriteria);
		}
		else {
			/*
			 * Dans le cas d'une recherche sur le numéro, on accepte plusieurs numéros séparés par des "+"
			 */
			final String[] numeros = criteria.getNumero().split("\\+");
			for (String numero : numeros) {

				final Long no;
				try {
					no = Long.valueOf(numero.trim());
				}
				catch (NumberFormatException ignored) {
					/*
					 * si le numéro ne peut pas être interpreté comme un long, on a de toutes façons aucune chance de le trouver dans le
					 * base
					 */
					continue;
				}

				final TiersCriteria coreCriteria = new TiersCriteria();
				coreCriteria.setNumero(no);
				list.add(coreCriteria);
			}
		}

		return list;
	}

	public static Set<TiersCriteria.TypeTiers> webToCore(List<TypeTiers> typeTiers) {
		Set<TiersCriteria.TypeTiers> set = new HashSet<TiersCriteria.TypeTiers>();
		for (TypeTiers t : typeTiers) {
			switch (t) {
			case DEBITEUR:
				set.add(TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
				break;
			case MENAGE_COMMUN:
				set.add(TiersCriteria.TypeTiers.MENAGE_COMMUN);
				break;
			case PERSONNE_MORALE:
				set.add(TiersCriteria.TypeTiers.ENTREPRISE);
				break;
			case PERSONNE_PHYSIQUE:
				set.add(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
				break;
			default:
				throw new IllegalArgumentException("Type de tiers inconnu = [" + typeTiers + "]");
			}
		}
		return set;
	}

	public static TiersInfo coreToWeb(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final TiersInfo i = new TiersInfo();
		i.setNumero(value.getNumero());
		i.setNom1(value.getNom1());
		i.setNom2(value.getNom2());
		i.setRue(value.getRue());
		i.setNpa(value.getNpa());
		i.setLocalite(value.getLocalite());
		i.setPays(value.getPays());
		i.setDateNaissance(DataHelper.coreToWeb(value.getRegDateNaissance()));
		i.setType(DataHelper.getTypeTiers(value));
		return i;
	}

	/**
	 * Retourne le numéro de la déclaration d'impôt associée avec une période d'imposition.
	 *
	 * @param periodeImposition la période d'imposition considérée
	 * @return l'id de déclaration associée; ou <b>null</b> si aucune déclaration n'est émise.
	 */
	public static Long getAssociatedDi(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periodeImposition) {

		final Contribuable contribuable = periodeImposition.getContribuable();
		final List<ch.vd.uniregctb.declaration.Declaration> dis = contribuable.getDeclarationsForPeriode(periodeImposition.getDateDebut()
				.year());
		if (dis == null) {
			return null;
		}

		Long idDi = null;

		for (ch.vd.uniregctb.declaration.Declaration di : dis) {
			if (!di.isAnnule() && DateRangeHelper.intersect(periodeImposition, di)) {
				if (idDi != null) {
					final String erreur = String.format("Inhérence des données: trouvé deux déclarations (ids %d et %d) "
							+ "associées avec la période d'imposition du %s au %s sur le contribuable n°%d", idDi, di.getId(),
							periodeImposition.getDateDebut().toString(), periodeImposition.getDateFin().toString(), contribuable
							.getNumero());
					throw new ValidationException(contribuable, erreur);
				}
				idDi = di.getId();
			}
		}

		return idDi;
	}

	/**
	 * Détermine le type d'un tiers à partir de son instance concrète.
	 *
	 * @param tiers l'instance concrète du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static TypeTiers getType(final ch.vd.uniregctb.tiers.Tiers tiers) {
		final TypeTiers type;
		if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
			type = TypeTiers.PERSONNE_PHYSIQUE;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
			type = TypeTiers.MENAGE_COMMUN;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
			type = TypeTiers.DEBITEUR;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise || tiers instanceof ch.vd.uniregctb.tiers.Etablissement
				|| tiers instanceof ch.vd.uniregctb.tiers.AutreCommunaute
				|| tiers instanceof ch.vd.uniregctb.tiers.CollectiviteAdministrative) {
			type = TypeTiers.PERSONNE_MORALE;
		}
		else {
			type = null;
		}
		return type;
	}

	private static final Map<String, TypeTiers> indexedData2Type = new HashMap<String, TypeTiers>() {
		private static final long serialVersionUID = -6977238534201838137L;

		{
			put(HabitantIndexable.SUB_TYPE, TypeTiers.PERSONNE_PHYSIQUE);
			put(NonHabitantIndexable.SUB_TYPE, TypeTiers.PERSONNE_PHYSIQUE);
			put(EntrepriseIndexable.SUB_TYPE, TypeTiers.PERSONNE_MORALE);
			put(MenageCommunIndexable.SUB_TYPE, TypeTiers.MENAGE_COMMUN);
			put(AutreCommunauteIndexable.SUB_TYPE, TypeTiers.PERSONNE_MORALE);
			put(EntrepriseIndexable.SUB_TYPE, TypeTiers.PERSONNE_MORALE);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, TypeTiers.DEBITEUR);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static TypeTiers getTypeTiers(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2Type.get(typeAsString);
	}

	public static Set<Parts> webToCore(Set<TiersPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<Parts> results = new HashSet<Parts>(parts.size());
		for (TiersPart p : parts) {
			switch (p) {
			case ADRESSES:
			case ADRESSES_ENVOI:
				results.add(Parts.ADRESSES);
				results.add(Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case DECLARATIONS:
				results.add(Parts.DECLARATIONS);
				break;
			case FORS_FISCAUX:
			case FORS_FISCAUX_VIRTUELS:
			case FORS_GESTION:
			case ASSUJETTISSEMENTS:
			case PERIODES_ASSUJETTISSEMENT:
			case PERIODES_IMPOSITION:
				results.add(Parts.FORS_FISCAUX);
				break;
			case RAPPORTS_ENTRE_TIERS:
			case COMPOSANTS_MENAGE:
				results.add(Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case SITUATIONS_FAMILLE:
				results.add(Parts.SITUATIONS_FAMILLE);
				break;
			case PERIODICITES:
				results.add(Parts.PERIODICITES);
				break;
			case COMPTES_BANCAIRES:
			case CAPITAUX:
			case ETATS_PM:
			case FORMES_JURIDIQUES:
			case REGIMES_FISCAUX:
			case SIEGES:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + "]");
			}
		}

		return results;
	}

	@SuppressWarnings("unchecked")
	public static List<ForFiscalPrincipal> getForsFiscauxVirtuels(ch.vd.uniregctb.tiers.Tiers tiers, TiersDAO tiersDAO) {

		// Récupère les appartenances ménages du tiers
		final Set<ch.vd.uniregctb.tiers.RapportEntreTiers> rapports = tiers.getRapportsSujet();
		final Collection<AppartenanceMenage> rapportsMenage = CollectionUtils.select(rapports, new Predicate() {
			public boolean evaluate(Object object) {
				final ch.vd.uniregctb.tiers.RapportEntreTiers rapport = (ch.vd.uniregctb.tiers.RapportEntreTiers) object;
				return !rapport.isAnnule() && rapport instanceof AppartenanceMenage;
			}
		});

		if (rapportsMenage.isEmpty()) {
			return Collections.emptyList();
		}

		final List<ForFiscalPrincipal> forsVirtuels = new ArrayList<ForFiscalPrincipal>();

		// Extrait les fors principaux du ménage, en les adaptant à la période de validité des appartenances ménages
		for (AppartenanceMenage a : rapportsMenage) {
			final Long menageId = a.getObjetId();
			final List<ForFiscalPrincipal> forsMenage =
					tiersDAO.getHibernateTemplate().find("from ForFiscalPrincipal f where f.annulationDate is null and f.tiers.id = ? order by f.dateDebut asc", menageId);

			final List<ForFiscalPrincipal> extraction = DateRangeHelper.extract(forsMenage, a.getDateDebut(), a.getDateFin(),
					new DateRangeHelper.AdapterCallback<ForFiscalPrincipal>() {
						public ForFiscalPrincipal adapt(ForFiscalPrincipal f, RegDate debut, RegDate fin) {
							if (debut == null && fin == null) {
								return f;
							}
							else {
								ForFiscalPrincipal clone = (ForFiscalPrincipal) f.duplicate();
								clone.setDateDebut(debut);
								clone.setDateFin(fin);
								return clone;
							}
						}
					});

			forsVirtuels.addAll(extraction);
		}

		return forsVirtuels;
	}

	public static Date coreToWeb(String s) {
		return coreToWeb(RegDateHelper.dashStringToDate(s));
	}

	public static AdresseEnvoi createAdresseFormattee(ch.vd.uniregctb.tiers.Tiers tiers, @Nullable RegDate date, Context context, TypeAdresseFiscale type) throws AdresseException {
		final AdresseEnvoiDetaillee adressePoursuite = context.adresseService.getAdresseEnvoi(tiers, date, type, false);
		if (adressePoursuite == null) {
			return null;
		}
		return AdresseBuilder.newAdresseEnvoi(adressePoursuite);
	}

	public static AdresseEnvoiAutreTiers createAdresseFormatteeAT(ch.vd.uniregctb.tiers.Tiers tiers, @Nullable RegDate date, Context context, TypeAdresseFiscale type) throws AdresseException {
		final AdresseEnvoiDetaillee adressePoursuite = context.adresseService.getAdresseEnvoi(tiers, date, type, false);
		if (adressePoursuite == null) {
			return null;
		}
		return AdresseBuilder.newAdresseEnvoiAutreTiers(adressePoursuite);
	}

	public static Set<TiersPart> toSet(List<TiersPart> parts) {
		return new HashSet<TiersPart>(parts);
	}
}
