package ch.vd.uniregctb.foncier.migration.ifonc;

import java.util.LinkedList;
import java.util.List;

public class MigrationExoIFONCImporterResults {

	private final int nbThreads;
	private boolean interrompu = false;
	private final long start = System.currentTimeMillis();
	private long end;

	private int nbLignesLues;
	private int nbExonerationsTraitees;
	private final List<LigneInfo> lignesEnErreur = new LinkedList<>();
	private final List<ExonerationInfo> exonerationsEnErreur = new LinkedList<>();
	private final List<ContribuableInfo> contribuablesEnErreur = new LinkedList<>();

	public static final class LigneInfo {
		public final int index;
		public final String message;

		public LigneInfo(int index, String message) {
			this.index = index;
			this.message = message;
		}

		@Override
		public String toString() {
			return "LigneInfo{" +
					"index=" + index +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static final class ExonerationInfo {
		public final MigrationExoIFONC exo;
		public final String message;

		public ExonerationInfo(MigrationExoIFONC exo, String message) {
			this.exo = exo;
			this.message = message;
		}

		@Override
		public String toString() {
			return "ExonerationInfo{" +
					"exo=" + exo +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static final class ContribuableInfo {
		public final long noContribuable;
		public final String message;

		public ContribuableInfo(long noContribuable, String message) {
			this.noContribuable = noContribuable;
			this.message = message;
		}

		@Override
		public String toString() {
			return "ContribuableInfo{" +
					"noContribuable=" + noContribuable +
					", message='" + message + '\'' +
					'}';
		}
	}
	
	public MigrationExoIFONCImporterResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public void end() {
		this.end = System.currentTimeMillis();
	}

	public long duration() {
		if (end == 0L) {
			throw new IllegalStateException("Job is not over yet!!");
		}
		return end - start;
	}

	public void addLigneEnErreur(int index, String message) {
		this.lignesEnErreur.add(new LigneInfo(index, message));
	}

	public void incExonerationsTraitees() {
		++ this.nbExonerationsTraitees;
	}

	public void addAll(MigrationExoIFONCImporterResults other) {
		this.nbLignesLues += other.nbLignesLues;
		this.nbExonerationsTraitees += other.nbExonerationsTraitees;
		this.exonerationsEnErreur.addAll(other.exonerationsEnErreur);
		this.lignesEnErreur.addAll(other.lignesEnErreur);
		this.contribuablesEnErreur.addAll(other.contribuablesEnErreur);
	}

	public void addExonerationEnErreur(MigrationExoIFONC migrationExoIFONC, String message) {
		this.exonerationsEnErreur.add(new ExonerationInfo(migrationExoIFONC, message));
	}

	public void addContribuableEnErreur(long noContribuable, String message) {
		this.contribuablesEnErreur.add(new ContribuableInfo(noContribuable, message));
	}

	public void addLigneLue() {
		++ nbLignesLues;
	}

	public int getNbLignesLues() {
		return nbLignesLues;
	}

	public int getNbExonerationsTraitees() {
		return nbExonerationsTraitees;
	}

	public List<LigneInfo> getLignesEnErreur() {
		return lignesEnErreur;
	}

	public List<ExonerationInfo> getExonerationsEnErreur() {
		return exonerationsEnErreur;
	}

	public List<ContribuableInfo> getContribuablesEnErreur() {
		return contribuablesEnErreur;
	}
}
