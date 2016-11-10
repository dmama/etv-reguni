package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.JAXBContext;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;

/**
 * Classe de manipulation XML des entités propres à l'import du registre foncier.
 */
public interface XmlHelperRF {

	/**
	 * @return le context JAXB pour les immeubles
	 */
	JAXBContext getImmeubleContext();

	/**
	 * @return le context JAXB pour les droits
	 */
	JAXBContext getDroitContext();

	/**
	 * @return le context JAXB pour les propriétaires
	 */
	JAXBContext getProprietaireContext();

	/**
	 * @return le context JAXB pour les bâtiments
	 */
	JAXBContext getBatimentContext();

	/**
	 * @return le context JAXB pour les surface
	 */
	JAXBContext getSurfaceContext();

	/**
	 * @return le context JAXB pour les listes de surfaces
	 */
	JAXBContext getSurfaceListContext();

	/**
	 * @return le context JAXB pour les droits contenus dans le fichier qui contient les usufruitiers et les bénéficiaires de droits d'habitation.
	 */
	JAXBContext getAutreDroitContext();

	/**
	 * Converti l'immeuble spécifié dans sa représentation XML.
	 */
	String toXMLString(Grundstueck obj);

	/**
	 * Converti le droit spécifié dans sa représentation XML.
	 */
	String toXMLString(PersonEigentumAnteil obj);

	/**
	 * Converti le propriétaire spécifié dans sa représentation XML.
	 */
	String toXMLString(Personstamm obj);

	/**
	 * Converti le bâtiment spécifié dans sa représentation XML.
	 */
	String toXMLString(Gebaeude obj);

	/**
	 * Converti la surface spécifiée dans sa représentation XML.
	 */
	String toXMLString(Bodenbedeckung obj);

	/**
	 * Converti la liste de surfaces spécifiée dans sa représentation XML (élément = BodenbedeckungListElement)
	 */
	String toXMLString(GrundstueckExport.BodenbedeckungList surfaces);
}
