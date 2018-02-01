package ch.vd.uniregctb.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

public class DelayedDownloadServiceImpl implements DelayedDownloadService, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DelayedDownloadServiceImpl.class);

	private final Map<UUID, Pair<? extends TypedDataContainer, Long>> container = new HashMap<>();
	private ScheduledExecutorService scheduler;

	@Override
	public UUID putDocument(EditiqueResultatDocument document, String filenameRadical) throws IOException {
		synchronized (container) {
			final UUID uuid = newUUID();
			final PrintedDocument pd = new PrintedDocument(document.getContentType(), document.getDocument(), filenameRadical);
			container.put(uuid, Pair.of(pd, System.nanoTime()));
			return uuid;
		}
	}

	@Override
	public TypedDataContainer fetchDocument(UUID id, boolean remove) {
		final Pair<? extends TypedDataContainer, Long> found;
		synchronized (container) {
			found = remove ? container.remove(id) : container.get(id);
		}
		return found != null ? found.getLeft() : null;
	}

	@Override
	public void eraseDocument(UUID id) {
		final TypedDataContainer data = fetchDocument(id, true);
		if (data != null) {
			data.close();
		}
	}

	@Override
	public int getPendingSize() {
		synchronized (container) {
			return container.size();
		}
	}

	/**
	 * Tache de cleanup pour effacer les contenus qui ont plus d'une heure de présence dans la map
	 * (quelque chose est parti de travers s'il y en a)
	 */
	private class CleanupTask implements Runnable {
		@Override
		public void run() {
			final long now = System.nanoTime();
			synchronized (container) {
				final Iterator<Pair<? extends TypedDataContainer, Long>> iterator = container.values().iterator();
				while (iterator.hasNext()) {
					final Pair<? extends TypedDataContainer, Long> data = iterator.next();
					if (now - data.getRight() > TimeUnit.HOURS.toNanos(1)) {
						iterator.remove();

						LOGGER.warn(String.format("Nettoyage du document %s (%s) enregistré il y a plus d'une heure pour un téléchargement décalé.", data.getLeft().getFilenameRadical(), data.getLeft().getMimeType()));
						data.getLeft().close();
					}
				}
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(new DefaultThreadNameGenerator("DelayedDownloadCleanup"), Boolean.TRUE));
		this.scheduler.scheduleWithFixedDelay(new CleanupTask(), 30L, 30L, TimeUnit.MINUTES);      // toutes les 30 minutes...
	}

	@Override
	public void destroy() throws Exception {
		this.scheduler.shutdownNow();
	}

	/**
	 * Allocation d'un nouvel UUID avec la garantie qu'il n'est pas dans la Map
	 * (cette méthode doit être appelée dans un contexte synchronisé pour éviter les allocations du même élément à double...)
	 * @return un nouvel UUID
	 */
	@NotNull
	private UUID newUUID() {
		UUID uuid;
		do {
			uuid = UUID.randomUUID();
		}
		while (container.containsKey(uuid));
		return uuid;
	}
}
