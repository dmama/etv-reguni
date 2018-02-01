package ch.vd.unireg.extraction;

/**
 * Extracteur de données sous la forme d'un fichier CSV par exemple
 */
public interface PlainExtractor extends Extractor {

	/**
	 * Point d'entrée officiel de l'opération d'extraction
	 * @return le résultat de l'extraction
	 * @throws Exception en cas de problème...
	 */
	ExtractionResult doExtraction() throws Exception;
}
