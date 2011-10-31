package ch.vd.uniregctb.inbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Pair;

/**
 * Classe qui maintient le contenu des inbox dont l'InboxService est dépositaire ; cette classe est thread-safe
 * @see InboxService
 */
public class InboxContainer {

	public static final Logger LOGGER = Logger.getLogger(InboxContainer.class);

	/**
	 * Map des éléments indexés par le visa de l'utilisateur propriétaire
	 */
	private final Map<String, Set<InboxElement>> byUser = new HashMap<String, Set<InboxElement>>();

	/**
	 * Map de tous les éléments connus, indexés par leur identifiant
	 */
	private final Map<UUID, Pair<String, InboxElement>> byUuid = new HashMap<UUID, Pair<String, InboxElement>>();

	/**
	 * Listeners des opérations de gestion des inboxes
	 */
	private final List<InboxManagementListener> listeners = new LinkedList<InboxManagementListener>();

	/**
	 * Ajoute l'élément donné au contenu de l'inbox déterminée par le visa
	 * @param visa visa identifiant de l'inbox à modifier
	 * @param element élément à ajouter
	 * @throws IllegalArgumentException si l'élément est déjà connu
	 */
	public synchronized void addElement(String visa, InboxElement element) {
		if (byUuid.containsKey(element.getUuid())) {
			throw new IllegalArgumentException("Elément " + element.getUuid() + " déjà présent");
		}
		byUuid.put(element.getUuid(), new Pair<String, InboxElement>(visa, element));
		final Set<InboxElement> set = getUserRelativeSet(visa, true);
		set.add(element);
	}

	/**
	 * @param visa visa identifiant l'inbox dont le contenu doit être renvoyé
	 * @return contenu de l'inbox identifiée par le visa donnée, trié dans l'ordre naturel des éléments
	 */
	public List<InboxElement> getInboxContent(String visa) {
		final List<InboxElement> liste;
		synchronized (this) {
			final Set<InboxElement> set = getUserRelativeSet(visa, false);
			if (set == null || set.size() == 0) {
				liste = Collections.emptyList();
			}
			else {
				liste = new ArrayList<InboxElement>(set);
			}
		}
		Collections.sort(liste);
		return liste;
	}

	/**
	 * @param uuid identifiant du document à récupérer
	 * @return document récupéré correspondant à l'identifiant donné
	 */
	public synchronized InboxElement get(UUID uuid) {
		final Pair<String, InboxElement> elt = byUuid.get(uuid);
		return elt != null ? elt.getSecond() : null;
	}

	/**
	 * Efface un élément de la boîte de réception de son propriétaire
	 * @param uuid identifiant de l'élément à effacer
	 * @param visa propriétaire annoncé (pour contrôle)
	 */
	public synchronized void removeElement(UUID uuid, String visa) {
		final Pair<String, InboxElement> elt = byUuid.get(uuid);
		if (elt != null) {
			if (elt.getFirst().equals(visa)) {
				final Set<InboxElement> set = getUserRelativeSet(visa, false);
				final InboxElement inboxElement = elt.getSecond();
				if (set != null) {
					set.remove(inboxElement);
				}
				byUuid.remove(uuid);
				inboxElement.onDiscard();
			}
		}
	}

	/**
	 * Nettoyage du contenu
	 * @param onlyExpired si <code>true</code>, ne supprime que les éléments expirés, contre tous si <code>false</code>
	 */
	public synchronized void cleanup(boolean onlyExpired) {

		// boucle sur la liste indexée par visa en premier car il est plus
		// facile de retrouver les éléments dans l'autre map directement dans ce sens
		for (Map.Entry<String, Set<InboxElement>> entry : byUser.entrySet()) {
			final Set<InboxElement> content = entry.getValue();
			if (content != null && content.size() > 0) {
				final Iterator<InboxElement> iterator = content.iterator();
				while (iterator.hasNext()) {
					final InboxElement element = iterator.next();
					final boolean isExpired = element != null && element.isExpired();
					if (!onlyExpired || isExpired || element == null) {
						if (element != null) {
							element.onDiscard();
							byUuid.remove(element.getUuid());
						}
						iterator.remove();

						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(String.format("Discarded%s inbox element %s for user %s", (isExpired ? " expired" : StringUtils.EMPTY), element, entry.getKey()));
						}
					}
				}
			}
		}
	}

	/**
	 * Doit être appelé dans un contexte synchronisé
	 * @param visa le visa qui nous intéresse
	 * @param createIfNecessary <code>true</code> si un nouvel ensemble doit être créé s'il n'existe pas encore, <code>false</code> si la méthode peut retourner <code>null</code> dans ce cas
	 * @return l'ensemble des éléments connus de l'inbox du visa donné
	 */
	private Set<InboxElement> getUserRelativeSet(String visa, boolean createIfNecessary) {
		Set<InboxElement> set = byUser.get(visa);
		if (set == null && createIfNecessary) {
			set = new HashSet<InboxElement>();
			byUser.put(visa, set);
			notifyListenersOnNewInbox(visa);
		}
		return set;
	}

	/**
	 * Enregistrement d'un nouveau listener pour les opérations de créations/destruction (?) d'inboxes
	 * @param listener la nouvelle entité à notifier
	 * @param notifyOfExistingInboxes <code>true</code> s'il faut notifier ce nouveau listener de la "création" des inboxes déjà présentes
	 */
	public synchronized void registerInboxManagementListener(InboxManagementListener listener, boolean notifyOfExistingInboxes) {
		if (listener != null) {
			listeners.add(listener);

			if (notifyOfExistingInboxes) {
				for (String visa : byUser.keySet()) {
					notifyListenerOnNewInbox(listener, visa);
				}
			}
		}
	}

	/**
	 * Doit être appelé dans un contexte synchronisé
	 * @param visa visa de l'inbox qui vient d'être créée
	 */
	private void notifyListenersOnNewInbox(String visa) {
		for (InboxManagementListener listener : listeners) {
			notifyListenerOnNewInbox(listener, visa);
		}
	}

	/**
	 * Doit être appelé dans un contexte synchronisé
	 * @param listener listener à notifier
	 * @param visa visa de l'inbox qui vient d'être créée
	 */
	private void notifyListenerOnNewInbox(InboxManagementListener listener, String visa) {
		try {
			listener.onNewInbox(visa);
		}
		catch (Exception e) {
			LOGGER.warn("La notification du listener " + listener + " a échoué", e);
		}
	}
}
