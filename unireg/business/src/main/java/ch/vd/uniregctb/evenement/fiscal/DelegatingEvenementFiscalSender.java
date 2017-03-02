package ch.vd.uniregctb.evenement.fiscal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.data.DataEventSender;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;

/**
 * Implémentation du sender d'événements fiscaux qui collecte les événements envoyés par la partie WEB
 * et en délègue l'émission à la partie WS en passant par un message "data"
 */
public class DelegatingEvenementFiscalSender implements EvenementFiscalSender, ModificationSubInterceptor, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingEvenementFiscalSender.class);

	private final ThreadLocal<List<EvenementFiscal>> evenementsCollectes = ThreadLocal.withInitial(LinkedList::new);

	private ModificationInterceptor parent;
	private boolean enabled;
	private DataEventSender dataEventSender;

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setDataEventSender(DataEventSender dataEventSender) {
		this.dataEventSender = dataEventSender;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		if (!enabled) {
			LOGGER.info(String.format("Evénements fiscaux désactivés : l'événement fiscal %d n'est pas envoyé.", evenement.getId()));
			return;
		}

		// on collecte et on enverra ça à la fin...
		evenementsCollectes.get().add(evenement);
	}

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
		// rien ici, on attend la fin de la transaction
		return false;
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien ici non plus, on attend la fin de la transaction
	}

	@Override
	public void preTransactionCommit() {
		// ici, si on a des événements fiscaux collectés, il faut les envoyer au travers d'un événement DATA
		final List<EvenementFiscal> toSend = evenementsCollectes.get();
		if (!toSend.isEmpty()) {
			final List<Long> ids = toSend.stream()
					.map(EvenementFiscal::getId)
					.collect(Collectors.toCollection(() -> new ArrayList<>(toSend.size())));

			// envoi, par le canal des DATA events, de la liste des identifiants des événements fiscaux à envoyer
			dataEventSender.sendEvenementsFiscaux(ids);
		}
	}

	@Override
	public void postTransactionCommit() {
		// cleanup
		evenementsCollectes.remove();
	}

	@Override
	public void postTransactionRollback() {
		// cleanup
		evenementsCollectes.remove();
	}
}
