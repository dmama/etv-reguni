package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

/**
 * Container d'extraction et cache de données (non-nulles !!!) depuis une entité RegPM
 * (il faut d'abord enregistrer les extracteurs - voir {@link #registerDataExtractor(Class, Function, Function, Function)} -
 * avant d'utiliser la méthode {@link #getExtractedData(Class, EntityKey)}
 */
public class ExtractedDataCache {

	/**
	 * Données maintenues pour les enregistrements de structures de données à extraire (une et une seule fois par entité)
	 * @see #registerDataExtractor(Class, Function, Function, Function)
	 */
	private final Map<Class<?>, DataExtractionRegistration<?>> dataExtractionRegistrations = new HashMap<>();

	/**
	 * Données extraites
	 * @see #getExtractedData(Class, EntityKey)
	 */
	private final Map<Pair<EntityKey, Class<?>>, Object> dataExtractionCache = new HashMap<>();

	/**
	 * Graphe des données en cours de migration
	 */
	private final Graphe graphe;

	/**
	 * @param graphe graphe des données en cours de migration
	 */
	public ExtractedDataCache(Graphe graphe) {
		this.graphe = graphe;
	}

	/**
	 * Enregistre une méthode d'extraction de données depuis les données RegPM (l'idée est de ne la calculer qu'une seule fois,
	 * ces extracteurs ne seront appelés qu'une seule fois par instance de graphe) utilisable ensuite au travers de la méthode
	 * {@link MigrationResultProduction#getExtractedData(Class, EntityKey)}
	 * @param dataClass classe discriminante pour la donnée à extraire (une donnée par classe et entité)
	 * @param entrepriseExtractor l'extracteur à utiliser si cette données est extraite d'une entreprise
	 * @param etablissementExtractor l'extracteur à utiliser si cette données est extraite d'un établissement
	 * @param individuExtractor l'extracteur à utiliser si cette données est extraite d'un individu
	 * @param <D> le type de la donnée extraite
	 */
	public <D> void registerDataExtractor(Class<D> dataClass,
	                                      @Nullable Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor,
	                                      @Nullable Function<? super RegpmEtablissement, ? extends D> etablissementExtractor,
	                                      @Nullable Function<? super RegpmIndividu, ? extends D> individuExtractor) {
		if (dataExtractionRegistrations.containsKey(dataClass)) {
			throw new IllegalStateException("Classe " + dataClass.getName() + " déjà utilisée!");
		}
		dataExtractionRegistrations.put(dataClass, new DataExtractionRegistration<>(entrepriseExtractor, etablissementExtractor, individuExtractor));
	}

	/**
	 * Récupère la donnée préalablement enregistrée lors d'un précédent appel pour la même classe et la même entité. Si aucune donnée
	 * n'a été préalablement enregistrée (= premier appel), alors l'extracteur correspondant (préalablement enregistré par un
	 * appel à {@link MigrationResultInitialization#registerDataExtractor(Class, Function, Function, Function)}) est sollicité
	 * @param clazz classe discriminante pour la donnée à extraire (une donnée par classe et entité)
	 * @param key clé de l'entité concernée par la donnée à extraire (une donnée par classe et entité)
	 * @param <D> le type de la donnée extraite
	 * @return la donnée extraite
	 */
	@NotNull
	public <D> D getExtractedData(Class<D> clazz, EntityKey key) {
		//noinspection unchecked
		final DataExtractionRegistration<D> registration = (DataExtractionRegistration<D>) dataExtractionRegistrations.get(clazz);
		if (registration == null) {
			throw new IllegalArgumentException("Aucun extracteur n'a été enregistré pour la classe " + clazz.getName());
		}

		switch (key.getType()) {
		case ENTREPRISE:
			return getExtractedData(key, clazz, () -> graphe.getEntreprises().get(key.getId()), registration.entrepriseExtractor);
		case ETABLISSEMENT:
			return getExtractedData(key, clazz, () -> graphe.getEtablissements().get(key.getId()), registration.etablissementExtractor);
		case INDIVIDU:
			return getExtractedData(key, clazz, () -> graphe.getIndividus().get(key.getId()), registration.individuExtractor);
		default:
			throw new IllegalArgumentException("Invalid key type : " + key.getType());
		}
	}

	private static final class DataExtractionRegistration<D> {
		final Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor;
		final Function<? super RegpmEtablissement, ? extends D> etablissementExtractor;
		final Function<? super RegpmIndividu, ? extends D> individuExtractor;

		public DataExtractionRegistration(Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor,
		                                  Function<? super RegpmEtablissement, ? extends D> etablissementExtractor,
		                                  Function<? super RegpmIndividu, ? extends D> individuExtractor) {
			this.entrepriseExtractor = entrepriseExtractor;
			this.etablissementExtractor = etablissementExtractor;
			this.individuExtractor = individuExtractor;
		}
	}

	private <S extends RegpmEntity & WithLongId, D> D getExtractedData(EntityKey key, Class<D> clazz, Supplier<S> entity, @Nullable Function<? super S, ? extends D> extractor) {
		final Pair<EntityKey, Class<?>> cacheKey = Pair.of(key, clazz);

		// la valeur a-t-elle déjà été extraite ?
		//noinspection unchecked
		final D cachedValue = (D) dataExtractionCache.get(cacheKey);
		if (cachedValue != null) {
			//noinspection unchecked
			return cachedValue;
		}

		// non, c'est la première fois que cette valeur est demandée -> il faut donc la calculer

		// mais s'il n'y a pas d'extracteur... c'est la fin (mauvaise utilisation...)
		if (extractor == null) {
			throw new IllegalStateException("Pas d'extracteur enregistré pour la classe " + clazz.getName() + " et le type d'entité " + key.getType());
		}

		// calcul et sauvegarde pour la prochaine fois
		final D data = extractor.apply(entity.get());
		if (data == null) {
			throw new NullPointerException("Les extracteurs de données cachées ne doivent jamais retourner null... (extracteur " + extractor + " avec entité " + key + ")");
		}

		dataExtractionCache.put(cacheKey, data);
		return data;
	}
}
