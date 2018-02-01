package ch.vd.unireg.webservices.v7;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.PartnershipForm;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationType;
import ch.vd.unireg.xml.party.taxdeclaration.v5.WithholdingTaxDeclaration;

/**
 * Classes et méthodes utiles pour la manipulation du type polymorphique {@link TaxDeclaration} avec JSON
 */
public abstract class JsonTaxDeclarationHelper {

	/**
	 * Interface pour définir l'attribut "type" en JSON
	 */
	public interface JsonTaxDeclaration {

		@JsonProperty(value = "type")
		TaxDeclarationType getType();
	}

	/**
	 * Interface des <i>builders</i> d'équivalents JSON aux sous-classes connues de {@link TaxDeclaration}
	 * @param <S> classe source
	 */
	private interface JsonTaxDeclarationBuilder<S extends TaxDeclaration> {
		TaxDeclaration buildJsonEquivalent(S src);
	}

	/**
	 * Déclaration d'impôt ordinaire
	 */
	private static class JsonOrdinaryTaxDeclaration extends OrdinaryTaxDeclaration implements JsonTaxDeclaration {

		private JsonOrdinaryTaxDeclaration(OrdinaryTaxDeclaration src) {
			super(src.getId(), src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getTaxPeriod(), src.getStatuses(), src.getDeadlines(),
			      src.getSequenceNumber(), src.getDocumentType(), src.getManagingMunicipalityFSOId(), src.getSegmentationCode(), src.getPadding(), src.getAny());
		}

		@Override
		public TaxDeclarationType getType() {
			return TaxDeclarationType.ORDINARY_TAX_DECLARATION;
		}

		public static final class Builder implements JsonTaxDeclarationBuilder<OrdinaryTaxDeclaration> {
			@Override
			public TaxDeclaration buildJsonEquivalent(OrdinaryTaxDeclaration src) {
				return new JsonTaxDeclarationHelper.JsonOrdinaryTaxDeclaration(src);
			}
		}
	}

	/**
	 * Liste récapitulative IS
	 */
	private static class JsonWithholdingTaxDeclaration extends WithholdingTaxDeclaration implements JsonTaxDeclaration {

		private JsonWithholdingTaxDeclaration(WithholdingTaxDeclaration src) {
			super(src.getId(), src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getTaxPeriod(), src.getStatuses(), src.getDeadlines(),
			      src.getPeriodicity(), src.getCommunicationMode(), src.getAny());
		}

		@Override
		public TaxDeclarationType getType() {
			return TaxDeclarationType.WITHHOLDING_TAX_DECLARATION;
		}

		public static final class Builder implements JsonTaxDeclarationBuilder<WithholdingTaxDeclaration> {
			@Override
			public TaxDeclaration buildJsonEquivalent(WithholdingTaxDeclaration src) {
				return new JsonTaxDeclarationHelper.JsonWithholdingTaxDeclaration(src);
			}
		}
	}

	/**
	 * Questionnaire SNC
	 */
	private static class JsonPartnershipForm extends PartnershipForm implements JsonTaxDeclaration {

		private JsonPartnershipForm(PartnershipForm src) {
			super(src.getId(), src.getDateFrom(), src.getDateTo(), src.getCancellationDate(), src.getTaxPeriod(), src.getStatuses(), src.getDeadlines(),
			      src.getPadding(), src.getAny());
		}

		@Override
		public TaxDeclarationType getType() {
			return TaxDeclarationType.PARTNERSHIP_FORM;
		}

		public static final class Builder implements JsonTaxDeclarationBuilder<PartnershipForm> {
			@Override
			public TaxDeclaration buildJsonEquivalent(PartnershipForm src) {
				return new JsonTaxDeclarationHelper.JsonPartnershipForm(src);
			}
		}
	}

	private static final Map<Class<? extends TaxDeclaration>, JsonTaxDeclarationBuilder<? extends TaxDeclaration>> BUILDERS = buildBuilders();

	private static <T extends TaxDeclaration> void registerBuilder(Map<Class<? extends TaxDeclaration>, JsonTaxDeclarationBuilder<? extends TaxDeclaration>> map,
	                                                               Class<T> clazz, JsonTaxDeclarationBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends TaxDeclaration>, JsonTaxDeclarationBuilder<? extends TaxDeclaration>> buildBuilders() {
		final Map<Class<? extends TaxDeclaration>, JsonTaxDeclarationBuilder<? extends TaxDeclaration>> map = new HashMap<>();
		registerBuilder(map, OrdinaryTaxDeclaration.class, new JsonOrdinaryTaxDeclaration.Builder());
		registerBuilder(map, WithholdingTaxDeclaration.class, new JsonWithholdingTaxDeclaration.Builder());
		registerBuilder(map, PartnershipForm.class, new JsonPartnershipForm.Builder());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T extends TaxDeclaration> TaxDeclaration jsonEquivalentOf(TaxDeclaration source) {
		if (source == null) {
			return null;
		}
		if (source instanceof JsonTaxDeclaration) {
			return source;
		}
		final JsonTaxDeclarationBuilder<T> builder = (JsonTaxDeclarationBuilder<T>) BUILDERS.get(source.getClass());
		return builder.buildJsonEquivalent((T) source);
	}
}
