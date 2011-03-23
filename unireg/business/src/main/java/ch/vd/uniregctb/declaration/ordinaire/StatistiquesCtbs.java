package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.tiers.Contribuable;

public class StatistiquesCtbs extends JobResults<Long, StatistiquesCtbs> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	/**
	 * Version spécialisé pour les statistiques des contribuables.
	 */
	public enum TypeContribuable {
		VAUDOIS_ORDINAIRE("ordinaire"),
		VAUDOIS_DEPENSE("à la dépense"),
		HORS_CANTON("hors canton"),
		HORS_SUISSE("hors suisse"),
		SOURCIER_PUR("sourcier");

		private String description;

		private TypeContribuable(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Key implements Comparable<Key> {

		public final Integer oid;
		public final Commune commune;
		public final TypeContribuable typeCtb;

		public Key(Integer oid, Commune commune, TypeContribuable typeCtb) {
			this.oid = oid;
			this.commune = commune;
			this.typeCtb = typeCtb;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((commune == null) ? 0 : commune.getNoOFSEtendu());
			result = prime * result + ((oid == null) ? 0 : oid.hashCode());
			result = prime * result + ((typeCtb == null) ? 0 : typeCtb.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Key key = (Key) o;

			if (oid != null ? !oid.equals(key.oid) : key.oid != null) return false;
			if (typeCtb != key.typeCtb) return false;

			if (commune == key.commune) {
				return true;
			}
			if (commune == null || key.commune == null) {
				return false;
			}
			return commune.getNoOFSEtendu() == key.commune.getNoOFSEtendu();
		}

		/**
		 * Ordre de tri naturel: oid, commune et typeCtb.
		 */
		public int compareTo(Key o) {

			if (oid != o.oid) {
				if (oid == null) {
					return 1;
				}
				if (o.oid == null) {
					return -1;
				}
				return oid - o.oid;
			}

			if ((commune == null && o.commune != null) || (commune != null && o.commune == null) || (commune != null && o.commune != null && commune.getNoOFSEtendu() != o.commune.getNoOFSEtendu())) {
				final String nomCommune1 = commune != null ? commune.getNomMinuscule() : "";
				final String nomCommune2 = o.commune != null ? o.commune.getNomMinuscule() : "";
				return nomCommune1.compareTo(nomCommune2);
			}

			if (typeCtb == null) {
				return 1;
			}
			if (o.typeCtb == null) {
				return -1;
			}
			return typeCtb.compareTo(o.typeCtb);
		}

	}

	public final class Value {
		public int nombre;
	}

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public boolean interrompu;

	// Données de processing
	public int nbCtbsTotal = 0;
	public List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public Map<Key, Value> stats = new HashMap<Key, Value>();

	public StatistiquesCtbs(int annee, RegDate dateTraitement) {
		this.annee = annee;
		this.dateTraitement = dateTraitement;
	}

	public void addStats(Integer oid, Commune commune, TypeContribuable typeCtb) {
		Key key = new Key(oid, commune, typeCtb);
		Value value = stats.get(key);
		if (value == null) {
			value = new Value();
			stats.put(key, value);
		}
		value.nombre++;
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		Long numero = (ctb == null ? null : ctb.getNumero());
		Integer officeImpotId = (ctb == null ? null : ctb.getOfficeImpotId());
		ctbsEnErrors.add(new Erreur(numero, officeImpotId, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Long numero, Exception e) {
		ctbsEnErrors.add(new Erreur(numero, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addAll(StatistiquesCtbs rapport) {
		this.nbCtbsTotal += rapport.nbCtbsTotal;
		this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);

		for (Map.Entry<Key, Value> e : rapport.stats.entrySet()) {
			mergeStats(e.getKey(), e.getValue());
		}
	}

	public void mergeStats(Key rightKey, Value rightValue) {
		Value value = stats.get(rightKey);
		if (value == null) {
			value = new Value();
			stats.put(rightKey, value);
		}
		value.nombre += rightValue.nombre;
	}
}
