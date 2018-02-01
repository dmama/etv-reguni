package ch.vd.uniregctb.tiers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.vd.uniregctb.parentes.ParenteUpdateInfo;

public final class ParenteUpdateResult {

	public static final ParenteUpdateResult EMPTY = new ParenteUpdateResult(Collections.emptyList(), Collections.emptyList());

	public static final class Error {

		private final long noCtb;
		private final String errorMsg;

		public Error(long noCtb, String errorMsg) {
			this.noCtb = noCtb;
			this.errorMsg = errorMsg;
		}

		public long getNoCtb() {
			return noCtb;
		}

		public String getErrorMsg() {
			return errorMsg;
		}
	}

	private final List<ParenteUpdateInfo> updates;
	private final List<Error> errors;

	public ParenteUpdateResult() {
		this(new LinkedList<>(), new LinkedList<>());
	}

	private ParenteUpdateResult(List<ParenteUpdateInfo> updates, List<Error> errors) {
		this.updates = updates;
		this.errors = errors;
	}

	public void addUpdate(ParenteUpdateInfo update) {
		updates.add(update);
	}

	public void addError(long noCtb, String exception) {
		errors.add(new Error(noCtb, exception));
	}

	public List<ParenteUpdateInfo> getUpdates() {
		return updates;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void addAll(ParenteUpdateResult other) {
		this.updates.addAll(other.updates);
		this.errors.addAll(other.errors);
	}

	public boolean isEmpty() {
		return this.updates.isEmpty() && this.errors.isEmpty();
	}
}
