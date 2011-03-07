package ch.vd.uniregctb.inbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
	private final Map<UUID, InboxElement> byUuid = new HashMap<UUID, InboxElement>();

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
		byUuid.put(element.getUuid(), element);
		final Set<InboxElement> set = getUserSet(visa, true);
		set.add(element);
	}

	/**
	 * @param visa visa identifiant l'inbox dont le contenu doit être renvoyé
	 * @return contenu de l'inbox identifiée par le visa donnée, trié dans l'ordre naturel des éléments
	 */
	public List<InboxElement> getInboxContent(String visa) {
		final List<InboxElement> liste;
		synchronized (this) {
			final Set<InboxElement> set = getUserSet(visa, false);
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
		return byUuid.get(uuid);
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
	private Set<InboxElement> getUserSet(String visa, boolean createIfNecessary) {
		Set<InboxElement> set = byUser.get(visa);
		if (set == null && createIfNecessary) {
			set = new HashSet<InboxElement>();
			byUser.put(visa, set);
		}
		return set;
	}
}
