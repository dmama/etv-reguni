package ch.vd.unireg.xml.party.v5;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * [SIFISC-28888] Représentation interne des parts possibles sur un tiers. Cet enum est une vue <i>à plat</i> de l'enum {@link PartyPart} avec la correspondance suivante :
 *
 * <pre>
 *  PartyPart                         | -                                   |  VIRTUAL_LAND_RIGHTS                    |
 * -----------------------------------+-------------------------------------+-----------------------------------------+
 *   -                                | -                                   |  REAL_LAND_RIGHTS                       |
 *                                    |                                     |  VIRTUAL_TRANSITIVE_LAND_RIGHTS         |
 * -----------------------------------+-------------------------------------+-----------------------------------------+
 *   VIRTUAL_INHERITANCE_LAND_RIGHTS  | REAL_LAND_RIGHTS                    |  REAL_LAND_RIGHTS                       |
 *                                    | VIRTUAL_INHERITED_REAL_LAND_RIGHTS  |  VIRTUAL_TRANSITIVE_LAND_RIGHTS         |
 *                                    |                                     |  VIRTUAL_INHERITED_REAL_LAND_RIGHTS     |
 *                                    |                                     |  VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS  |
 * -----------------------------------+-------------------------------------+-----------------------------------------+
 * </pre>
 */
public enum InternalPartyPart {
	ADDRESSES,
	TAX_RESIDENCES,
	VIRTUAL_TAX_RESIDENCES,
	MANAGING_TAX_RESIDENCES,
	HOUSEHOLD_MEMBERS,
	TAX_LIABILITIES,
	SIMPLIFIED_TAX_LIABILITIES,
	TAXATION_PERIODS,
	WITHHOLDING_TAXATION_PERIODS,
	RELATIONS_BETWEEN_PARTIES,
	FAMILY_STATUSES,
	TAX_DECLARATIONS,
	TAX_DECLARATIONS_STATUSES,
	TAX_DECLARATIONS_DEADLINES,
	BANK_ACCOUNTS,
	LEGAL_SEATS,
	LEGAL_FORMS,
	CAPITALS,
	TAX_SYSTEMS,
	TAX_LIGHTENINGS,
	BUSINESS_YEARS,
	CORPORATION_STATUSES,
	CORPORATION_FLAGS,
	DEBTOR_PERIODICITIES,
	IMMOVABLE_PROPERTIES,
	CHILDREN,
	PARENTS,
	EBILLING_STATUSES,
	AGENTS,
	LABELS,
	/**
	 * Les droits réels du tiers.
	 */
	REAL_LAND_RIGHTS,
	/**
	 * Les droits virtuels qui correspondent aux droits d'immeubles sur d'autres immeubles.
	 */
	VIRTUAL_TRANSITIVE_LAND_RIGHTS,
	/**
	 * Les droits virtuels qui correspondent aux droits <i>réels</i> hérités d'un défunt.
	 */
	VIRTUAL_INHERITED_REAL_LAND_RIGHTS,
	/**
	 * Les droits virtuels qui correspondent aux droits <i>virtuels</i> hérités d'un défunt.
	 */
	VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS,
	RESIDENCY_PERIODS,
	LAND_TAX_LIGHTENINGS,
	INHERITANCE_RELATIONSHIPS;

	@Nullable
	public static InternalPartyPart fromPartyV5(@NotNull ch.vd.unireg.xml.party.v5.PartyPart part) {
		switch (part) {
		case ADDRESSES:
			return ADDRESSES;
		case TAX_RESIDENCES:
			return TAX_RESIDENCES;
		case VIRTUAL_TAX_RESIDENCES:
			return VIRTUAL_TAX_RESIDENCES;
		case MANAGING_TAX_RESIDENCES:
			return MANAGING_TAX_RESIDENCES;
		case HOUSEHOLD_MEMBERS:
			return HOUSEHOLD_MEMBERS;
		case TAX_LIABILITIES:
			return TAX_LIABILITIES;
		case SIMPLIFIED_TAX_LIABILITIES:
			return SIMPLIFIED_TAX_LIABILITIES;
		case TAXATION_PERIODS:
			return TAXATION_PERIODS;
		case WITHHOLDING_TAXATION_PERIODS:
			return WITHHOLDING_TAXATION_PERIODS;
		case RELATIONS_BETWEEN_PARTIES:
			return RELATIONS_BETWEEN_PARTIES;
		case FAMILY_STATUSES:
			return FAMILY_STATUSES;
		case TAX_DECLARATIONS:
			return TAX_DECLARATIONS;
		case TAX_DECLARATIONS_STATUSES:
			return TAX_DECLARATIONS_STATUSES;
		case TAX_DECLARATIONS_DEADLINES:
			return TAX_DECLARATIONS_DEADLINES;
		case BANK_ACCOUNTS:
			return BANK_ACCOUNTS;
		case LEGAL_SEATS:
			return LEGAL_SEATS;
		case LEGAL_FORMS:
			return LEGAL_FORMS;
		case CAPITALS:
			return CAPITALS;
		case TAX_SYSTEMS:
			return TAX_SYSTEMS;
		case TAX_LIGHTENINGS:
			return TAX_LIGHTENINGS;
		case BUSINESS_YEARS:
			return BUSINESS_YEARS;
		case CORPORATION_STATUSES:
			return CORPORATION_STATUSES;
		case CORPORATION_FLAGS:
			return CORPORATION_FLAGS;
		case DEBTOR_PERIODICITIES:
			return DEBTOR_PERIODICITIES;
		case IMMOVABLE_PROPERTIES:
			return IMMOVABLE_PROPERTIES;
		case CHILDREN:
			return CHILDREN;
		case PARENTS:
			return PARENTS;
		case EBILLING_STATUSES:
			return EBILLING_STATUSES;
		case AGENTS:
			return AGENTS;
		case LABELS:
			return LABELS;
		case LAND_RIGHTS:
			// [SIFISC-28888] doit être interprété en combinaison avec VIRTUAL_LAND_RIGHTS et VIRTUAL_INHERITANCE_LAND_RIGHTS
			return null;
		case VIRTUAL_LAND_RIGHTS:
			// [SIFISC-28888] doit être interprété en combinaison avec REAL_LAND_RIGHTS et VIRTUAL_INHERITANCE_LAND_RIGHTS
			return null;
		case VIRTUAL_INHERITANCE_LAND_RIGHTS:
			// [SIFISC-28888] doit être interprété en combinaison avec REAL_LAND_RIGHTS et VIRTUAL_LAND_RIGHTS
			return null;
		case RESIDENCY_PERIODS:
			return RESIDENCY_PERIODS;
		case LAND_TAX_LIGHTENINGS:
			return LAND_TAX_LIGHTENINGS;
		case INHERITANCE_RELATIONSHIPS:
			return INHERITANCE_RELATIONSHIPS;
		default:
			throw new IllegalArgumentException("Part inconnue = [" + part + "]");
		}
	}
}
