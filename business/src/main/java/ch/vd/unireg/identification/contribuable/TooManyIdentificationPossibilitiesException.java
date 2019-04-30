package ch.vd.unireg.identification.contribuable;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.indexer.tiers.TiersIndexedData;

/**
 * Exception lancée quand une demande d'identifcation fournit plus de résultats que le seuil autorisé
 */
public class TooManyIdentificationPossibilitiesException extends RuntimeException {

	private final int maxAllowedSize;
	private final List<Long> examplesFound;

	public TooManyIdentificationPossibilitiesException(int maxAllowedSize, List<TiersIndexedData> indexedData) {
		this.maxAllowedSize = maxAllowedSize;
		this.examplesFound = new ArrayList<>(maxAllowedSize);
		for (TiersIndexedData data : indexedData.subList(0, Math.min(maxAllowedSize, indexedData.size()))) {
			this.examplesFound.add(data.getNumero());
		}
	}

	public int getMaxAllowedSize() {
		return maxAllowedSize;
	}

	public List<Long> getExamplesFound() {
		return examplesFound;
	}
}
