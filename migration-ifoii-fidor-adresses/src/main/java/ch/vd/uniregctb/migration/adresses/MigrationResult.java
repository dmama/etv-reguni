package ch.vd.uniregctb.migration.adresses;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

abstract class MigrationResult {

	private static final String RUE = "RUE";
	private static final String NUMERO_ORDRE_POSTE = "NUMERO_ORDRE_POSTE";
	private static final String NUMERO_RUE = "NUMERO_RUE";
	private static final String NUMERO_MAISON = "NUMERO_MAISON";

	protected final DataAdresse data;

	MigrationResult(DataAdresse data) {
		this.data = data;
	}

	public abstract Map<String, Object> getFieldModifications();

	private static String enquote(String str) {
		if (str == null) {
			return "null";
		}
		return String.format("'%s'", str);
	}

	static class Ok extends MigrationResult {
		protected final Integer estrid;
		protected final Integer numeroOrdrePoste;
		protected final String numeroMaison;

		Ok(DataAdresse data, Integer estrid, Integer numeroOrdrePoste, String numeroMaison) {
			super(data);
			this.estrid = estrid;
			this.numeroOrdrePoste = numeroOrdrePoste;
			this.numeroMaison = numeroMaison;
		}

		@Override
		public Map<String, Object> getFieldModifications() {
			final Map<String, Object> map = new LinkedHashMap<>(4);
			map.put(NUMERO_RUE, estrid);
			if (StringUtils.isNotBlank(numeroMaison)) {
				map.put(NUMERO_MAISON, StringUtils.trim(numeroMaison));
			}
			map.put(NUMERO_ORDRE_POSTE, numeroOrdrePoste);
			map.put(RUE, null);
			return map;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" + data + " -> {" +
					"estrid=" + estrid +
					", numeroOrdrePoste=" + numeroOrdrePoste +
					", numeroMaison=" + enquote(numeroMaison) +
					"}}";
		}
	}

	static class NotFound extends MigrationResult {
		protected final Integer numeroOrdrePoste;
		protected final String rue;

		NotFound(DataAdresse data, Integer numeroOrdrePoste, String rue) {
			super(data);
			this.numeroOrdrePoste = numeroOrdrePoste;
			this.rue = rue;
		}

		@Override
		public Map<String, Object> getFieldModifications() {

			// ici, il peut y avoir plusieurs cas :
			// 1. on avait un numéro de rue avant
			//      -> il faut l'effacer et placer le libellé de la rue dans la colonne "RUE" et le numéro postal de la localité dans "NUMERO_ORDRE_POSTE"
			// 2. on n'avait pas de numéro de rue avant
			//      -> on ne touche à rien ?
			final Map<String, Object> map;
			if (data.noRue != null) {
				map = new LinkedHashMap<>(3);
				map.put(NUMERO_RUE, null);

				// je ne sais pas quoi faire...
				map.put(RUE, StringUtils.isBlank(rue) ? "Rue inconnue" : rue);
				map.put(NUMERO_ORDRE_POSTE, numeroOrdrePoste);
			}
			else {
				map = Collections.emptyMap();
			}
			return map;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" + data + " -> {" +
					"numeroOrdrePoste=" + numeroOrdrePoste +
					", rue=" + enquote(rue) +
					"}}";
		}
	}

	static class LocalityNotFound extends NotFound {
		LocalityNotFound(DataAdresse data, Integer numeroOrdrePoste, String rue) {
			super(data, numeroOrdrePoste, rue);
		}
	}

	static class Erreur extends NotFound {
		protected final Exception e;

		Erreur(DataAdresse data, Integer numeroOrdrePoste, String rue, Exception e) {
			super(data, numeroOrdrePoste, rue);
			this.e = e;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" + data + " -> {" +
					"numeroOrdrePoste=" + numeroOrdrePoste +
					", rue=" + enquote(rue) +
					", e=" + e +
					"}}";
		}
	}
}
