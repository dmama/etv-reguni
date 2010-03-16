package ch.vd.uniregctb.evenement.jms;

import org.apache.xmlbeans.XmlException;

/**
 * Message driven Pojo : en charge de la persistence des événements civils unitaires reçus au format XML.
 *
 * @author Jean-Eric Cuendet
 */
public interface EvenementCivilUnitaireMDP {

	public void onMessage(String message) throws Exception;

	public long insertEvenementUnitaire(String message) throws XmlException;
	public boolean insertRegroupeAndTraite(String message, StringBuffer errorMsg);
	public long regroupeEvenement(long id, StringBuffer errorMsg);

}
