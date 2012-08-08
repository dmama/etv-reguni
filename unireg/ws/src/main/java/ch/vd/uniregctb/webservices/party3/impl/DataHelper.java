package ch.vd.uniregctb.webservices.party3.impl;

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

import ch.ech.ech0044.v2.DatePartiallyKnown;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.corporation.v1.CorporationEvent;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.EntrepriseIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.xml.address.AddressBuilder;

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

	public static List<Address> coreToWeb(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws WebServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<Address> list = new ArrayList<Address>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<AddressOtherParty> coreToWebAT(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws WebServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<AddressOtherParty> list = new ArrayList<AddressOtherParty>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newOtherPartyAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<TiersCriteria> webToCore(SearchPartyRequest criteria) {
		if (criteria == null) {
			return null;
		}

		final List<TiersCriteria> list = new ArrayList<TiersCriteria>();

		if (criteria.getNumber() == null || criteria.getNumber().length() == 0) {
			/*
			 * Si le numéro est nul, on fait une recherche normale
			 */
			final TiersCriteria coreCriteria = new TiersCriteria();
			coreCriteria.setNumero(null);

			final TiersCriteria.TypeRecherche type = EnumHelper.webToCore(criteria.getSearchMode());
			coreCriteria.setTypeRechercheDuNom(type == null ? TiersCriteria.TypeRecherche.CONTIENT : type);

			coreCriteria.setLocaliteOuPays(criteria.getTownOrCountry());
			coreCriteria.setNomRaison(criteria.getContactName());
			coreCriteria.setNumeroAVS(criteria.getSocialInsuranceNumber());
			coreCriteria.setDateNaissance(DataHelper.webToCore(criteria.getDateOfBirth()));
			if (criteria.getTaxResidenceFSOId() != null) {
				coreCriteria.setNoOfsFor(criteria.getTaxResidenceFSOId().toString());
			}
			if (criteria.isActiveMainTaxResidence() != null) {
				coreCriteria.setForPrincipalActif(criteria.isActiveMainTaxResidence());
			}
			if (criteria.getPartyTypes() != null) {
				coreCriteria.setTypesTiers(webToCore(criteria.getPartyTypes()));
			}
			coreCriteria.setCategorieDebiteurIs(EnumHelper.webToCore(criteria.getDebtorCategory()));

			coreCriteria.setTiersActif(criteria.isActiveParty());

			if (criteria.getOldWithholdingNumber() != null) { // [SIFISC-5846]
				coreCriteria.setAncienNumeroSourcier(criteria.getOldWithholdingNumber().longValue());
			}

			list.add(coreCriteria);
		}
		else {
			/*
			 * Dans le cas d'une recherche sur le numéro, on accepte plusieurs numéros séparés par des "+"
			 */
			final String[] numeros = criteria.getNumber().split("\\+");
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

	public static Set<TiersCriteria.TypeTiers> webToCore(List<PartyType> typeTiers) {
		Set<TiersCriteria.TypeTiers> set = new HashSet<TiersCriteria.TypeTiers>();
		for (PartyType t : typeTiers) {
			set.add(EnumHelper.webToCore(t));
		}
		return set;
	}

	public static PartyInfo coreToWeb(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final PartyInfo i = new PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToWeb(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyType(value));
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
				.year(), false);
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
	public static PartyType getPartyType(final ch.vd.uniregctb.tiers.Tiers tiers) {
		final PartyType type;
		if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
			type = PartyType.NATURAL_PERSON;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
			type = PartyType.HOUSEHOLD;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
			type = PartyType.DEBTOR;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise || tiers instanceof ch.vd.uniregctb.tiers.Etablissement
				|| tiers instanceof ch.vd.uniregctb.tiers.AutreCommunaute
				|| tiers instanceof ch.vd.uniregctb.tiers.CollectiviteAdministrative) {
			type = PartyType.CORPORATION;
		}
		else {
			type = null;
		}
		return type;
	}

	private static final Map<String, PartyType> indexedData2Type = new HashMap<String, PartyType>() {
		private static final long serialVersionUID = -6977238534201838137L;

		{
			put(HabitantIndexable.SUB_TYPE, PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(EntrepriseIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, PartyType.DEBTOR);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static PartyType getPartyType(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2Type.get(typeAsString);
	}

	public static Set<Parts> webToCore(Set<PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<Parts> results = new HashSet<Parts>(parts.size());
		for (PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(Parts.ADRESSES);
				results.add(Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
				results.add(Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(Parts.FORS_FISCAUX);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
				results.add(Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(Parts.IMMEUBLES);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case CORPORATION_STATUSES:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
			case CHILDREN:
			case PARENTS:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static Set<Parts> xmlToCore(Set<ch.vd.unireg.xml.party.v1.PartyPart> parts) {
		return ch.vd.uniregctb.xml.DataHelper.xmlToCore(parts);
	}

	@SuppressWarnings("unchecked")
	public static List<ForFiscalPrincipal> getForsFiscauxVirtuels(ch.vd.uniregctb.tiers.Tiers tiers, TiersDAO tiersDAO) {

		// Récupère les appartenances ménages du tiers
		final Set<ch.vd.uniregctb.tiers.RapportEntreTiers> rapports = tiers.getRapportsSujet();
		final Collection<AppartenanceMenage> rapportsMenage = CollectionUtils.select(rapports, new Predicate() {
			@Override
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
						@Override
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

	public static Set<ch.vd.unireg.xml.party.v1.PartyPart> webToXML(Collection<PartyPart> parts) {
		final HashSet<ch.vd.unireg.xml.party.v1.PartyPart> set = new HashSet<ch.vd.unireg.xml.party.v1.PartyPart>();
		for (PartyPart part : parts) {
			set.add(webToXML(part));
		}
		return set;
	}

	public static ch.vd.unireg.xml.party.v1.PartyPart webToXML(PartyPart part) {
		if (part == null) {
			return null;
		}
		switch (part) {
		case ADDRESSES:
			return ch.vd.unireg.xml.party.v1.PartyPart.ADDRESSES;
		case BANK_ACCOUNTS:
			return ch.vd.unireg.xml.party.v1.PartyPart.BANK_ACCOUNTS;
		case CAPITALS:
			return ch.vd.unireg.xml.party.v1.PartyPart.CAPITALS;
		case CHILDREN:
			return ch.vd.unireg.xml.party.v1.PartyPart.CHILDREN;
		case CORPORATION_STATUSES:
			return ch.vd.unireg.xml.party.v1.PartyPart.CORPORATION_STATUSES;
		case DEBTOR_PERIODICITIES:
			return ch.vd.unireg.xml.party.v1.PartyPart.DEBTOR_PERIODICITIES;
		case FAMILY_STATUSES:
			return ch.vd.unireg.xml.party.v1.PartyPart.FAMILY_STATUSES;
		case HOUSEHOLD_MEMBERS:
			return ch.vd.unireg.xml.party.v1.PartyPart.HOUSEHOLD_MEMBERS;
		case IMMOVABLE_PROPERTIES:
			return ch.vd.unireg.xml.party.v1.PartyPart.IMMOVABLE_PROPERTIES;
		case LEGAL_FORMS:
			return ch.vd.unireg.xml.party.v1.PartyPart.LEGAL_FORMS;
		case LEGAL_SEATS:
			return ch.vd.unireg.xml.party.v1.PartyPart.LEGAL_SEATS;
		case MANAGING_TAX_RESIDENCES:
			return ch.vd.unireg.xml.party.v1.PartyPart.MANAGING_TAX_RESIDENCES;
		case PARENTS:
			return ch.vd.unireg.xml.party.v1.PartyPart.PARENTS;
		case RELATIONS_BETWEEN_PARTIES:
			return ch.vd.unireg.xml.party.v1.PartyPart.RELATIONS_BETWEEN_PARTIES;
		case SIMPLIFIED_TAX_LIABILITIES:
			return ch.vd.unireg.xml.party.v1.PartyPart.SIMPLIFIED_TAX_LIABILITIES;
		case TAX_DECLARATIONS:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_DECLARATIONS;
		case TAX_DECLARATIONS_STATUSES:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_DECLARATIONS_STATUSES;
		case TAX_LIABILITIES:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_LIABILITIES;
		case TAX_RESIDENCES:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_RESIDENCES;
		case TAX_SYSTEMS:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_SYSTEMS;
		case TAXATION_PERIODS:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAXATION_PERIODS;
		case VIRTUAL_TAX_RESIDENCES:
			return ch.vd.unireg.xml.party.v1.PartyPart.VIRTUAL_TAX_RESIDENCES;
		default:
			throw new IllegalArgumentException("Type de part inconnue = [" + part + "]");
		}
	}

	public static DatePartiallyKnown clone(DatePartiallyKnown right) {
		if (right == null) {
			return null;
		}
		return new DatePartiallyKnown(right.getYearMonthDay(), right.getYearMonth(), right.getYear());
	}

	public static SearchCorporationEventsResponse events2web(List<ch.vd.uniregctb.interfaces.model.EvenementPM> events) {
		if (events == null || events.isEmpty()) {
			return null;
		}
		final SearchCorporationEventsResponse response = new SearchCorporationEventsResponse();
		for (ch.vd.uniregctb.interfaces.model.EvenementPM e : events) {
			CorporationEvent event = new CorporationEvent();
			event.setPartyNumber(e.getNumeroPM().intValue());
			event.setDate(coreToWeb(e.getDate()));
			event.setCode(e.getCode());
			response.getEvents().add(event);
		}
		return response;
	}
}
