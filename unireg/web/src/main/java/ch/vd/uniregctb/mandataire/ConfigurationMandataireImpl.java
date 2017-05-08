package ch.vd.uniregctb.mandataire;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Configuration basée sur les propriétés de configuration présentes par exemple dans le fichier unireg.properties
 * <ul>
 *     <li>dont le nom commence par un préfixe donné (exemble <code>extprop.onglet.mandataires</code>)</li>
 *     <li>dont le nom, après le préfixe, est construit comme suit ". &lt;type de mandat&gt; . &lt;type de tiers&gt; ", où
 *      <ul>
 *          <li>type de mandat est l'une des modalités de l'énum {@link TypeMandat}</li>
 *          <li>type de tiers est l'une des modalités de l'énum {@link TypeTiers}</li>
 *      </ul>
 *     </li>
 *     <li>pour les mandats {@link TypeMandat#SPECIAL spéciaux}, on ajoute encore ".&lt;genre d'impôt&gt;", où le genre d'impôt est le code issu de la liste de FiDoR</li>
 *     <li>et dont les valeurs peuvent être :
 *      <ul>
 *          <li>{@link ConfigurationMandataire.Acces#AUCUN AUCUN} (ou vide) pour signaler une absence d'affichage&nbsp;;</li>
 *          <li>{@link ConfigurationMandataire.Acces#VISUALISATION_SEULE VISUALISATION_SEULE} pour signaler un affichage en lecture seule uniquement&nbsp;;</li>
 *          <li>{@link ConfigurationMandataire.Acces#EDITION_POSSIBLE EDITION_POSSIBLE} pour signaler une un affichage potentiellement en modification (moyennant les droits applicatifs corrects, bien-évidemment).</li>
 *      </ul>
 *     </li>
 * </ul>
 */
public class ConfigurationMandataireImpl implements ConfigurationMandataire, InitializingBean {

	private UniregProperties properties;
	private String propertyNamePrefix;
	private boolean creationRapportEntreTiersAutoriseePourMandatsCourrier;

	private final Map<TypeTiers, Acces> mandatGeneral = new EnumMap<>(TypeTiers.class);
	private final Map<TypeTiers, Acces> mandatPerception = new EnumMap<>(TypeTiers.class);
	private final Map<TypeTiersAvecGenreImpot, Acces> mandatSpecial = new HashMap<>();

	public void setProperties(UniregProperties properties) {
		this.properties = properties;
	}

	public void setPropertyNamePrefix(String propertyNamePrefix) {
		this.propertyNamePrefix = propertyNamePrefix;
	}

	public void setCreationRapportEntreTiersAutoriseePourMandatsCourrier(boolean creationRapportEntreTiersAutoriseePourMandatsCourrier) {
		this.creationRapportEntreTiersAutoriseePourMandatsCourrier = creationRapportEntreTiersAutoriseePourMandatsCourrier;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.isBlank(propertyNamePrefix)) {
			throw new IllegalArgumentException("Un préfixe pour les propriétés liées à la configuration mandataire est nécessaire...");
		}

		initConfigurationData();
	}

	private static Map<String, String> extractPrefixedSubmap(Map<String, String> source, String keyPrefix) {
		return source.entrySet().stream()
				.filter(entry -> entry.getKey() != null && entry.getKey().startsWith(keyPrefix))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map<String, String> extractPrefixedSubmap() {
		return extractPrefixedSubmap(properties.getAllProperties(), propertyNamePrefix);
	}

	private String buildKey(TypeMandat typeMandat, TypeTiers typeTiers) {
		return String.format("%s.%s.%s", propertyNamePrefix, typeMandat, typeTiers);
	}

	private static final class TypeTiersAvecGenreImpot {
		private final TypeTiers typeTiers;
		private final String genreImpot;

		public TypeTiersAvecGenreImpot(TypeTiers typeTiers, String genreImpot) {
			this.typeTiers = typeTiers;
			this.genreImpot = genreImpot;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final TypeTiersAvecGenreImpot that = (TypeTiersAvecGenreImpot) o;
			return typeTiers == that.typeTiers && (genreImpot != null ? genreImpot.equals(that.genreImpot) : that.genreImpot == null);
		}

		@Override
		public int hashCode() {
			int result = typeTiers != null ? typeTiers.hashCode() : 0;
			result = 31 * result + (genreImpot != null ? genreImpot.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "TypeTiersAvecGenreImpot{" +
					"typeTiers=" + typeTiers +
					", genreImpot='" + genreImpot + '\'' +
					'}';
		}
	}

	private void initConfigurationData() {
		final Map<String, String> all = extractPrefixedSubmap();

		// récupération de l'accès configuré pour les mandats généraux : <prefixe>.GENERAL.<typeTiers>
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final String key = buildKey(TypeMandat.GENERAL, typeTiers);
			final String value = all.get(key);
			final Acces acces = StringUtils.isBlank(value) ? Acces.AUCUN : Acces.valueOf(value);
			mandatGeneral.put(typeTiers, acces);
		}

		// récupération de l'accès configuré pour les mandats tiers (= perception) : <prefixe>.TIERS.<typeTiers>
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final String key = buildKey(TypeMandat.TIERS, typeTiers);
			final String value = all.get(key);
			final Acces acces = StringUtils.isBlank(value) ? Acces.AUCUN : Acces.valueOf(value);
			mandatPerception.put(typeTiers, acces);
		}

		// et aussi pour les mandats spéciaux : <prefixe>.SPECIAL.<typeTiers>.<genreImpot>
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final String keyPrefix = buildKey(TypeMandat.SPECIAL, typeTiers);
			final Map<String, String> mapTypeTiers = extractPrefixedSubmap(all, keyPrefix);
			for (Map.Entry<String, String> entry : mapTypeTiers.entrySet()) {
				final String genreImpot = entry.getKey().substring(keyPrefix.length() + 1);     // +1 pour le point séparateur
				final Acces acces = StringUtils.isBlank(entry.getValue()) ? Acces.AUCUN : Acces.valueOf(entry.getValue());
				mandatSpecial.put(new TypeTiersAvecGenreImpot(typeTiers, genreImpot), acces);
			}
		}
	}

	@Override
	public Acces getAffichageMandatGeneral(@NotNull Tiers tiers) {
		return mandatGeneral.get(tiers.getType());
	}

	@Override
	public Acces getAffichageMandatTiers(@NotNull Tiers tiers) {
		return mandatPerception.get(tiers.getType());
	}

	@Override
	public Acces getAffichageMandatSpecial(@NotNull Tiers tiers, @NotNull GenreImpotMandataire genreImpotMandataire) {
		final TypeTiersAvecGenreImpot key = new TypeTiersAvecGenreImpot(tiers.getType(), genreImpotMandataire.getCode());
		final Acces acces = mandatSpecial.get(key);
		return acces != null ? acces : Acces.AUCUN;
	}

	@Override
	public boolean isCreationRapportEntreTiersAutoriseePourMandatsCourrier() {
		return creationRapportEntreTiersAutoriseePourMandatsCourrier;
	}
}
