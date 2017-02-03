package ch.vd.uniregctb.foncier.migration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	private final List<LigneInfo> lignesEnErreur = new LinkedList<>();
	private int nbDemandesTraitees = 0;
	private final List<Ignore> donneesIgnorees = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
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

	public static class Ignore {
		private final MigrationDDKey key;
		private final String message;

		public Ignore(MigrationDDKey key, String message) {
			this.key = key;
			this.message = message;
		}

		public MigrationDDKey getKey() {
			return key;
		}

		public String getMessage() {
			return message;
		}
	}

	public static class LigneIgnoree extends Ignore {
		public final MigrationDD dd;

		public LigneIgnoree(MigrationDD dd, String message) {
			super(new MigrationDDKey(dd), message);
			this.dd = dd;
		}

		@Override
		public String getMessage() {
			return String.format("%s (%s)", super.getMessage(), dd);
		}
	}

	public static class Erreur {
		private final String message;

		public Erreur(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}

	public static final class ErreurDemande extends Erreur {
		private final MigrationDD dd;

		public ErreurDemande(MigrationDD dd, String message) {
			super(message);
			this.dd = dd;
		}

		@Override
		public String getMessage() {
			return String.format("%s (%s)", super.getMessage(), dd);
		}
	}

	public static final class ErreurContribuable extends Erreur {
		private final long noContribuable;

		public ErreurContribuable(long noContribuable, String message) {
			super(message);
			this.noContribuable = noContribuable;
		}

		@Override
		public String getMessage() {
			return String.format("%d : %s", noContribuable, super.getMessage());
		}
	}

	public MigrationDDImporterResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public void addAll(MigrationDDImporterResults right) {
		nbLignes += right.nbLignes;
		lignesEnErreur.addAll(right.lignesEnErreur);
		nbDemandesTraitees += right.nbDemandesTraitees;
		donneesIgnorees.addAll(right.donneesIgnorees);
		erreurs.addAll(right.erreurs);
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public int incNbLignes() {
		return ++nbLignes;
	}

	public int incNbDemandesTraitees() {
		return ++nbDemandesTraitees;
	}

	public void addLineEnErreur(int index, String message) {
		lignesEnErreur.add(new LigneInfo(index, message));
	}

	public void addLigneIgnoree(MigrationDD migrationDD, String message) {
		donneesIgnorees.add(new LigneIgnoree(migrationDD, message));
	}

	public void addDonneeDegrevementVide(Map.Entry<MigrationDDKey, ValeurDegrevement> dd) {
		donneesIgnorees.add(new Ignore(dd.getKey(), "Aucune valeur de dégrèvement disponible pour la PF " + dd.getValue().getPeriodeFiscale()));
	}

	public void addErreur(Map.Entry<MigrationDDKey, ValeurDegrevement> dd, String message) {
		erreurs.add(new Erreur(String.format("%s (%s)", message, dd.getKey())));
	}

	public void addDemandeEnErreur(MigrationDD dd, String message) {
		erreurs.add(new ErreurDemande(dd, message));
	}

	public void addContribuableEnErreur(long noContribuable, String message) {
		erreurs.add(new ErreurContribuable(noContribuable, message));
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

	public List<LigneInfo> getLignesEnErreur() {
		return lignesEnErreur;
	}

	public int getNbDemandesTraitees() {
		return nbDemandesTraitees;
	}

	/**
	 * @return la liste des lignes d'input ignorées
	 */
	public List<Ignore> getDonneesIgnorees() {
		return donneesIgnorees;
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

	public List<Erreur> getErreurs() {
		return erreurs;
	}
}

