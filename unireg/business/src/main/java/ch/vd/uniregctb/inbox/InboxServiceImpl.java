package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
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
	 * Contenu de l'inbox, indexé par visa utilisateur
	 */
	private final Map<String, List<InboxElement>> content = new HashMap<String, List<InboxElement>>();

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
			cleanup(true);
		}
	}

	private void cleanup(boolean onlyExpired) {

		synchronized (content) {
			for (Map.Entry<String, List<InboxElement>> entry : content.entrySet()) {
				final List<InboxElement> content = entry.getValue();
				if (content != null && content.size() > 0) {
					final Iterator<InboxElement> iter = content.iterator();
					while (iter.hasNext()) {
						final InboxElement element = iter.next();
						final boolean isExpired = element != null && element.isExpired();
						if (!onlyExpired || isExpired || element == null) {
							iter.remove();
							if (element != null) {
								element.onDiscard();
							}

							if (LOGGER.isInfoEnabled()) {
								LOGGER.info(String.format("Discarded%s inbox element %s for user %s", (isExpired ? " expired" : StringUtils.EMPTY), element, entry.getKey()));
							}
						}
					}
				}
			}
		}
	}

	@Override
	public List<InboxElement> getContent(String visa) {
		final List<InboxElement> list;
		synchronized (content) {
			list = getListePourVisa(visa, true, false);
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public void addDocument(String visa, String docName, String description, String mimeType, InputStream document, int hoursUntilExpiration) throws IOException {
		final InboxElement element = new InboxElement(docName, mimeType, document, TimeUnit.HOURS.toMillis(hoursUntilExpiration));
		addElement(visa, element);
	}

	/**
	 * Ajout d'un document déjà formé à l'inbox du visa donné
	 * @param visa visa dont on doit utiliser l'inbox
	 * @param element élément arrivé
	 */
	protected void addElement(String visa, InboxElement element) {
		synchronized (content) {
			final List<InboxElement> liste = getListePourVisa(visa, false, true);
			liste.add(element);
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Arrivée d'un nouveau document dans l'inbox de l'utilisateur %s : %s", visa, element));
		}
	}

	/**
	 * Renvoie le contenu de l'inbox de l'utilisateur dont le visa est donné.<p/>
	 * Cette méthode <b>doit</b> être appelée dans un contexte synchronisé sur l'élément {@link #content} !
	 * @param visa le visa de l'utilisateur dont on veut récupérer la liste des éléments
	 * @param copie <code>true</code> si une copie (= un instantané) de la liste doit être renvoyée, <code>false</code> si on veut la liste originale
	 * @param createIfNecessary <code>true</code> si une nouvelle liste doit être créée s'il n'y en a pas, <code>false</code> si on doit renvoyer <code>null</code> dans ce cas
	 * @return la liste du contenu de l'inbox demandé
	 */
	private List<InboxElement> getListePourVisa(String visa, boolean copie, boolean createIfNecessary) {
		List<InboxElement> liste = content.get(visa);
		if (liste == null && createIfNecessary) {
			liste = new LinkedList<InboxElement>();
			content.put(visa, liste);
		}
		if (copie) {
			if (liste == null || liste.size() == 0) {
				liste = Collections.emptyList();
			}
			else {
				liste = new ArrayList<InboxElement>(liste);
			}
		}
		return liste;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// on lance le timer du nettoyeur
		final long p = getCleaningPeriod();
		if (p > 0) {
			cleaningTimer = new Timer("Inbox-Cleaner");
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
		cleanup(false);
	}
}
