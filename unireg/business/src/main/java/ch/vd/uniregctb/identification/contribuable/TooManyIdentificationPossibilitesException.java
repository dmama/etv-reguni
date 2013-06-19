package ch.vd.uniregctb.identification.contribuable;

/**
 * Exception lancée quand une demande d'identifcation fournit plus de résultats que le seuil autorisé
 */
public class TooManyIdentificationPossibilitesException extends Exception {

	private final int maxAllowedSize;

	public TooManyIdentificationPossibilitesException(int maxAllowedSize) {
		this.maxAllowedSize = maxAllowedSize;
	}

	public int getMaxAllowedSize() {
		return maxAllowedSize;
	}
}
