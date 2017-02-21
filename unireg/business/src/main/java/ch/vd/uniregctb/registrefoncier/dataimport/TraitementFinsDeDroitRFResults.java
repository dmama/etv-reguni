package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.LinkedList;
import java.util.List;

import ch.vd.uniregctb.common.JobResults;

public class TraitementFinsDeDroitRFResults extends JobResults<Long, TraitementFinsDeDroitRFResults> {

	private final int nbThreads;

	private boolean interrompu = false;
	private final List<Traite> traites = new LinkedList<>();
	private final List<Ignore> ignores = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();

	/**
	 * Information sur un immeuble traité.
	 */
	public static class Traite {
		public final long immeubleId;

		public Traite(long immeubleId) {
			this.immeubleId = immeubleId;
		}

		public long getImmeubleId() {
			return immeubleId;
		}
	}

	/**
	 * Information sur un immeuble qui aurait dû être traité mais qui ne l'a pas été.
	 */
	public static class Ignore {
		public final long immeubleId;
		public final String raison;

		public Ignore(long immeubleId, String raison) {
			this.immeubleId = immeubleId;
			this.raison = raison;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getRaison() {
			return raison;
		}
	}

	/**
	 * Information sur un immeuble dont le traitement a levé une erreur.
	 */
	public static final class Erreur {
		public final long immeubleId;
		private final String message;

		public Erreur(long immeubleId, String message) {
			this.immeubleId = immeubleId;
			this.message = message;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "Erreur{" +
					"immeubleId=" + immeubleId +
					", message='" + message + '\'' +
					'}';
		}
	}

	public TraitementFinsDeDroitRFResults(int nbThreads) {
		super(null, null);
		this.nbThreads = nbThreads;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public List<Traite> getTraites() {
		return traites;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public int getNbErreurs() {
		return erreurs.size();
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public void addImmeubleTraite(long immeubleId) {
		traites.add(new Traite(immeubleId));
	}

	public void addImmeubleIgnore(long immeubleId, String raison) {
		ignores.add(new Ignore(immeubleId, raison));
	}

	@Override
	public void addErrorException(Long id, Exception e) {
		erreurs.add(new Erreur(id, String.format("Exception levée : %s", e.getMessage())));
	}

	@Override
	public void addAll(TraitementFinsDeDroitRFResults right) {
		traites.addAll(right.traites);
		erreurs.addAll(right.erreurs);
	}
}
