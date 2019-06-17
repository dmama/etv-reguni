package ch.vd.unireg.cache;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cache manager qui expose les caches d'Unireg
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface UniregCacheManager {

	@NotNull
	Map<String, UniregCacheInterface> getCaches();

	@Nullable
	UniregCacheInterface getCache(@NotNull String name);
}
