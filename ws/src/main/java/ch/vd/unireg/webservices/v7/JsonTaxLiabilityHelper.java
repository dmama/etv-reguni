package ch.vd.unireg.webservices.v7;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import ch.vd.unireg.xml.party.taxresidence.v4.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v4.ForeignCountry;
import ch.vd.unireg.xml.party.taxresidence.v4.Indigent;
import ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v4.MixedWithholding137Par1;
import ch.vd.unireg.xml.party.taxresidence.v4.MixedWithholding137Par2;
import ch.vd.unireg.xml.party.taxresidence.v4.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v4.OtherCanton;
import ch.vd.unireg.xml.party.taxresidence.v4.PureWithholding;
import ch.vd.unireg.xml.party.taxresidence.v4.SwissDiplomat;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiability;

/**
 * Classes et méthodes utiles pour la manipulation du type polymorphique {@link TaxLiability} avec JSON
 */
public abstract class JsonTaxLiabilityHelper {

	/**
	 * Interface pour définir l'attribut "type" en JSON
	 */
	public interface JsonTaxLiability {

		@JsonProperty(value = "type")
		IndividualTaxLiabilityType getType();
	}

	/**
	 * Interface des <i>builders</i> d'équivalents JSON aux sous-classes connues de {@link TaxLiability}
	 * @param <S> classe source
	 */
	private interface JsonTaxLiabilityBuilder<S extends TaxLiability> {
		TaxLiability buildJsonEquivalent(S src);
	}

	/**
	 * Dépense
	 */
	private static class JsonExpenditureBased extends ExpenditureBased implements JsonTaxLiability {

		private JsonExpenditureBased(ExpenditureBased src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.EXPENDITURE_BASED;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<ExpenditureBased> {
			@Override
			public JsonTaxLiabilityHelper.JsonExpenditureBased buildJsonEquivalent(ExpenditureBased src) {
				return new JsonTaxLiabilityHelper.JsonExpenditureBased(src);
			}
		}
	}

	/**
	 * Hors-Suisse
	 */
	private static class JsonForeignCountry extends ForeignCountry implements JsonTaxLiability {

		private JsonForeignCountry(ForeignCountry src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.FOREIGN_COUNTRY;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<ForeignCountry> {
			@Override
			public JsonTaxLiabilityHelper.JsonForeignCountry buildJsonEquivalent(ForeignCountry src) {
				return new JsonTaxLiabilityHelper.JsonForeignCountry(src);
			}
		}
	}

	/**
	 * Hors-Canton
	 */
	private static class JsonOtherCanton extends OtherCanton implements JsonTaxLiability {

		private JsonOtherCanton(OtherCanton src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.OTHER_CANTON;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<OtherCanton> {
			@Override
			public JsonTaxLiabilityHelper.JsonOtherCanton buildJsonEquivalent(OtherCanton src) {
				return new JsonTaxLiabilityHelper.JsonOtherCanton(src);
			}
		}
	}

	/**
	 * Vaudois ordinaire
	 */
	private static class JsonOrdinaryResident extends OrdinaryResident implements JsonTaxLiability {

		private JsonOrdinaryResident(OrdinaryResident src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.ORDINARY_RESIDENT;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<OrdinaryResident> {
			@Override
			public JsonTaxLiabilityHelper.JsonOrdinaryResident buildJsonEquivalent(OrdinaryResident src) {
				return new JsonTaxLiabilityHelper.JsonOrdinaryResident(src);
			}
		}
	}

	/**
	 * Sourcier pur
	 */
	private static class JsonPureWithholding extends PureWithholding implements JsonTaxLiability {

		private JsonPureWithholding(PureWithholding src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getTaxationAuthority(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.PURE_WITHHOLDING;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<PureWithholding> {
			@Override
			public JsonTaxLiabilityHelper.JsonPureWithholding buildJsonEquivalent(PureWithholding src) {
				return new JsonTaxLiabilityHelper.JsonPureWithholding(src);
			}
		}
	}

	/**
	 * Sourcier mixte 2
	 */
	private static class JsonMixedWithholdingArt137Par2 extends MixedWithholding137Par2 implements JsonTaxLiability {

		private JsonMixedWithholdingArt137Par2(MixedWithholding137Par2 src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getTaxationAuthority(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<MixedWithholding137Par2> {
			@Override
			public JsonTaxLiabilityHelper.JsonMixedWithholdingArt137Par2 buildJsonEquivalent(MixedWithholding137Par2 src) {
				return new JsonTaxLiabilityHelper.JsonMixedWithholdingArt137Par2(src);
			}
		}
	}

	/**
	 * Sourcier mixte 1
	 */
	private static class JsonMixedWithholdingArt137Par1 extends MixedWithholding137Par1 implements JsonTaxLiability {

		private JsonMixedWithholdingArt137Par1(MixedWithholding137Par1 src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getTaxationAuthority(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<MixedWithholding137Par1> {
			@Override
			public JsonTaxLiabilityHelper.JsonMixedWithholdingArt137Par1 buildJsonEquivalent(MixedWithholding137Par1 src) {
				return new JsonTaxLiabilityHelper.JsonMixedWithholdingArt137Par1(src);
			}
		}
	}

	/**
	 * Diplomate suisse
	 */
	private static class JsonSwissDiplomat extends SwissDiplomat implements JsonTaxLiability {

		private JsonSwissDiplomat(SwissDiplomat src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.SWISS_DIPLOMAT;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<SwissDiplomat> {
			@Override
			public JsonTaxLiabilityHelper.JsonSwissDiplomat buildJsonEquivalent(SwissDiplomat src) {
				return new JsonTaxLiabilityHelper.JsonSwissDiplomat(src);
			}
		}
	}

	/**
	 * Indigent
	 */
	private static class JsonIndigent extends Indigent implements JsonTaxLiability {

		private JsonIndigent(Indigent src) {
			super(src.getStartReason(), src.getEndReason(), src.getDateTo(), src.getDateFrom(), src.getAny());
		}

		@Override
		public final IndividualTaxLiabilityType getType() {
			return IndividualTaxLiabilityType.INDIGENT;
		}

		public static final class Builder implements JsonTaxLiabilityBuilder<Indigent> {
			@Override
			public JsonTaxLiabilityHelper.JsonIndigent buildJsonEquivalent(Indigent src) {
				return new JsonTaxLiabilityHelper.JsonIndigent(src);
			}
		}
	}

	private static final Map<Class<? extends TaxLiability>, JsonTaxLiabilityBuilder<? extends TaxLiability>> BUILDERS = buildBuilders();

	private static <T extends TaxLiability>	void registerBuilder(Map<Class<? extends TaxLiability>, JsonTaxLiabilityBuilder<? extends TaxLiability>> map,
	                                                             Class<T> clazz, JsonTaxLiabilityBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends TaxLiability>, JsonTaxLiabilityBuilder<? extends TaxLiability>> buildBuilders() {
		final Map<Class<? extends TaxLiability>, JsonTaxLiabilityBuilder<? extends TaxLiability>> map = new HashMap<>();
		registerBuilder(map, ExpenditureBased.class, new JsonExpenditureBased.Builder());
		registerBuilder(map, ForeignCountry.class, new JsonForeignCountry.Builder());
		registerBuilder(map, OtherCanton.class, new JsonOtherCanton.Builder());
		registerBuilder(map, OrdinaryResident.class, new JsonOrdinaryResident.Builder());
		registerBuilder(map, PureWithholding.class, new JsonPureWithholding.Builder());
		registerBuilder(map, MixedWithholding137Par2.class, new JsonMixedWithholdingArt137Par2.Builder());
		registerBuilder(map, MixedWithholding137Par1.class, new JsonMixedWithholdingArt137Par1.Builder());
		registerBuilder(map, SwissDiplomat.class, new JsonSwissDiplomat.Builder());
		registerBuilder(map, Indigent.class, new JsonIndigent.Builder());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T extends TaxLiability> TaxLiability jsonEquivalentOf(TaxLiability source) {
		if (source == null) {
			return null;
		}
		if (source instanceof JsonTaxLiability) {
			return source;
		}
		final JsonTaxLiabilityBuilder<T> builder = (JsonTaxLiabilityBuilder<T>) BUILDERS.get(source.getClass());
		return builder.buildJsonEquivalent((T) source);
	}
}
