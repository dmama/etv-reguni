package ch.vd.uniregctb.registrefoncier.elements;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;

/**
 * Classe de manipulation XML des entités propres à l'import du registre foncier.
 */
public interface XmlHelperRF {

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
}
