package ch.vd.uniregctb.evenement.civil.externe.jms;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument.EvtRegCivil;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilAsyncProcessor;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Listener des évéments civils envoyés par le registre civil (REG-PP ou RCPers) et reçus à travers l'ESB.
 */
public class EvenementCivilListener extends EsbMessageEndpointListener implements ErrorMonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilListener.class);

	private static final String DEFAULT_BUSINESS_USER = "JMSEvtCivil-SansVisa";

	private EvenementCivilExterneDAO evenementCivilExterneDAO;
	private EvenementCivilAsyncProcessor evenementCivilAsyncProcessor;

	private final AtomicInteger nombreMessagesRecus = new AtomicInteger(0);
	private final AtomicInteger nombreMessagesRenvoyesEnErreur = new AtomicInteger(0);
	private final AtomicInteger nombreMessagesRenvoyesEnException = new AtomicInteger(0);

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		nombreMessagesRecus.incrementAndGet();

		try {
			final String message = esbMessage.getBodyAsString();
			final String visaMutation = StringUtils.trimToNull(esbMessage.getBusinessUser());

			// le user de création est initialisé avec le user à l'origine de l'événement civil
			AuthenticationHelper.setPrincipal(visaMutation != null ? visaMutation : DEFAULT_BUSINESS_USER);
			try {
				onEvenementCivil(message);
			}
			finally {
				AuthenticationHelper.resetAuthentication();
			}
		}
		catch (EvenementCivilException e) {
			// on a un truc qui a sauté au moment de l'arrivée de l'événement (test métier)
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			nombreMessagesRenvoyesEnErreur.incrementAndGet();
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(esbMessage, e.getMessage(), e, ErrorType.UNKNOWN, "");
		}
		catch (RuntimeException e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			nombreMessagesRenvoyesEnException.incrementAndGet();
			LOGGER.error(e, e);
			throw e;
		}
	}

	@Override
	public int getNombreMessagesRecus() {
		return nombreMessagesRecus.intValue();
	}

	@Override
	public int getNombreMessagesRenvoyesEnErreur() {
		return nombreMessagesRenvoyesEnErreur.intValue();
	}

	@Override
	public int getNombreMessagesRenvoyesEnException() {
		return nombreMessagesRenvoyesEnException.intValue();
	}

	/**
	 * Transforme le body du message XML en événement civil et demande son traitement par l'application
	 * @param message body du message XML reçu
	 * @return <code>true</code> si l'événement a été posté pour traitement, <code>false</code> si ce n'est pas la peine
	 * @throws EvenementCivilException en cas de problème
	 */
	protected boolean onEvenementCivil(String message) throws EvenementCivilException {

		final long start = System.nanoTime();

		final EvenementCivilExterne evenement = extractEvenement(message);
		if (evenement == null) {
			return false; // rien à faire
		}

		final long extraction = System.nanoTime();

		// on insère l'événement dans la base de données (status = à traiter)
		if (!insertEvenement(evenement)) {
			return false; // rien de plus à faire
		}

		final long insertion = System.nanoTime();

		// on poste une demande de traitement pour cet événement
		evenementCivilAsyncProcessor.postEvenementCivil(evenement.getId());

		final long post = System.nanoTime();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Evénement reçu en %d ms (extraction %d ms, insertion %d ms)", (post - start) / 1000000, (extraction - start) / 1000000, (insertion - extraction) / 1000000));
		}

		return true;
	}

	/**
	 * Méthode qui attend que tous les événements postés soient traités
	 * @throws InterruptedException en cas d'interruption forcée de l'attente
	 */
	protected void sync() throws InterruptedException {
		evenementCivilAsyncProcessor.sync();
	}

	private EvenementCivilExterne extractEvenement(String xmlMessage) throws EvenementCivilException {

		/* On parse le message XML */
		EvtRegCivilDocument doc;
		try {
			doc = EvtRegCivilDocument.Factory.parse(xmlMessage);
		}
		catch (XmlException e) {
			LOGGER.warn("Le message suivant n'est pas un document XML valide:\n" + xmlMessage, e);
			throw new EvenementCivilException("Message XML invalide", e);
		}
		final EvtRegCivil bean = doc.getEvtRegCivil();

		// filtrage des événements que l'on ne connait pas ou que l'on connait mais que l'on ne traite pas...
		final TypeEvenementCivil type = TypeEvenementCivil.valueOf(bean.getCode());
		if (type == null || type.isIgnore()) {
			Audit.info(bean.getNoTechnique(), String.format("Arrivée d'un message JMS ignoré (id %d, code %d)", bean.getNoTechnique(), bean.getCode()));
			return null;
		}

		final EvenementCivilExterne evenement = new EvenementCivilExterne(bean);
		if (evenement.getId() == null) {
			throw new EvenementCivilException("L'ID de l'événement ne peut pas être nul");
		}
		if (evenement.getDateEvenement() == null) {
			throw new EvenementCivilException("La date de l'événement ne peut pas être nulle");
		}
		if (evenement.getNumeroIndividuPrincipal() == null) {
			throw new EvenementCivilException("Le numéro d'individu de l'événement ne peut pas être nul");
		}
		return evenement;
	}

	private boolean insertEvenement(final EvenementCivilExterne evenement) {

		final Long id = evenement.getId();
		Audit.info(id, "Arrivée du message JMS avec l'id " + id);

		final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

		final boolean ok = template.execute(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction(TransactionStatus status) {

				if (evenementCivilExterneDAO.exists(id)) {
					Audit.warn(id, String.format("L'événement civil n°%d existe DEJA en DB", id));
					return false; // rien de plus à faire
				}

				evenementCivilExterneDAO.save(evenement);

				final StringBuilder b = new StringBuilder();
				b.append("L'événement civil ").append(id).append(" est inséré en base de données {");
				b.append("id=").append(id);
				b.append(", type=").append(evenement.getType());
				b.append(", date=").append(RegDateHelper.dateToDisplayString(evenement.getDateEvenement()));
				b.append(", no individu=").append(evenement.getNumeroIndividuPrincipal());
				b.append(", OFS commune=").append(evenement.getNumeroOfsCommuneAnnonce()).append("}.");
				Audit.info(id, b.toString());

				return true;
			}
		});

		return ok;
	}

	public void setEvenementCivilExterneDAO(EvenementCivilExterneDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}

	public void setEvenementCivilAsyncProcessor(EvenementCivilAsyncProcessor evenementCivilAsyncProcessor) {
		this.evenementCivilAsyncProcessor = evenementCivilAsyncProcessor;
	}
}
