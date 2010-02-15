package ch.vd.uniregctb.evenement.externe.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.jms.listener.SessionAwareMessageListener;

import ch.vd.uniregctb.evenement.externe.DelegateEvenementExterne;
import ch.vd.uniregctb.evenement.externe.EvenementExterneResultat;

/**
 * Implémentation du listener sur la queue de réception
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2008/02/15 16:42:12 $)
 * @version $Revision: 1.2 $
 */
public final class MessageListener implements SessionAwareMessageListener {

	/**
	 * object délégué a appelé quand un message est recu
	 */
	private DelegateEvenementExterne delegate;

	/**
	 * contient le service responsable des evenement externe
	 */
	private EvenementExterneFacadeImpl parent;

	/**
	 * logger associé à cette classe.
	 */
	private static final Logger LOGGER = Logger.getLogger(MessageListener.class);

	/**
	 * Définit la classe déléguée sur le service Editique.
	 *
	 * @param delegate
	 *            classe déléguée.
	 */
	public void setDelegate(DelegateEvenementExterne delegate) {
		this.delegate = delegate;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onMessage(Message message, Session session) throws JMSException {
		if (this.delegate == null) {
			// force le rollback car le callback n'est pas définit
			throw new javax.jms.IllegalStateException("DelegateEvenementIS est null.");
		}
		if (message instanceof TextMessage) {
			try {
				EvenementExterneResultat resultat = parent.createResultfromMessage(message);
				this.delegate.surEvenementRecu(resultat);
			}
			catch (Exception ex) {
				LOGGER.error(ex, ex);
				throw new JMSException(ex.getMessage());
			}
		}
		else {
			LOGGER.error("message n'est pas un javax.jms.TextMessage.");
		}
	}

	/**
	 * Définit le service responsable des evenement externe.
	 *
	 * @param parent
	 *            le service responsable des evenement externe
	 */
	public void setParent(EvenementExterneFacadeImpl parent) {
		this.parent = parent;
	}

}
