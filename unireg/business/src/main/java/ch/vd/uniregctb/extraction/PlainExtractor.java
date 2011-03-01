package ch.vd.uniregctb.extraction;

/**
 * Extracteur de données sous la forme d'un fichier CSV par exemple
 */
public interface PlainExtractor extends Extractor {

	/**
	 * Point d'entrée officiel de l'opération d'extraction
	 * @return le résultat de l'extraction
	 */
	ExtractionResult doExtraction();
}
