package ch.vd.uniregctb.foncier.migration;

import java.util.LinkedList;
import java.util.List;

public class MigrationDDImporterResults {

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime = System.currentTimeMillis();

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	private boolean interrompu = false;

	private int nbLignes = 0;
	private final List<LigneInfo> lignesEnErreurs = new LinkedList<>();
	private int nbDemandesExtraites = 0;
	private int nbDemandesTraitees = 0;
	private final List<DemandeInfo> demandesIgnorees = new LinkedList<>();
	private final List<DemandeInfo> demandesEnErreurs = new LinkedList<>();
	private int nbThreads;

	public static final class LigneInfo {
		public final int index;
		public final String message;

		public LigneInfo(int index, String message) {
			this.index = index;
			this.message = message;
		}

		public int getIndex() {
			return index;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "LigneInfo{" +
					"index=" + index +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static final class DemandeInfo {
		public final MigrationDD dd;
		public final String message;

		public DemandeInfo(MigrationDD dd, String message) {
			this.dd = dd;
			this.message = message;
		}

		public MigrationDD getDd() {
			return dd;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "Info{" +
					"dd=" + dd +
					", message='" + message + '\'' +
					'}';
		}
	}

	public MigrationDDImporterResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public void addAll(MigrationDDImporterResults right) {
		nbLignes += right.nbLignes;
		lignesEnErreurs.addAll(right.lignesEnErreurs);
		nbDemandesExtraites += right.nbDemandesExtraites;
		nbDemandesTraitees += right.nbDemandesTraitees;
		demandesIgnorees.addAll(right.demandesIgnorees);
		demandesEnErreurs.addAll(right.demandesEnErreurs);
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public int incNbLignes() {
		return ++nbLignes;
	}

	public int incNbDemandesExtraites() {
		return ++nbDemandesExtraites;
	}

	public int incNbDemandesTraitees() {
		return ++nbDemandesTraitees;
	}

	public void addLineEnErreur(int index, String message) {
		lignesEnErreurs.add(new LigneInfo(index, message));
	}

	public void addDemandeIgnoree(MigrationDD migrationDD, String message) {
		demandesIgnorees.add(new DemandeInfo(migrationDD, message));
	}

	public void addDemandeEnErreur(MigrationDD dd, String message) {
		demandesEnErreurs.add(new DemandeInfo(dd, message));
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public int getNbLignes() {
		return nbLignes;
	}

	public List<LigneInfo> getLignesEnErreurs() {
		return lignesEnErreurs;
	}

	public int getNbDemandesExtraites() {
		return nbDemandesExtraites;
	}

	public int getNbDemandesTraitees() {
		return nbDemandesTraitees;
	}

	/**
	 * @return la liste des demandes de dégrèvement ignorées
	 */
	public List<DemandeInfo> getDemandesIgnorees() {
		return demandesIgnorees;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public List<DemandeInfo> getDemandesEnErreurs() {
		return demandesEnErreurs;
	}
}

