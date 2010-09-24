package ch.vd.uniregctb.evenement.civil;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.jms.EvenementCivilSenderImpl;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe qui va lire un fichier log d'unireg pour en extraire les événements civils reçus
 * et les envoyer sur une queue de l'ESB définie.
 * <p/>
 * L'application se lance avec le nom du fichier en paramètre, le reste est en dur
 */
public class EvenementCivilRepeater {

	private static final String ESB_URL = "failover:(tcp://spip:50900)?useExponentialBackOff=false&reconnectDelay=30000&initialReconnectDelay=10000";
	private static final String ESB_USER = "smx";
	private static final String ESB_PWD = "smx";
	private static final String RAFT_STORE_URL = "http://raft-in.etat-de-vaud.ch/raft/store";
	private static final String QUEUE = "test.unireg.evtRegCivil";

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("Le nom du fichier de log à lire doit être donné en paramètre.");
		}

		System.out.println("Lecture du fichier de log '" + args[0] + "'.");
		final LogFileExtractor extractor = new LogFileExtractor(args[0]);
		final int size = extractor.getEvenementsRecus().size();
		if (size > 0) {

			// connexion à l'ESB
			final EsbMessageFactory messageFactory = new EsbMessageFactory();
			messageFactory.setValidator(null);

			final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ESB_USER, ESB_PWD, ESB_URL);
			final RaftEsbStore esbStore = new RaftEsbStore();
			esbStore.setEndpoint(RAFT_STORE_URL);

			final EsbJmsTemplate template = new EsbJmsTemplate(connectionFactory, esbStore);
			template.setApplication("unireg");
			template.setDomain("fiscalite");

			int index = 0;
			for (LogFileExtractor.EvenementRecu evt : extractor.getEvenementsRecus()) {

				final EvenementCivilData data = new EvenementCivilData(evt.id, evt.type, EtatEvenementCivil.A_TRAITER, evt.dateEvement, evt.noIndividu, null, null, null, evt.ofsAnnonce, null);

				final EsbMessage m = messageFactory.createMessage();
				m.setBusinessId(Long.toString(evt.id));
				m.setBusinessUser("Repeater");
				m.setBusinessCorrelationId(Long.toString(evt.id));
				m.setServiceDestination(QUEUE);
				m.setContext("evenementCivil");

				final EvtRegCivilDocument document = EvenementCivilSenderImpl.createDocument(data);
				final Node node = document.newDomNode();
				m.setBody((Document) node);

				template.send(m);

				if (++index % 100 == 0) {
					System.out.println("Envoyé " + index + " événements");
				}
			}

			System.out.println(index + " événements envoyés");
		}
		else {
			System.out.println("Aucun événement trouvé!");
		}
	}
}
