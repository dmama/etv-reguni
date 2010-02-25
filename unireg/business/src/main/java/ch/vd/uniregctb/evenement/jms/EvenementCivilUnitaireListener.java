package ch.vd.uniregctb.evenement.jms;

import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument.EvtRegCivil;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
import ch.vd.uniregctb.evenement.engine.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.engine.EvenementCivilRegrouper;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Message driven Pojo : en charge de la persistence des événements civils unitaires reçus au format XML.
 *
 * @author Jean-Eric Cuendet
 */
public class EvenementCivilUnitaireListener extends EsbMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireListener.class);

	private EvenementCivilUnitaireDAO evenementCivilUnitaireDAO;
	private EvenementCivilRegrouper evenementCivilRegrouper;
	private EvenementCivilProcessor evenementCivilProcessor;
	private PlatformTransactionManager transactionManager;

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		AuthenticationHelper.setPrincipal("JMS-EvtCivil");

		try {
			final String message = esbMessage.getBodyAsString();
			onMessage(message);
		}
		catch (EvenementUnitaireException e) {
			// on a un truc qui a sauté au moment de l'insertion de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(esbMessage, e.getMessage(), e, ErrorType.UNKNOWN, "");
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.resetAuthentication();
		}
	}

	/**
	 * Traite le message XML reçu pour en extraire les informations de l'événement civil et les persister en base.
	 * La methode onMessage() ne doit pas être appelée explicitement.
	 * Seul le mécanisme JMS doit l'appeler
	 *
	 * @param message message XML décrivant l'évènement civil unitaire
	 */
	private void onMessage(String message) throws EvenementUnitaireException {
		
		// D'abord on insére l'événement
		final Long id = insertEvenementUnitaire(message);

		if (id != null) {
			// Ensuite on regroupe
			long ret = -1L;
			try {
				ret = evenementCivilRegrouper.regroupeUnEvenementById(id, null);
			}
			catch (Exception e) {
				/*
				 * On catche l'exception et on ne FAIT RIEN!
				 * Le regroupement peut failer mais la consommation du message JMS ne doit pas être impactée.
				 * L'Etat de l'événement civil REGROUPE est mis à ERREUR.
				 */
				LOGGER.error(e, e);
			}

			if (ret > 0) {
				try {
					evenementCivilProcessor.traiteEvenementCivilRegroupe(ret);
				}
				catch (Exception e) {
					/*
					 * On catche l'exception et on ne FAIT RIEN!
					 * Le regroupement peut failer mais la consommation du message JMS ne doit pas être impactée.
					 * L'Etat de l'événement civil REGROUPE est mis à ERREUR.
					 */
					LOGGER.error(e, e);
				}
			}
		}

	}

	private class InsertionEvenementUnitaireTransactionCallback implements TransactionCallback  {

		final String xmlMessage;

		private InsertionEvenementUnitaireTransactionCallback(String xmlMessage) {
			this.xmlMessage = xmlMessage;
		}

		/**
		 * Renvoie l'identifiant technique de l'événement unitaire inséré
		 * @param status
		 * @return
		 */
		public Long doInTransaction(TransactionStatus status) {

			/* On parse le message XML */
			EvtRegCivilDocument doc;
			try {
				doc = EvtRegCivilDocument.Factory.parse(xmlMessage);
			}
			catch (XmlException e) {
				LOGGER.warn("Le message suivant n'est pas un document XML valide:\n" + xmlMessage, e);
				throw new RuntimeException("Message invalide", e);
			}
			final EvtRegCivil bean = doc.getEvtRegCivil();

			// filtrage des événements que l'on ne connait pas ou que l'on connait
			// mais que l'on ne traite pas...
			final long id = bean.getNoTechnique();
			final TypeEvenementCivil type = TypeEvenementCivil.valueOf(bean.getCode());
			if (type == null || type.isIgnore()) {
				Audit.info(id, String.format("Arrivée d'un message JMS ignoré (id %d, code %d)", id, bean.getCode()));
				return null;
			}
			else {

				Audit.info(id, "Arrivée du message JMS avec l'id " + id);

				/*
				 * Si l'événement se trouve déja dans la base, on log la duplication est on ne fait rien.
				 *
				 * Par définition, un événement identifié par un id est immutable: s'il est dans la base, il a déjà été traité et on ne peut rien
				 * faire de plus. Recevoir plus d'un fois un même événement ne doit cependant pas être traité comme une erreur grave (= pas
				 * d'exception). Car dans la pratique il est possible que ce cas de figure se produise lorsque le gateway des événements est
				 * rebooté.
				 */
				if (!evenementCivilUnitaireDAO.exists(id)) {

					// Sauvegarde du nouvel événement en base de donnée
					final EvenementCivilUnitaire evt = new EvenementCivilUnitaire();
					evt.setId(id);
					evt.setType(type);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu((long) bean.getNoIndividu());
					evt.setDateEvenement(RegDate.get(bean.getDateEvenement().getTime()));
					evt.setNumeroOfsCommuneAnnonce(bean.getNumeroOFS());

					// Checks
					checkNotNull(id, evt.getId(), "L'ID de l'événement ne peut pas être nul");
					checkNotNull(id, evt.getDateEvenement(), "La date de l'événement ne peut pas être nulle");
					checkNotNull(id, evt.getNumeroIndividu(), "Le numéro d'individu de l'événement ne peut pas être nul");

					evenementCivilUnitaireDAO.save(evt);

					final StringBuilder b = new StringBuilder();
					b.append("L'événement unitaire ").append(id).append(" est inséré en base de données.");
					b.append(" ID=").append(evt.getId());
					b.append(" Type=").append(evt.getType());
					b.append(" Date=").append(RegDateHelper.dateToDashString(evt.getDateEvenement()));
					b.append(" No individu=").append(evt.getNumeroIndividu());
					b.append(" OFS commune=").append(evt.getNumeroOfsCommuneAnnonce());
					Audit.info(id, b.toString());
				}
				else { // L'Evt existe deja
					Audit.warn(id, String.format("L'événement unitaire %d existe DEJA en DB", id));
				}
				return id;
			}
		}
	}

	private Long insertEvenementUnitaire(final String message) throws EvenementUnitaireException {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

		try {
			return (Long) template.execute(new InsertionEvenementUnitaireTransactionCallback(message));
		}
		catch (Exception e) {
			throw new EvenementUnitaireException(e);
		}
	}

	/**
	 * Cette méthode peut être appelée depuis n'importe quel code qui a placé un User d'Audit
	 *
	 * @param message le contenu XML du message
	 * @param errorMsg message renseigné en cas d'erreur (out)
	 * @return <i>vrai</i> si le traitement s'est bien passé; <i>faux</i> si le message a été ignoré ou est parti en erreur.
	 */
	protected boolean insertRegroupeAndTraite(String message, StringBuffer errorMsg) {

		// D'abord on insére l'evenement
		boolean ok = true;
		Long idU = null;
		try {
			idU = insertEvenementUnitaire(message);
			if (idU == null) {
				errorMsg.append("Message ignoré");
				ok = false;
			}
		}
		catch (Exception e) {
			errorMsg.append(e.getMessage());
			ok = false;
		}

		// Ensuite on regroupe
		if (ok) {
			long idR = -1L;
			try {
				idR = evenementCivilRegrouper.regroupeUnEvenementById(idU, errorMsg);
				if (idR < 0) {
					ok = false;
					if (errorMsg.length() == 0) {
						errorMsg.append("Probleme lors du regroupement de l'evenement unitaire ").append(idU);
					}
				}
			}
			catch (Exception e) {
				errorMsg.append(e.getMessage());
				ok = false;
			}

			if (ok) {
				try {
					evenementCivilProcessor.traiteEvenementCivilRegroupe(idR);
				}
				catch (Exception e) {
					errorMsg.append(e.getMessage());
					ok = false;
				}
			}
		}

		return ok;
	}

	private void checkNotNull(long id, Object o, String message) {
		if (o == null) {
			Audit.error(id, message);
			Assert.notNull(o, message);
		}
	}

	/**
	 * Setter pour la couche d'accès aux évènements civils unitaires.
	 *
	 * @param evenementCivilUnitaireDAO
	 *            couche d'accès aux évènements civils unitaires.
	 */
	public void setEvenementCivilUnitaireDAO(EvenementCivilUnitaireDAO evenementCivilUnitaireDAO) {
		this.evenementCivilUnitaireDAO = evenementCivilUnitaireDAO;
	}

	public void setEvenementCivilRegrouper(EvenementCivilRegrouper evenementCivilRegrouper) {
		this.evenementCivilRegrouper = evenementCivilRegrouper;
	}

	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
