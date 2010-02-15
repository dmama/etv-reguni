package ch.vd.uniregctb.evenement.jms;

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
	 * La methode onMessage() ne doit être appelée explicitement
	 * Seul le mechanisme JMS doit l'appeler
	 *
	 * @param message
	 *            message XML décrivant l'évènement civil unitaire
	 */
	private void onMessage(String message) {
		
		// D'abord on insére l'evenement
		final long id = insertEvenementUnitaire(message);

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

	private long insertEvenementUnitaire(final String message) {

		TransactionCallback callback = new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				/* On parse le message XML */
				EvtRegCivilDocument doc;
				try {
					doc = EvtRegCivilDocument.Factory.parse(message);
				}
				catch (XmlException e) {
					LOGGER.warn("Le message suivant n'est pas un document XML valide:\n" + message, e);
					throw new RuntimeException(e);
				}
				EvtRegCivil bean = doc.getEvtRegCivil();

				Audit.info(bean.getNoTechnique(), "Arrivée du message JMS avec l'id " + bean.getNoTechnique());

				/*
				 * Si l'événement se trouve déja dans la base, on log la duplication est on ne fait rien.
				 *
				 * Par définition, un événement identifié par un id est immutable: s'il est dans la base, il a déjà été traité et on ne peut rien
				 * faire de plus. Recevoir plus d'un fois un même événement ne doit cependant pas être traité comme une erreur grave (= pas
				 * d'exception). Car dans la pratique il est possible que ce cas de figure se produise lorsque le gateway des événements est
				 * rebooté.
				 */
				long id = bean.getNoTechnique();
				if (!evenementCivilUnitaireDAO.exists(id)) {

					/* Sauvegarde du nouvel événement en base de donnée */
					EvenementCivilUnitaire evt = new EvenementCivilUnitaire();
					evt.setId(id);
					evt.setType( TypeEvenementCivil.valueOf(bean.getCode()) );
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu((long) bean.getNoIndividu());
					evt.setDateEvenement(RegDate.get(bean.getDateEvenement().getTime()));
					evt.setNumeroOfsCommuneAnnonce(bean.getNumeroOFS());

					// Checks
					checkNotNull(evt.getId(), "L'ID de l'événement ne peut pas être null");
					checkNotNull(evt.getType(), "Le type de l'événement ("+bean.getCode()+") ne peut pas être null");
					checkNotNull(evt.getDateEvenement(), "La date de l'événement ne peut pas être null");
					checkNotNull(evt.getNumeroIndividu(), "Le numéro d'individu de l'événement ne peut pas être null");

					evenementCivilUnitaireDAO.save(evt);

					String msg = "L'Evenement unitaire " + id + " est inséré en base de données.";
					msg += " Id=" + evt.getId();
					msg += " Type=" + evt.getType();
					msg += " Date=" + RegDateHelper.dateToDashString(evt.getDateEvenement());
					msg += " No individu=" + evt.getNumeroIndividu();
					msg += " OFS commune=" + evt.getNumeroOfsCommuneAnnonce();
					Audit.info(id, msg);
				}
				else { // L'Evt existe deja
					Audit.warn(id, "L'Evenement unitaire "+id+" existe DEJA en DB");
				}
				return id;
			}
		};

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		return (Long)template.execute(callback);
	}

	/**
	 * Cette méthode peut être appelée depuis n'importe quel code qui a placé un User d'Audit
	 *
	 * @param message le contenu XML du message
	 * @param errorMsg message renseigné en cas d'erreur (out)
	 * @return <i>vrai</i> si le traitement s'est bien passé; <i>faux</i> autrement.
	 */
	protected boolean insertRegroupeAndTraite(String message, StringBuffer errorMsg) {

		// D'abord on insére l'evenement
		boolean ok = true;
		long idU = -1L;
		try {
			idU = insertEvenementUnitaire(message);
			if (idU < 0) {
				errorMsg.append("Probleme lors de l'insertion du message unitaire ").append(idU);
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

	private void checkNotNull(Object o, String message) {
		if (o == null) {
			Audit.error(message);
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
