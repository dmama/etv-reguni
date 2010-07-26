package ch.vd.uniregctb.indexer;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Exception renvoyée par le moteur d'indexation lors d'un traitement batch de plusieurs tiers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IndexerBatchException extends RuntimeException {

	private static final long serialVersionUID = -4334630426885016881L;

	private final List<Pair<Long, Exception>> exceptions = new ArrayList<Pair<Long, Exception>>();

	public IndexerBatchException() {
	}

	public IndexerBatchException(String message) {
		super(message);
	}

	public void addException(Tiers tiers, Exception first) {
		exceptions.add(new Pair<Long, Exception>(tiers.getId(), first));
	}

	public void addException(Long tiersId, Exception first) {
		exceptions.add(new Pair<Long, Exception>(tiersId, first));
	}

	/**
	 * @return la liste des tiers en erreur et des exceptions associées.
	 */
	public List<Pair<Long, Exception>> getExceptions() {
		return exceptions;
	}

	@Override
	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);
		final int size = exceptions.size();
		for (int i = 0; i < size; ++i) {
			final Pair<Long, Exception> p = exceptions.get(i);
			s.println("---- Batch Sub-Exception #" + i + " ------------");
			p.getSecond().printStackTrace(s);
		}
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		super.printStackTrace(s);
		final int size = exceptions.size();
		for (int i = 0; i < size; ++i) {
			final Pair<Long, Exception> p = exceptions.get(i);
			s.println("---- Batch Sub-Exception #" + i + " ------------");
			p.getSecond().printStackTrace(s);
		}
	}

	@Override
	public String getMessage() {

		final StringBuilder message = new StringBuilder();

		final String superMessage = super.getMessage();
		if (superMessage != null) {
			message.append(superMessage).append(": ");
		}

		if (exceptions.size() == 1) {

			// Entête
			final Pair<Long, Exception> p = exceptions.get(0);
			final Long tiersId = p.getFirst();
			final Exception e = p.getSecond();

			if (tiersId != null) {
				message.append("Impossible d'indexer le tiers n°").append(tiersId).append(": ");
			}
			else {
				message.append("Impossible d'indexer un tiers inconnu: ");
			}

			message.append(e.getMessage());
		}
		else {
			// Entête
			message.append("Impossible d'indexer les tiers n°").append(getTiersNumerosDescription());

			// Message des sous-exceptions
			final int size = exceptions.size();
			for (int i = 0; i < size; ++i) {
				final Pair<Long, Exception> p = exceptions.get(i);
				final Long tiersId = p.getFirst();
				final Exception e = p.getSecond();
				message.append("\n  ");
				if (tiersId != null) {
					message.append(tiersId);
				}
				else {
					message.append("<unknown>");
				}
				message.append(": ").append(e.getMessage());
			}
		}

		return message.toString();
	}

	public String getTiersNumerosDescription() {
		StringBuilder builder = new StringBuilder("{");
		final int size = exceptions.size();
		for (int i = 0; i < size; ++i) {
			final Pair<Long, Exception> p = exceptions.get(i);
			final Long tiersId = p.getFirst();
			if (tiersId == null) {
				continue;
			}
			builder.append(tiersId);
			if (i < size - 1) {
				builder.append(", ");
			}
		}
		builder.append("}");
		return builder.toString();
	}

	public void add(IndexerBatchException e) {
		exceptions.addAll(e.exceptions);
	}
}
