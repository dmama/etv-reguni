package ch.vd.unireg.adresse;

/**
 * @author Raphaël Marmier, 2016-07-07, <raphael.marmier@vd.ch>
 */
public interface LocaliteInvalideMatcherService {

	/**
	 * <p>Analyse si le paramètre ressemble à une localité invalide, résutat non-garanti!
	 * Mais ça permet de filtrer les cas les plus courant.</p>
	 *
	 * <p>A noter que cette méthode renverra toujours <code>false</code> si les propriétés attendues
	 * dans <code>unireg.properties</code> sont absentes.</p>
	 *
	 * @param localite la désignation de la localité à analyser
	 * @return <code>true</code> si la localité correspond à un pattern de localité invalide
	 */
	boolean match(String localite);
}
