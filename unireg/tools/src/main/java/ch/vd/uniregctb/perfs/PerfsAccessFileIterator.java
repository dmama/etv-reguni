package ch.vd.uniregctb.perfs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.uniregctb.perfs.PerfsAccessFile.Call;

/**
 * Itérateur qui permet de récupérer les ids des contribuables à processer en respectant les temps d'accès défini dans le fichier.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsAccessFileIterator {

	private static final Logger LOGGER = Logger.getLogger(PerfsAccessFileIterator.class);

	private final long startTime;
	private final ArrayList<Call> calls;
	private final int targetCount;

	private int i;
	private int count;

	/**
	 * Construit un itérateur qui itère sur tous les appels du fichier une fois et s'arrête.
	 */
	public PerfsAccessFileIterator(PerfsAccessFile file) {
		this.startTime = System.currentTimeMillis();
		this.calls = file.getCalls();
		this.targetCount = this.calls.size();

		this.i = -1;
		this.count = -1;
	}

	/**
	 * Construit un itérateur qui itère sur tous les appels du fichier de manière rotative tant que targetCount n'est pas atteint.
	 */
	public PerfsAccessFileIterator(PerfsAccessFile file, int targetCount) {
		this.startTime = System.currentTimeMillis();
		this.calls = file.getCalls();
		this.targetCount = targetCount;
		Assert.isTrue(calls.size() > 0 || targetCount == 0);

		this.i = -1;
		this.count = -1;
	}

	/**
	 * @return le prochain id de contribuable à processer, ou <b>null</b> s'il n'y a plus d'id. Cette méthode bloque le temps nécessaire
	 *         pour assurer un traitement conforme au profil d'accès.
	 */
	public Long getNextId() {

		synchronized (this) {
			// incrémente l'index
			++i;
			++count;
			if (i >= calls.size()) {
				i = 0; // comportement rotatif
			}
			if (count >= targetCount) {
				return null;
			}

			// récupère le prochain appel et attend le temps nécessaire
			Call call = calls.get(i);
			long now = System.currentTimeMillis();
			long sleep = startTime + call.getMillisecondes() - now;
			if (sleep > 0) {
				try {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Sleeping for " + sleep + " ms...");
					}
					Thread.sleep(sleep);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return call.getId();
		}
	}

	/**
	 * @return les prochains 'count' ids à processer, ou <b>null</b> si'il n'y a pluis d'id.
	 * <p>
	 * Cette méthode ne bloque pas.
	 */
	public List<Long> getNextIds(final int size) {

		List<Long> ids = null;

		synchronized (this) {
			for (int k = 0; k < size; k++) {
				// incrémente l'index
				++i;
				++count;
				if (i >= calls.size()) {
					i = 0; // comportement rotatif
				}
				if (count >= targetCount) {
					break;
				}

				if (ids == null) {
					// création à la demande
					ids = new ArrayList<Long>(size);
				}

				Call call = calls.get(i);
				ids.add(call.getId());
			}
		}

		return ids;
	}

	public int getCount() {
		return count;
	}
}
