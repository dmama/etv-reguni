package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class StatistiquesDIs extends JobResults<Long, StatistiquesDIs> {

	public enum ErreurType {
		EXCEPTION("une exception est apparue pendant le traitement de la déclaration"); // ----------------

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

	public static class Key implements Comparable<Key> {

		public final int oid;
		public final TypeContribuable typeCtb;
		public final TypeEtatDeclaration etat;

		public Key(int oid, TypeContribuable typeCtb, TypeEtatDeclaration etat) {
			this.oid = oid;
			this.typeCtb = typeCtb;
			this.etat = etat;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((etat == null) ? 0 : etat.hashCode());
			result = prime * result + oid;
			result = prime * result + ((typeCtb == null) ? 0 : typeCtb.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (etat == null) {
				if (other.etat != null)
					return false;
			}
			else if (etat != other.etat)
				return false;
			if (oid != other.oid)
				return false;
			if (typeCtb == null) {
				if (other.typeCtb != null)
					return false;
			}
			else if (typeCtb != other.typeCtb)
				return false;
			return true;
		}

		/**
		 * Ordre de tri naturel: oid, typeCtb et etat.
		 */
		public int compareTo(Key o) {

			if (oid != o.oid) {
				return oid - o.oid;
			}

			if (typeCtb != o.typeCtb) {
				if (typeCtb == null) {
					return 1;
				}
				if (o.typeCtb == null) {
					return -1;
				}
				return typeCtb.compareTo(o.typeCtb);
			}

			return etat.compareTo(o.etat);
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
	public int nbDIsTotal = 0;
	public List<Erreur> disEnErrors = new ArrayList<Erreur>();
	public Map<Key, Value> stats = new HashMap<Key, Value>();

	public StatistiquesDIs(int annee, RegDate dateTraitement) {
		this.annee = annee;
		this.dateTraitement = dateTraitement;
	}

	public void addStats(int oid, TypeContribuable typeCtb, TypeEtatDeclaration etat) {
		Key key = new Key(oid, typeCtb, etat);
		Value value = stats.get(key);
		if (value == null) {
			value = new Value();
			stats.put(key, value);
		}
		value.nombre++;
	}

	public void addErrorException(DeclarationImpotOrdinaire di, Exception e) {
		final Tiers tiers = di.getTiers();
		final Long numero = (tiers == null ? null : tiers.getNumero());
		final Integer oid = (tiers == null ? null : tiers.getOfficeImpotId());
		disEnErrors.add(new Erreur(numero, oid, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Long numero, Exception e) {
		disEnErrors.add(new Erreur(numero, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addAll(StatistiquesDIs rapport) {
		this.nbDIsTotal += rapport.nbDIsTotal;
		this.disEnErrors.addAll(rapport.disEnErrors);

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
