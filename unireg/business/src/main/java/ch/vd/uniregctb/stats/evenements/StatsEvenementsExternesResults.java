package ch.vd.uniregctb.stats.evenements;

import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StatsEvenementsExternesResults {

	public static class EvenementExterneErreur implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "MESSAGE" };

		public final long id;
		public final String message;

		public EvenementExterneErreur(long id, String message) {
			this.id = id;
			this.message = message;
		}

		public String[] getNomsColonnes() {
			return COLONNES;
		}

		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), message };
		}
	}

	private final Map<EtatEvenementExterne, Integer> etats;
	private final List<EvenementExterneErreur> erreurs;

	public StatsEvenementsExternesResults(Map<EtatEvenementExterne, Integer> etats, List<EvenementExterneErreur> erreurs) {
		this.etats = etats != null ? Collections.unmodifiableMap(etats) : Collections.<EtatEvenementExterne, Integer>emptyMap();
		this.erreurs = erreurs != null ? Collections.unmodifiableList(erreurs) : Collections.<EvenementExterneErreur>emptyList();
	}

	public Map<EtatEvenementExterne, Integer> getEtats() {
		return etats;
	}

	public List<EvenementExterneErreur> getErreurs() {
		return erreurs;
	}
}
