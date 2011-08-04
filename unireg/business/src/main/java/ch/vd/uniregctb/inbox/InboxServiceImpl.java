package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Service de gestion des inbox des utilisateurs. Encore une fois : les inbox ne sont pas persistées
 * lors d'un redémarrage de l'application.
 */
public class InboxServiceImpl implements InboxService, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(InboxServiceImpl.class);

	/**
	 * Contenu de l'inbox
	 */
	private final InboxContainer container = new InboxContainer();

	/**
	 * Timer utilisé pour le nettoyage des vieux éléments
	 */
	private Timer cleaningTimer;

	/**
	 * Nettoyeur des éléments qui ont expiré
	 */
	private final class Nettoyeur extends TimerTask {
		@Override
		public void run() {
			// On passe en revue le contenu de toutes les inbox existantes
			// si un élément a expiré, il saute.
			container.cleanup(true);
		}
	}

	@Override
	public List<InboxElement> getInboxContent(String visa) {
		return container.getInboxContent(visa);
	}

	@Override
	public InboxElement getInboxElement(UUID uuid) {
		return container.get(uuid);
	}

	@Override
	public void addDocument(String visa, String docName, String description, InboxAttachment attachment, int hoursUntilExpiration) throws IOException {
		final InboxElement element = new InboxElement(docName, description, attachment, TimeUnit.HOURS.toMillis(hoursUntilExpiration));
		addElement(visa, element);
	}

	@Override
	public void addDocument(UUID uuid, String visa, String docName, String description, InboxAttachment attachment, int hoursUntilExpiration) throws IOException {
		final InboxElement element = new InboxElement(uuid, docName, description, attachment, TimeUnit.HOURS.toMillis(hoursUntilExpiration));
		addElement(visa, element);
	}

	@Override
	public void removeDocument(UUID uuid, String visa) {
		container.removeElement(uuid, visa);
	}

	/**
	 * Ajout d'un document déjà formé à l'inbox du visa donné
	 * @param visa visa dont on doit utiliser l'inbox
	 * @param element élément arrivé
	 */
	protected void addElement(String visa, InboxElement element) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Arrivée d'un nouveau document dans l'inbox de l'utilisateur %s : %s", visa, element));
		}
		container.addElement(visa, element);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// on lance le timer du nettoyeur
		final long p = getCleaningPeriod();
		if (p > 0) {
			cleaningTimer = new Timer("InboxCleaner");
			cleaningTimer.schedule(new Nettoyeur(), p, p);
		}
	}

	/**
	 * A surcharger pour les tests unitaires. La période par défaut est de 10 minutes. Si cette méthode renvoie 0 ou négatif, le nettoyeur n'est pas démarré.
	 * @return la valeur de la période de nettoyage des éléments expirés, en millisecondes
	 */
	protected long getCleaningPeriod() {
		return TimeUnit.MINUTES.toMillis(10);
	}

	@Override
	public void destroy() throws Exception {
		// on arrête le nettoyeur
		if (cleaningTimer != null) {
			cleaningTimer.cancel();
		}

		// on efface tout
		container.cleanup(false);
	}
}
