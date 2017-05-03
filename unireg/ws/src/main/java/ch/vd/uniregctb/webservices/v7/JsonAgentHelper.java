package ch.vd.uniregctb.webservices.v7;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import ch.vd.unireg.xml.party.agent.v1.Agent;
import ch.vd.unireg.xml.party.agent.v1.AgentType;
import ch.vd.unireg.xml.party.agent.v1.GeneralAgent;
import ch.vd.unireg.xml.party.agent.v1.SpecialAgent;

/**
 * Classes et méthodes utiles pour la manipulation du type polymorphique {@link Agent} avec JSON
 */
public abstract class JsonAgentHelper {

	/**
	 * Interface pour définir l'attribut "type" en JSON
	 */
	public interface JsonAgent {

		@JsonProperty(value = "type")
		AgentType getType();
	}

	/**
	 * Interface des <i>builders</i> d'équivalents JSON aux sous-classes connues de {@link Agent}
	 * @param <S> classe source
	 */
	private interface JsonAgentBuilder<S extends Agent> {
		Agent buildJsonEquivalent(S src);
	}

	/**
	 * Mandat général
	 */
	public static class JsonGeneralAgent extends GeneralAgent implements JsonAgent {

		private JsonGeneralAgent(GeneralAgent source) {
			super(source.getDateFrom(), source.getDateTo(), source.getPostAddress(), source.isWithCopy(), source.getContactPerson(), source.getContactPhoneNumber(), source.getPadding(), source.getAny());
		}

		@Override
		public AgentType getType() {
			return AgentType.GENERAL;
		}

		public static final class Builder implements JsonAgentBuilder<GeneralAgent> {
			@Override
			public Agent buildJsonEquivalent(GeneralAgent src) {
				return new JsonGeneralAgent(src);
			}
		}
	}

	/**
	 * Mandat spécial
	 */
	public static class JsonSpecialAgent extends SpecialAgent implements JsonAgent {

		private JsonSpecialAgent(SpecialAgent source) {
			super(source.getDateFrom(), source.getDateTo(), source.getPostAddress(), source.isWithCopy(), source.getTaxKind(), source.getContactPerson(), source.getContactPhoneNumber(), source.getPadding(), source.getAny());
		}

		@Override
		public AgentType getType() {
			return AgentType.SPECIAL;
		}

		public static final class Builder implements JsonAgentBuilder<SpecialAgent> {
			@Override
			public Agent buildJsonEquivalent(SpecialAgent src) {
				return new JsonSpecialAgent(src);
			}
		}
	}

	private static final Map<Class<? extends Agent>, JsonAgentBuilder<? extends Agent>> BUILDERS = buildBuilders();

	private static <T extends Agent> void registerBuilder(Map<Class<? extends Agent>, JsonAgentBuilder<? extends Agent>> map,
	                                                      Class<T> clazz, JsonAgentBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends Agent>, JsonAgentBuilder<? extends Agent>> buildBuilders() {
		final Map<Class<? extends Agent>, JsonAgentBuilder<? extends Agent>> map = new HashMap<>();
		registerBuilder(map, GeneralAgent.class, new JsonGeneralAgent.Builder());
		registerBuilder(map, SpecialAgent.class, new JsonSpecialAgent.Builder());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Agent> Agent jsonEquivalentOf(Agent source) {
		if (source == null) {
			return null;
		}
		if (source instanceof JsonAgent) {
			return source;
		}
		final JsonAgentBuilder<T> builder = (JsonAgentBuilder<T>) BUILDERS.get(source.getClass());
		return builder.buildJsonEquivalent((T) source);
	}

}
