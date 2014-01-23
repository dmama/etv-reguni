package ch.vd.uniregctb.cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.StringRenderer;

/**
 * Ensemble de méthodes disponibles pour la gestion des données en cache
 */
public abstract class CacheHelper {

	private static final String NULL = "null";
	private static final String LI = " * ";
	private static final String ARROW = " --> ";

	/**
	 * Classe qui est capable de fournir des {@link StringRenderer} pour plusieurs classes distinctes préalablement enregistrées
	 */
	public static final class ValueRendererFactory {

		public static final StringRenderer<Object> DEFAULT_RENDERER = new StringRenderer<Object>() {
			@Override
			public String toString(Object object) {
				return String.format("%s", object);
			}
		};

		private final Map<Class<?>, StringRenderer<?>> renderers = new HashMap<>();

		/**
		 * Enregistrement d'un {@link StringRenderer} spécifique pour une classe donnée
		 * @param clazz la classe que le {@link StringRenderer} est capable d'
		 * @param renderer
		 * @param <T>
		 */
		public <T> void addSpecificRenderer(Class<T> clazz, StringRenderer<T> renderer) {
			renderers.put(clazz, renderer);
		}

		@SuppressWarnings("unchecked")
		public <T> StringRenderer<T> getRenderer(T value) {
			StringRenderer<T> found = null;
			if (value != null) {
				final Class<?> clazz = value.getClass();
				found = (StringRenderer<T>) renderers.get(clazz);
				if (found == null) {
					final Class<?>[] intfs = clazz.getInterfaces();
					for (Class<?> intf : intfs) {
						found = (StringRenderer<T>) renderers.get(intf);
						if (found != null) {
							break;
						}
					}
				}
			}
			return found != null ? found : (StringRenderer<T>) DEFAULT_RENDERER;
		}
	}

	public static void dumpCacheKeys(Ehcache cache, Logger logger, Level level) {
		if (logger.isEnabledFor(level)) {
			try (BufferedReader reader = dumpKeys(cache.getKeys())) {
				logger.log(level, "Dump des clés du cache " + cache.getName() + " :");
				log(logger, level, reader);
				logger.log(level, "Dump des clés du cache " + cache.getName() + " terminé.");
			}
			catch (IOException e) {
				logger.log(level, "Impossible de dumper toutes les clés du cache " + cache.getName(), e);
			}
		}
	}

	public static void dumpCacheKeysAndValues(Ehcache cache, Logger logger, Level level, @Nullable ValueRendererFactory rendererFactory) {
		if (logger.isEnabledFor(level)) {
			try (BufferedReader reader = dumpKeysValues(cache, rendererFactory)) {
				logger.log(level, "Dump du contenu du cache " + cache.getName() + " :");
				log(logger, level, reader);
				logger.log(level, "Dump du contenu du cache " + cache.getName() + " terminé.");
			}
			catch (IOException e) {
				logger.log(level, "Impossible de dumper toutes le contenu du cache " + cache.getName(), e);
			}
		}
	}

	private static void log(Logger logger, Level level, BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			logger.log(level, line);
		}
	}

	/**
	 * @param keys une collection de clés de cache
	 * @return un reader qui fourni, avec une ligne par clé, cette liste (les clé sont triées par ordre alphabétique de leur représentation {@link Object#toString()})
	 * @throws IOException en cas de souci avec la génération de la donnée
	 */
	private static BufferedReader dumpKeys(Collection<?> keys) throws IOException {
		final List<DisplayableKey> dispKeys = getSortedKeys(keys);
		return buildReader(new LongStringBuilder() {
			@Override
			public void build(BufferedWriter writer) throws IOException {
				for (DisplayableKey str : dispKeys) {
					writer.write(LI);
					writer.write(str.keyString);
					writer.newLine();
				}
			}
		});
	}

	/**
	 * @param cache un cache dont le contenu doit être dumpé
	 * @param rendererFactory factory des renderers à utiliser pour dumper les valeurs
	 * @return un reader qui fournit, avec une ligne par clé, le contenu (les clés sont triées par ordre alphabétique de leur représentation {@link Object#toString()})
	 * @throws IOException en cas de souci avec la génération de la donnée
	 */
	private static BufferedReader dumpKeysValues(final Ehcache cache, @Nullable final ValueRendererFactory rendererFactory) throws IOException {
		final List<DisplayableKey> dispKeys = getSortedKeys(cache.getKeys());
		return buildReader(new LongStringBuilder() {
			@Override
			public void build(BufferedWriter writer) throws IOException {
				for (DisplayableKey key : dispKeys) {
					final Element element = cache.getQuiet(key.key);
					if (element != null) {
						writer.write(LI);
						writer.write(key.keyString);
						writer.write(ARROW);
						writer.write(getDisplayedValue(element, rendererFactory));
						writer.newLine();
					}
				}
			}
		});
	}

	private static interface LongStringBuilder {
		void build(BufferedWriter writer) throws IOException;
	}

	private static final class FileAwareReader extends Reader {

		private final File file;
		private final Reader target;

		private FileAwareReader(File file, Charset charset) throws IOException {
			this.file = file;
			this.target = new InputStreamReader(new FileInputStream(file), charset);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return target.read(cbuf, off, len);
		}

		@Override
		public void close() throws IOException {
			try {
				target.close();
			}
			finally {
				file.delete();
			}
		}
	}

	private static BufferedReader buildReader(LongStringBuilder builder) throws IOException {
		final Charset charset = Charset.forName("UTF-8");
		final File file = File.createTempFile("ur-dump-", ".tmp");
		try {
			try (OutputStream out = new FileOutputStream(file); Writer writer = new OutputStreamWriter(out, charset); BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
				builder.build(bufferedWriter);
			}
			return new BufferedReader(new FileAwareReader(file, charset));
		}
		catch (IOException | RuntimeException | Error e) {
			file.delete();
			throw e;
		}
	}

	private static String getDisplayedValue(Element element, @Nullable ValueRendererFactory rendererFactory) {
		final Object value = element.getObjectValue();
		if (value == null) {
			return NULL;
		}
		return getDisplayedValue(value, rendererFactory);
	}

	private static String getDisplayedValue(Object value, @Nullable ValueRendererFactory rendererFactory) {
		if (value instanceof Collection) {
			final StringBuilder b = new StringBuilder();
			b.append("[");
			boolean first = true;
			for (Object data : (Collection) value) {
				if (!first) {
					b.append(", ");
				}
				b.append(getDisplayedValue(data, rendererFactory));
				first = false;
			}
			b.append("]");
			return b.toString();
		}
		else if (rendererFactory != null) {
			final StringRenderer<Object> renderer = rendererFactory.getRenderer(value);
			return renderer.toString(value);
		}
		else {
			return ValueRendererFactory.DEFAULT_RENDERER.toString(value);
		}
	}

	/**
	 * Classe qui permet de trier des clés d'après leur représentation textuelle tout en conservant
	 * la valeur de la clé elle-même
	 */
	private static class DisplayableKey implements Comparable<DisplayableKey> {
		public final Object key;
		public final String keyString;

		private DisplayableKey(Object key) {
			this.key = key;
			this.keyString = key == null ? NULL : key.toString();
		}

		@Override
		public int compareTo(DisplayableKey o) {
			return keyString.compareTo(o.keyString);
		}
	}

	private static List<DisplayableKey> getSortedKeys(Collection<?> keys) {
		final List<DisplayableKey> dispKeys = new ArrayList<>(keys.size());
		for (Object key : keys) {
			dispKeys.add(new DisplayableKey(key));
		}
		Collections.sort(dispKeys);
		return dispKeys;
	}
}
