package ch.vd.uniregctb.identification.contribuable;

/**
 * Exception lancée quand une demande d'identifcation fournit plus de résultats que le seuil autorisé
 */
public class TooManyIdentificationPossibilitiesException extends Exception {

	private final int maxAllowedSize;

	public TooManyIdentificationPossibilitiesException(int maxAllowedSize) {
		this.maxAllowedSize = maxAllowedSize;
	}

	public int getMaxAllowedSize() {
		return maxAllowedSize;
	}
}
