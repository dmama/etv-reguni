package ch.vd.unireg.webservices.v7;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRightType;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;

/**
 * Classes et méthodes utiles pour la manipulation du type polymorphique {@link LandRight} avec JSON
 */
public abstract class JsonLandRightHelper {

	/**
	 * Interface pour définir l'attribut "rightType" en JSON
	 */
	public interface JsonLandRight {
		@JsonProperty(value = "rightType")
		LandRightType getJsonType();
	}

	/**
	 * Interface des <i>builders</i> d'équivalents JSON aux sous-classes connues de {@link LandRight}
	 * @param <S> classe source
	 */
	private interface JsonLandRightBuilder<S extends LandRight> {
		LandRight buildJsonEquivalent(S src);
	}

	/**
	 * Droit de propriété
	 */
	public static class JsonLandOwnershipRight extends LandOwnershipRight implements JsonLandRight {

		private JsonLandOwnershipRight(LandOwnershipRight source) {
			super(source.getDateFrom(), source.getDateTo(), source.getStartReason(), source.getEndReason(), source.getCaseIdentifier(), source.getRightHolder(),
			      source.getImmovablePropertyId(), source.getShare(), source.getType(), source.getCommunityId(), source.getAcquisitionReasons(),
			      source.getPadding(), source.getId(), source.getDateInheritedTo(), source.getPadding2(), source.getAny());
		}

		@Override
		public LandRightType getJsonType() {
			return LandRightType.OWNERSHIP;
		}

		public static final class Builder implements JsonLandRightBuilder<LandOwnershipRight> {
			@Override
			public LandOwnershipRight buildJsonEquivalent(LandOwnershipRight src) {
				return new JsonLandOwnershipRight(src);
			}
		}
	}

	/**
	 * Usufruit
	 */
	public static class JsonUsufructRight extends UsufructRight implements JsonLandRight {

		private JsonUsufructRight(UsufructRight source) {
			super(source.getDateFrom(), source.getDateTo(), source.getStartReason(), source.getEndReason(), source.getCaseIdentifier(), source.getRightHolder(),
			      source.getImmovablePropertyId(), source.getRightHolders(), source.getImmovablePropertyIds(), source.getPadding(), source.getId(),
			      source.getDateInheritedTo(), source.getPadding2(), source.getMemberships(), source.getEncumbrances(), source.getPadding3(), source.getAny());
		}

		@Override
		public LandRightType getJsonType() {
			return LandRightType.USUFRUCT;
		}

		public static final class Builder implements JsonLandRightBuilder<UsufructRight> {
			@Override
			public UsufructRight buildJsonEquivalent(UsufructRight src) {
				return new JsonUsufructRight(src);
			}
		}
	}

	/**
	 * Droit d'habitation
	 */
	public static class JsonHousingRight extends HousingRight implements JsonLandRight {

		private JsonHousingRight(HousingRight source) {
			super(source.getDateFrom(), source.getDateTo(), source.getStartReason(), source.getEndReason(), source.getCaseIdentifier(), source.getRightHolder(),
			      source.getImmovablePropertyId(), source.getRightHolders(), source.getImmovablePropertyIds(), source.getPadding(), source.getId(),
			      source.getDateInheritedTo(), source.getPadding2(), source.getMemberships(), source.getEncumbrances(), source.getPadding3(), source.getAny());
		}

		@Override
		public LandRightType getJsonType() {
			return LandRightType.HOUSING;
		}

		public static final class Builder implements JsonLandRightBuilder<HousingRight> {
			@Override
			public HousingRight buildJsonEquivalent(HousingRight src) {
				return new JsonHousingRight(src);
			}
		}
	}

	private static final Map<Class<? extends LandRight>, JsonLandRightBuilder<? extends LandRight>> BUILDERS = buildBuilders();

	private static <T extends LandRight> void registerBuilder(Map<Class<? extends LandRight>, JsonLandRightBuilder<? extends LandRight>> map,
	                                                      Class<T> clazz, JsonLandRightBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends LandRight>, JsonLandRightBuilder<? extends LandRight>> buildBuilders() {
		final Map<Class<? extends LandRight>, JsonLandRightBuilder<? extends LandRight>> map = new HashMap<>();
		registerBuilder(map, LandOwnershipRight.class, new JsonLandOwnershipRight.Builder());
		registerBuilder(map, UsufructRight.class, new JsonUsufructRight.Builder());
		registerBuilder(map, HousingRight.class, new JsonHousingRight.Builder());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T extends LandRight> LandRight jsonEquivalentOf(LandRight source) {
		if (source == null) {
			return null;
		}
		if (source instanceof JsonLandRight) {
			return source;
		}
		final JsonLandRightBuilder<T> builder = (JsonLandRightBuilder<T>) BUILDERS.get(source.getClass());
		return builder.buildJsonEquivalent((T) source);
	}

}
