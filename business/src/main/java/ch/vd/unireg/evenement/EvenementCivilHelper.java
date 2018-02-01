package ch.vd.unireg.evenement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.type.TypeEvenementErreur;

public abstract class EvenementCivilHelper {

	private static final class EvenementCivilKey {

		private final String message;
		private final TypeEvenementErreur type;
		private final String callstack;

		private EvenementCivilKey(EvenementErreur erreur) {
			this.message = erreur.getMessage();
			this.type = erreur.getType();
			this.callstack = erreur.getCallstack();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EvenementCivilKey that = (EvenementCivilKey) o;

			if (type != that.type) return false;
			if (message != null ? !message.equals(that.message) : that.message != null) return false;
			if (callstack != null ? !callstack.equals(that.callstack) : that.callstack != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = message != null ? message.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (callstack != null ? callstack.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Elimination des doublons de messages d'erreur
	 * @param source liste produite par le traitement
	 * @param <T> type du message d'erreur
	 * @return liste épurée (la liste initiale n'est pas modifiée)
	 */
	public static <T extends EvenementErreur> List<T> eliminerDoublons(List<T> source) {
		if (source == null || source.size() < 2) {
			return source;
		}
		final Map<EvenementCivilKey, T> map = new LinkedHashMap<>(source.size());
		for (T src : source) {
			final EvenementCivilKey key = new EvenementCivilKey(src);
			if (!map.containsKey(key)) {
				map.put(key, src);
			}
		}
		if (map.size() < source.size()) {
			return new ArrayList<>(map.values());
		}
		else {
			return source;
		}
	}
}
