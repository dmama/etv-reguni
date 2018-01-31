package ch.vd.uniregctb.webservices.party3.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'web'.
 */
public class DataHelper {

	//private static final Logger LOGGER = LoggerFactory.getLogger(DataHelper.class);

	public static List<TiersCriteria> webToCore(SearchPartyRequest criteria) {
		if (criteria == null) {
			return null;
		}

		final List<TiersCriteria> list = new ArrayList<>();

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
			coreCriteria.setDateNaissanceInscriptionRC(ch.vd.uniregctb.xml.DataHelper.xmlToCore(criteria.getDateOfBirth()));
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
		final Set<TiersCriteria.TypeTiers> set = EnumSet.noneOf(TiersCriteria.TypeTiers.class);
		for (PartyType t : typeTiers) {
			set.add(EnumHelper.webToCore(t));
		}
		return set;
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
		else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise
				|| tiers instanceof ch.vd.uniregctb.tiers.AutreCommunaute
				|| tiers instanceof ch.vd.uniregctb.tiers.CollectiviteAdministrative) {
			type = PartyType.CORPORATION;
		}
		else {
			type = null;
		}
		return type;
	}

	public static Set<Parts> webToCore(Set<PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<Parts> results = new HashSet<>(parts.size());
		for (PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(Parts.ADRESSES);
				results.add(Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
			case TAX_DECLARATIONS_DEADLINES:
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
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
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

	public static Set<ch.vd.unireg.xml.party.v1.PartyPart> webToXML(Collection<PartyPart> parts) {
		final Set<ch.vd.unireg.xml.party.v1.PartyPart> set = EnumSet.noneOf(ch.vd.unireg.xml.party.v1.PartyPart.class);
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
		case TAX_DECLARATIONS_DEADLINES:
			return ch.vd.unireg.xml.party.v1.PartyPart.TAX_DECLARATIONS_DEADLINES;
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
}
