package ch.vd.uniregctb.evenement.jms;

import org.apache.xmlbeans.XmlException;

/**
 * Message driven Pojo : en charge de la persistence des événements civils unitaires reçus au format XML.
 *
 * @author Jean-Eric Cuendet
 */
public interface EvenementCivilUnitaireMDP {

	public void onMessage(String message) throws Exception;

	/**
	 * @return <code>null</code> si l'événement unitaire n'a pas été inséré (ignoré)
	 */
	public Long insertEvenementUnitaire(String message) throws XmlException;

	public boolean insertRegroupeAndTraite(String message, StringBuffer errorMsg);
	public long regroupeEvenement(long id, StringBuffer errorMsg);

}
