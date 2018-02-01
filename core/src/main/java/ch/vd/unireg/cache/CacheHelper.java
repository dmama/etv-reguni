package ch.vd.unireg.cache;

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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.utils.LogLevel;

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

		private final Map<Class<?>, StringRenderer<?>> renderers = new HashMap<>();

		/**
		 * Enregistrement d'un {@link StringRenderer} spécifique pour une classe donnée
		 * @param clazz la classe que le {@link StringRenderer} est capable d'
		 * @param renderer
		 * @param <T>
		 */
		public <T> void addSpecificRenderer(Class<T> clazz, StringRenderer<? super T> renderer) {
			renderers.put(clazz, renderer);
		}

		@SuppressWarnings("unchecked")
		public <T> StringRenderer<? super T> getRenderer(T value) {
			StringRenderer<? super T> found = null;
			if (value != null) {
				final Class<?> clazz = value.getClass();
				found = (StringRenderer<? super T>) renderers.get(clazz);
				if (found == null) {
					final Class<?>[] intfs = clazz.getInterfaces();
					for (Class<?> intf : intfs) {
						found = (StringRenderer<? super T>) renderers.get(intf);
						if (found != null) {
							break;
						}
					}
				}
			}
			if (found != null) {
				return found;
			}
			else {
				return StringRenderer.DEFAULT;
			}
		}
	}

	public static void dumpCacheKeys(Ehcache cache, Logger logger, LogLevel.Level level) {
		if (LogLevel.isEnabledFor(logger, level)) {
			try (BufferedReader reader = dumpKeys(cache.getKeys())) {
				LogLevel.log(logger, level, "Dump des clés du cache " + cache.getName() + " :");
				log(logger, level, reader);
				LogLevel.log(logger, level, "Dump des clés du cache " + cache.getName() + " terminé.");
			}
			catch (IOException e) {
				LogLevel.log(logger, level, "Impossible de dumper toutes les clés du cache " + cache.getName(), e);
			}
		}
	}

	public static void dumpCacheKeysAndValues(Ehcache cache, Logger logger, LogLevel.Level level, @Nullable ValueRendererFactory rendererFactory) {
		if (LogLevel.isEnabledFor(logger, level)) {
			try (BufferedReader reader = dumpKeysValues(cache, rendererFactory)) {
				LogLevel.log(logger, level, "Dump du contenu du cache " + cache.getName() + " :");
				log(logger, level, reader);
				LogLevel.log(logger, level, "Dump du contenu du cache " + cache.getName() + " terminé.");
			}
			catch (IOException e) {
				LogLevel.log(logger, level, "Impossible de dumper toutes le contenu du cache " + cache.getName(), e);
			}
		}
	}

	private static void log(Logger logger, LogLevel.Level level, BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			LogLevel.log(logger, level, line);
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

	private interface LongStringBuilder {
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

	private static <T> String getDisplayedValue(T value, @Nullable ValueRendererFactory rendererFactory) {
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
			final StringRenderer<? super T> renderer = rendererFactory.getRenderer(value);
			return renderer.toString(value);
		}
		else {
			return StringRenderer.DEFAULT.toString(value);
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
