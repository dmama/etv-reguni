package ch.vd.uniregctb.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ch.vd.uniregctb.common.LockHelper;
import ch.vd.uniregctb.common.StringRenderer;

public class ValidableEntityNamingServiceImpl implements ValidableEntityNamingService {

	private final Map<Class<?>, StringRenderer<?>> rendererMap = new HashMap<>();
	private final LockHelper lockHelper = new LockHelper();

	@Override
	public <T> void registerEntityRenderer(Class<T> clazz, StringRenderer<? super T> renderer) {
		lockHelper.doInWriteLock(() -> rendererMap.put(clazz, renderer));
	}

	private <T> StringRenderer<? super T> findRenderer(T object) {
		if (object == null) {
			return null;
		}

		return lockHelper.doInReadLock(() -> {
			StringRenderer<? super T> renderer = null;
			Class<?> clazz = object.getClass();
			while (renderer == null && clazz != null) {
				//noinspection unchecked
				renderer = (StringRenderer<? super T>) rendererMap.get(clazz);
				clazz = clazz.getSuperclass();
			}
			return renderer;
		});
	}

	@Override
	public String getDisplayName(Object object) {
		return _getDisplayName(object);
	}

	private <T> String _getDisplayName(T object) {
		final StringRenderer<? super T> renderer = findRenderer(object);
		if (renderer != null) {
			return renderer.toString(object);
		}
		else {
			return Objects.toString(object);
		}
	}
}
