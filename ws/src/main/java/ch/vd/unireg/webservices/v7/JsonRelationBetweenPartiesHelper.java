package ch.vd.uniregctb.webservices.v7;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import ch.vd.unireg.xml.party.relation.v4.Absorbed;
import ch.vd.unireg.xml.party.relation.v4.Absorbing;
import ch.vd.unireg.xml.party.relation.v4.Administration;
import ch.vd.unireg.xml.party.relation.v4.AfterSplit;
import ch.vd.unireg.xml.party.relation.v4.BeforeSplit;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.EconomicActivity;
import ch.vd.unireg.xml.party.relation.v4.Guardian;
import ch.vd.unireg.xml.party.relation.v4.HouseholdMember;
import ch.vd.unireg.xml.party.relation.v4.InheritanceFrom;
import ch.vd.unireg.xml.party.relation.v4.InheritanceTo;
import ch.vd.unireg.xml.party.relation.v4.LegalAdviser;
import ch.vd.unireg.xml.party.relation.v4.ManagementCompany;
import ch.vd.unireg.xml.party.relation.v4.Parent;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.relation.v4.Replaced;
import ch.vd.unireg.xml.party.relation.v4.ReplacedBy;
import ch.vd.unireg.xml.party.relation.v4.Representative;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstitute;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstituteFor;
import ch.vd.unireg.xml.party.relation.v4.TaxableRevenue;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferOriginator;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferRecipient;
import ch.vd.unireg.xml.party.relation.v4.WelfareAdvocate;
import ch.vd.unireg.xml.party.relation.v4.WithholdingTaxContact;

/**
 * Classes et méthodes utiles pour la manipulation du type polymorphique {@link RelationBetweenParties} avec JSON
 */
public abstract class JsonRelationBetweenPartiesHelper {

	/**
	 * Interface pour définir l'attribut "type" en JSON
	 */
	public interface JsonRelationBetweenParties {

		@JsonProperty(value = "type")
		RelationBetweenPartiesType getType();
	}

	/**
	 * Interface des <i>builders</i> d'équivalents JSON aux sous-classes connues de {@link RelationBetweenParties}
	 * @param <S> classe source
	 */
	private interface JsonRelationBetweenPartiesBuilder<S extends RelationBetweenParties> {
		RelationBetweenParties buildJsonEquivalent(S src);
	}

	/**
	 * Entreprise absorbée
	 */
	public static class JsonAbsorbed extends Absorbed implements JsonRelationBetweenParties {

		private JsonAbsorbed(Absorbed source) {
			super(source.getDateFrom(), source.getDateTo(), source.getCancellationDate(), source.getOtherPartyNumber(), source.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.ABSORBED;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Absorbed> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Absorbed src) {
				return new JsonAbsorbed(src);
			}
		}
	}

	/**
	 * Entreprise absorbante
	 */
	public static class JsonAbsorbing extends Absorbing implements JsonRelationBetweenParties {

		private JsonAbsorbing(Absorbing source) {
			super(source.getDateFrom(), source.getDateTo(), source.getCancellationDate(), source.getOtherPartyNumber(), source.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.ABSORBING;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Absorbing> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Absorbing src) {
				return new JsonAbsorbing(src);
			}
		}
	}

	/**
	 * Entreprise après scission
	 */
	public static class JsonAfterSplit extends AfterSplit implements JsonRelationBetweenParties {

		private JsonAfterSplit(AfterSplit source) {
			super(source.getDateFrom(), source.getDateTo(), source.getCancellationDate(), source.getOtherPartyNumber(), source.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.AFTER_SPLIT;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<AfterSplit> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(AfterSplit src) {
				return new JsonAfterSplit(src);
			}
		}
	}

	/**
	 * Entreprise avant scission
	 */
	public static class JsonBeforeSplit extends BeforeSplit implements JsonRelationBetweenParties {

		private JsonBeforeSplit(BeforeSplit source) {
			super(source.getDateFrom(), source.getDateTo(), source.getCancellationDate(), source.getOtherPartyNumber(), source.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.BEFORE_SPLIT;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<BeforeSplit> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(BeforeSplit src) {
				return new JsonBeforeSplit(src);
			}
		}
	}

	/**
	 * Annule et remplace (vers le remplacé)
	 */
	public static class JsonReplaced extends Replaced implements JsonRelationBetweenParties {

		private JsonReplaced(Replaced src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.REPLACED;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Replaced> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Replaced src) {
				return new JsonReplaced(src);
			}
		}
	}

	/**
	 * Annule et remplace (vers le remplaçant)
	 */
	public static class JsonReplacedBy extends ReplacedBy implements JsonRelationBetweenParties {

		private JsonReplacedBy(ReplacedBy src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.REPLACED_BY;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<ReplacedBy> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(ReplacedBy src) {
				return new JsonReplacedBy(src);
			}
		}
	}

	/**
	 * Enfant
	 */
	public static class JsonChild extends Child implements JsonRelationBetweenParties {

		private JsonChild(Child src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.CHILD;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Child> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Child src) {
				return new JsonChild(src);
			}
		}
	}

	/**
	 * Activité économique
	 */
	public static class JsonEconomicActivity extends EconomicActivity implements JsonRelationBetweenParties {

		private JsonEconomicActivity(EconomicActivity src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.isPrincipal(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.ECONOMIC_ACTIVITY;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<EconomicActivity> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(EconomicActivity src) {
				return new JsonEconomicActivity(src);
			}
		}
	}

	/**
	 * Tuteur
	 */
	public static class JsonGuardian extends Guardian implements JsonRelationBetweenParties {

		private JsonGuardian(Guardian src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.GUARDIAN;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Guardian> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Guardian src) {
				return new JsonGuardian(src);
			}
		}
	}

	/**
	 * Appartenance ménage
	 */
	public static class JsonHouseholdMember extends HouseholdMember implements JsonRelationBetweenParties {

		private JsonHouseholdMember(HouseholdMember src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.HOUSEHOLD_MEMBER;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<HouseholdMember> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(HouseholdMember src) {
				return new JsonHouseholdMember(src);
			}
		}
	}

	/**
	 * Conseiller légal
	 */
	public static class JsonLegalAdviser extends LegalAdviser implements JsonRelationBetweenParties {

		private JsonLegalAdviser(LegalAdviser src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.LEGAL_ADVISER;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<LegalAdviser> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(LegalAdviser src) {
				return new JsonLegalAdviser(src);
			}
		}
	}

	/**
	 * Parent
	 */
	public static class JsonParent extends Parent implements JsonRelationBetweenParties {

		private JsonParent(Parent src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.PARENT;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Parent> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Parent src) {
				return new JsonParent(src);
			}
		}
	}

	/**
	 * Représentant conventionnel
	 */
	public static class JsonRepresentative extends Representative implements JsonRelationBetweenParties {

		private JsonRepresentative(Representative src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.isExtensionToForcedExecution(), src.getPadding(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.REPRESENTATIVE;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Representative> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Representative src) {
				return new JsonRepresentative(src);
			}
		}
	}

	/**
	 * Prestation imposable
	 */
	public static class JsonTaxableRevenue extends TaxableRevenue implements JsonRelationBetweenParties {

		private JsonTaxableRevenue(TaxableRevenue src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getEndDateOfLastTaxableItem(), src.getPadding(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.TAXABLE_REVENUE;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<TaxableRevenue> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(TaxableRevenue src) {
				return new JsonTaxableRevenue(src);
			}
		}
	}

	/**
	 * Source de tranfert de patrimoine
	 */
	public static class JsonWealthTransferOriginator extends WealthTransferOriginator implements JsonRelationBetweenParties {

		private JsonWealthTransferOriginator(WealthTransferOriginator src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.WEALTH_TRANSFER_ORIGINATOR;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<WealthTransferOriginator> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(WealthTransferOriginator src) {
				return new JsonWealthTransferOriginator(src);
			}
		}
	}

	/**
	 * Destination de transfert de patrimoine
	 */
	public static class JsonWealthTransferRecipient extends WealthTransferRecipient implements JsonRelationBetweenParties {

		private JsonWealthTransferRecipient(WealthTransferRecipient src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.WEALTH_TRANSFER_RECIPIENT;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<WealthTransferRecipient> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(WealthTransferRecipient src) {
				return new JsonWealthTransferRecipient(src);
			}
		}
	}

	/**
	 * Curateur
	 */
	public static class JsonWelfareAdvocate extends WelfareAdvocate implements JsonRelationBetweenParties {

		private JsonWelfareAdvocate(WelfareAdvocate src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.WELFARE_ADVOCATE;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<WelfareAdvocate> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(WelfareAdvocate src) {
				return new JsonWelfareAdvocate(src);
			}
		}
	}

	/**
	 * Contact impôt source
	 */
	public static class JsonWithholdingTaxContact extends WithholdingTaxContact implements JsonRelationBetweenParties {

		private JsonWithholdingTaxContact(WithholdingTaxContact src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<WithholdingTaxContact> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(WithholdingTaxContact src) {
				return new JsonWithholdingTaxContact(src);
			}
		}
	}

	/**
	 * Administration d'entreprise
	 */
	public static class JsonAdministration extends Administration implements JsonRelationBetweenParties {

		private JsonAdministration(Administration src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.isChairman(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.ADMINISTRATION;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<Administration> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(Administration src) {
				return new JsonAdministration(src);
			}
		}
	}

	/**
	 * Société de direction (fonds d'investissement -> société de direction)
	 */
	public static class JsonManagementCompany extends ManagementCompany implements JsonRelationBetweenParties {

		private JsonManagementCompany(ManagementCompany src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.MANAGEMENT_COMPANY;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<ManagementCompany> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(ManagementCompany src) {
				return new JsonManagementCompany(src);
			}
		}
	}

	/**
	 * Assujettissement par substitution (substitué -> substituant)
	 */
	public static class JsonTaxLiabilitySubstitute extends TaxLiabilitySubstitute implements JsonRelationBetweenParties {

		private JsonTaxLiabilitySubstitute(TaxLiabilitySubstitute src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<TaxLiabilitySubstitute> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(TaxLiabilitySubstitute src) {
				return new JsonTaxLiabilitySubstitute(src);
			}
		}
	}

	/**
	 * Assujettissement par substitution (substituant -> substitué)
	 */
	public static class JsonTaxLiabilitySubstituteFor extends TaxLiabilitySubstituteFor implements JsonRelationBetweenParties {

		private JsonTaxLiabilitySubstituteFor(TaxLiabilitySubstituteFor src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE_FOR;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<TaxLiabilitySubstituteFor> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(TaxLiabilitySubstituteFor src) {
				return new JsonTaxLiabilitySubstituteFor(src);
			}
		}
	}

	/**
	 * Relation d'héritage décédé -> héritier
	 */
	public static class JsonInheritanceTo extends InheritanceTo implements JsonRelationBetweenParties {

		private JsonInheritanceTo(InheritanceTo src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.isPrincipal(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.INHERITANCE_TO;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<InheritanceTo> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(InheritanceTo src) {
				return new JsonInheritanceTo(src);
			}
		}
	}

	/**
	 * Relation d'héritage héritier -> décédé
	 */
	public static class JsonInheritanceFrom extends InheritanceFrom implements JsonRelationBetweenParties {

		private JsonInheritanceFrom(InheritanceFrom src) {
			super(src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getOtherPartyNumber(), src.isPrincipal(), src.getAny());
		}

		@Override
		public RelationBetweenPartiesType getType() {
			return RelationBetweenPartiesType.INHERITANCE_TO;
		}

		public static final class Builder implements JsonRelationBetweenPartiesBuilder<InheritanceFrom> {
			@Override
			public RelationBetweenParties buildJsonEquivalent(InheritanceFrom src) {
				return new JsonInheritanceFrom(src);
			}
		}
	}

	private static final Map<Class<? extends RelationBetweenParties>, JsonRelationBetweenPartiesBuilder<? extends RelationBetweenParties>> BUILDERS = buildBuilders();

	private static <T extends RelationBetweenParties> void registerBuilder(Map<Class<? extends RelationBetweenParties>, JsonRelationBetweenPartiesBuilder<? extends RelationBetweenParties>> map,
	                                                                       Class<T> clazz, JsonRelationBetweenPartiesBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends RelationBetweenParties>, JsonRelationBetweenPartiesBuilder<? extends RelationBetweenParties>> buildBuilders() {
		final Map<Class<? extends RelationBetweenParties>, JsonRelationBetweenPartiesBuilder<? extends RelationBetweenParties>> map = new HashMap<>();
		registerBuilder(map, Absorbed.class, new JsonAbsorbed.Builder());
		registerBuilder(map, Absorbing.class, new JsonAbsorbing.Builder());
		registerBuilder(map, AfterSplit.class, new JsonAfterSplit.Builder());
		registerBuilder(map, BeforeSplit.class, new JsonBeforeSplit.Builder());
		registerBuilder(map, Replaced.class, new JsonReplaced.Builder());
		registerBuilder(map, ReplacedBy.class, new JsonReplacedBy.Builder());
		registerBuilder(map, Child.class, new JsonChild.Builder());
		registerBuilder(map, EconomicActivity.class, new JsonEconomicActivity.Builder());
		registerBuilder(map, Guardian.class, new JsonGuardian.Builder());
		registerBuilder(map, HouseholdMember.class, new JsonHouseholdMember.Builder());
		registerBuilder(map, LegalAdviser.class, new JsonLegalAdviser.Builder());
		registerBuilder(map, Parent.class, new JsonParent.Builder());
		registerBuilder(map, Representative.class, new JsonRepresentative.Builder());
		registerBuilder(map, TaxableRevenue.class, new JsonTaxableRevenue.Builder());
		registerBuilder(map, WealthTransferOriginator.class, new JsonWealthTransferOriginator.Builder());
		registerBuilder(map, WealthTransferRecipient.class, new JsonWealthTransferRecipient.Builder());
		registerBuilder(map, WelfareAdvocate.class, new JsonWelfareAdvocate.Builder());
		registerBuilder(map, WithholdingTaxContact.class, new JsonWithholdingTaxContact.Builder());
		registerBuilder(map, Administration.class, new JsonAdministration.Builder());
		registerBuilder(map, ManagementCompany.class, new JsonManagementCompany.Builder());
		registerBuilder(map, TaxLiabilitySubstitute.class, new JsonTaxLiabilitySubstitute.Builder());
		registerBuilder(map, TaxLiabilitySubstituteFor.class, new JsonTaxLiabilitySubstituteFor.Builder());
		registerBuilder(map, InheritanceTo.class, new JsonInheritanceTo.Builder());
		registerBuilder(map, InheritanceFrom.class, new JsonInheritanceFrom.Builder());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T extends RelationBetweenParties> RelationBetweenParties jsonEquivalentOf(RelationBetweenParties source) {
		if (source == null) {
			return null;
		}
		if (source instanceof JsonRelationBetweenParties) {
			return source;
		}
		final JsonRelationBetweenPartiesBuilder<T> builder = (JsonRelationBetweenPartiesBuilder<T>) BUILDERS.get(source.getClass());
		return builder.buildJsonEquivalent((T) source);
	}
}
