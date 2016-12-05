package ch.vd.uniregctb.registrefoncier.dataimport;

import javax.xml.bind.JAXBContext;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.GrundstueckNummerElement;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.PersonEigentumAnteilListElement;

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
	 * @return le context JAXB pour les droits
	 */
	JAXBContext getDroitListContext();

	/**
	 * @return le context JAXB pour les propriétaires
	 */
	JAXBContext getProprietaireContext();

	/**
	 * @return le context JAXB pour les bâtiments
	 */
	JAXBContext getBatimentContext();

	/**
	 * @return le context JAXB pour les surfaces au sol.
	 */
	JAXBContext getSurfacesAuSolContext();

	/**
	 * @return le context JAXB pour les listes de surfaces au sol
	 */
	JAXBContext getSurfaceListContext();

	/**
	 * @return le context JAXB pour les droits contenus dans le fichier qui contient les usufruitiers et les bénéficiaires de droits d'habitation.
	 */
	JAXBContext getAutreDroitContext();

	/**
	 * @return le context JAXB pour les droits contenus dans le fichier qui contient les communautés.
	 */
	JAXBContext getCommunauteContext();

	/**
	 * @return le context JAXB pour les liste de communes.
	 */
	JAXBContext getCommuneContext();

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

	/**
	 * Converti la communauté spécifiée dans sa représentation XML.
	 */
	String toXMLString(Gemeinschaft gemeinschaft);

	/**
	 * Converti la communauté spécifiée dans sa représentation XML.
	 */
	String toXMLString(PersonEigentumAnteilListElement gemeinschaft);

	/**
	 * Converti l'ayant-droit spécifié dans sa représentation XML.
	 */
	String toXMLString(Rechteinhaber rechteinhaber);

	/**
	 * Converti l'information de la commune (représenté ici par le numéro d'immeuble, parce qu'il n'y avait pas d'autre élément disponible dans le XSD) dans sa représentation XML.
	 */
	String toXMLString(GrundstueckNummerElement grundstueckNummer);
}