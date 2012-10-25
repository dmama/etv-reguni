package ch.vd.uniregctb.jms;

/**
 * Interface implémentée par les classes dérivée de {@link ch.vd.technical.esb.jms.EsbMessageListener EsbMessageListener}
 * qui permet d'externaliser (en JMX) certaines données statistiques sur les erreurs générées à la réception de messages
 */
public interface ErrorMonitorableMessageListener extends MonitorableMessageListener {

	/**
	 * @return le nombre de messages renvoyés en erreur par le listener depuis le démarrage de l'application
	 */
	int getNombreMessagesRenvoyesEnErreur();

	/**
	 * @return le nombre de messages renvoyés en exception par le listener depuis le démarrage de l'application
	 */
	int getNombreMessagesRenvoyesEnException();
}
