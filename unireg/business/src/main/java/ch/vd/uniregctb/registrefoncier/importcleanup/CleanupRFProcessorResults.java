package ch.vd.uniregctb.registrefoncier.importcleanup;

import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.JobResults;

public class CleanupRFProcessorResults extends JobResults<Long, CleanupRFProcessorResults> {

	private boolean interrompu = false;
	private final List<Processed> processed = new LinkedList<>();
	private final List<Ignored> ignored = new LinkedList<>();
	private final List<Error> errors = new LinkedList<>();

	public static final class Error {
		private final Long importId;
		private final String message;

		public Error(Long importId, String message) {
			this.importId = importId;
			this.message = message;
		}

		public Long getImportId() {
			return importId;
		}

		public String getMessage() {
			return message;
		}
	}

	public enum IgnoreReason {
		/**
		 * Import retenu car trop récent
		 */
		RETAINED("L'import est trop récent"),
		/**
		 * Import à traiter, en traitement ou en erreur
		 */
		NOT_TREATED("L'import est encore à traite, en traitement ou en erreur"),
		/**
		 * Import avec des mutations à traiter, en traitement ou en erreur
		 */
		MUTATIONS_NOT_TREATED("L'import possède des mutations encore à traiter, en traitement ou erreur");

		private final String description;

		IgnoreReason(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static final class Ignored {
		private final long importId;
		private final IgnoreReason reason;

		public Ignored(long importId, IgnoreReason reason) {
			this.importId = importId;
			this.reason = reason;
		}

		public long getImportId() {
			return importId;
		}

		public IgnoreReason getReason() {
			return reason;
		}
	}

	public static final class Processed {
		private final long importId;
		private final RegDate dateValeur;
		private final int mutCount;

		public Processed(long importId, RegDate dateValeur, int mutCount) {
			this.importId = importId;
			this.dateValeur = dateValeur;
			this.mutCount = mutCount;
		}

		public long getImportId() {
			return importId;
		}

		public RegDate getDateValeur() {
			return dateValeur;
		}

		public int getMutCount() {
			return mutCount;
		}
	}

	public CleanupRFProcessorResults() {
		super(null, null);
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public List<Processed> getProcessed() {
		return processed;
	}

	public List<Ignored> getIgnored() {
		return ignored;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public int getNbErreurs() {
		return errors.size();
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(Long importId, Exception e) {
		errors.add(new Error(importId, String.format("Exception levée : %s\n%s", e.getMessage(), ExceptionUtils.extractCallStack(e))));
	}

	public void addProcessed(long importId, RegDate dateValeur, int mutCount) {
		processed.add(new Processed(importId, dateValeur, mutCount));
	}

	public void addIgnored(long importId, @NotNull IgnoreReason reason) {
		ignored.add(new Ignored(importId, reason));
	}

	@Override
	public void addAll(@NotNull CleanupRFProcessorResults right) {
		processed.addAll(right.processed);
		ignored.addAll(right.ignored);
		errors.addAll(right.errors);
	}
}
