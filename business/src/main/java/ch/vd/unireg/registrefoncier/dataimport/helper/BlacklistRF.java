package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.registrefoncier.BlacklistEntryRF;

/**
 * Copie en mémoire de tous les éléments RF blacklistés.
 */
public class BlacklistRF {

	private static class Key {
		@NotNull
		private final TypeEntiteRF type;
		@NotNull
		private final String idRF;

		public Key(@NotNull TypeEntiteRF type, @NotNull String idRF) {
			this.type = type;
			this.idRF = idRF;
		}

		public Key(@NotNull BlacklistEntryRF entry) {
			this.type = entry.getType();
			this.idRF = entry.getIdRF();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final Key key = (Key) o;
			return type == key.type &&
					Objects.equals(idRF, key.idRF);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, idRF);
		}
	}

	private final Map<Key, BlacklistEntryRF> map;

	public BlacklistRF(@NotNull Collection<BlacklistEntryRF> collection) {
		this.map = collection.stream().collect(Collectors.toMap(Key::new, l -> l));
	}

	/**
	 * @param type le type de l'entité RF
	 * @param idRF l'idRF d'une entité RF
	 * @return <b>vrai</b> si l'entité est blacklistée et doit être ignorée lors de l'import; <b>faux</b> autrement.
	 */
	public boolean isBlacklisted(@NotNull TypeEntiteRF type, @NotNull String idRF) {
		synchronized (map) {
			return map.containsKey(new Key(type, idRF));
		}
	}
}
