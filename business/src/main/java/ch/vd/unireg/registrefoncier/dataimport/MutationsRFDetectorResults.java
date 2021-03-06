package ch.vd.unireg.registrefoncier.dataimport;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;

public class MutationsRFDetectorResults {

	private final long importId;
	private final boolean importInitial;
	private final TypeImportRF type;
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

	private final List<Avertissement> avertissements = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();

	public static final class Avertissement {
		public final String idRF;
		public final String egrid;
		public final String message;

		public Avertissement(String idRF, String egrid, String message) {
			this.idRF = idRF;
			this.egrid = egrid;
			this.message = message;
		}

		public String getIdRF() {
			return idRF;
		}

		public String getEgrid() {
			return egrid;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "Avertissement{" +
					"idRF='" + idRF + '\'' +
					", egrid='" + egrid + '\'' +
					", message='" + message + '\'' +
					'}';
		}
	}

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

	public MutationsRFDetectorResults(long importId, boolean importInitial, TypeImportRF type, RegDate dateValeur, int nbThreads) {
		this.importId = importId;
		this.importInitial = importInitial;
		this.type = type;
		this.dateValeur = dateValeur;
		this.nbThreads = nbThreads;
	}

	public long getImportId() {
		return importId;
	}

	public boolean isImportInitial() {
		return importInitial;
	}

	public TypeImportRF getType() {
		return type;
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

	public List<Avertissement> getAvertissements() {
		return avertissements;
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

	public void addAvertissement(String idRf, String egrid, String message) {
		avertissements.add(new Avertissement(idRf, egrid, message));
	}

	public void addErrorException(Object id, Exception e) {
		erreurs.add(new Erreur(String.valueOf(id), String.format("Exception levée : %s\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e))));
	}
}
