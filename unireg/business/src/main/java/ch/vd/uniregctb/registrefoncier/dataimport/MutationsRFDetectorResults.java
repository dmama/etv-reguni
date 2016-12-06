package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;

public class MutationsRFDetectorResults {

	private final long importId;
	private final RegDate dateValeur;
	private final int nbThreads;

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime = System.currentTimeMillis();

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	private boolean interrompu = false;

	private final List<Erreur> erreurs = new LinkedList<>();

	public static final class Erreur {
		public final String idRF;
		public final String message;

		public Erreur(String idRF, String message) {
			this.idRF = idRF;
			this.message = message;
		}

		public String getIdRF() {
			return idRF;
		}

		@Override
		public String toString() {
			return "Erreur{" +
					"idRF='" + idRF + '\'' +
					", message='" + message + '\'' +
					'}';
		}
	}

	public MutationsRFDetectorResults(long importId, RegDate dateValeur, int nbThreads) {
		this.importId = importId;
		this.dateValeur = dateValeur;
		this.nbThreads = nbThreads;
	}

	public long getImportId() {
		return importId;
	}

	public RegDate getDateValeur() {
		return dateValeur;
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

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public int getNbErreurs() {
		return erreurs.size();
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public void addErrorException(Object id, Exception e) {
		erreurs.add(new Erreur(String.valueOf(id), String.format("Exception levée : %s\n%s", e.getMessage(), ExceptionUtils.extractCallStack(e))));
	}
}
