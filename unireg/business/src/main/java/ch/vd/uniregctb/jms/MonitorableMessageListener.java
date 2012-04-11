package ch.vd.uniregctb.jms;

/**
 * Interface implémentée par les classes dérivée de {@link ch.vd.technical.esb.jms.EsbMessageListener EsbMessageListener}
 * qui permet d'externaliser (en JMX) certaines données statistiques sur les messages reçus
 */
public interface MonitorableMessageListener {

	/**
	 * @return le nombre de messages reçus par le listener depuis le démarrage de l'application
	 */
	int getNombreMessagesRecus();
}
