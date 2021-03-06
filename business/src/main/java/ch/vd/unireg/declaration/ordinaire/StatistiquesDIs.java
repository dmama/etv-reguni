package ch.vd.unireg.declaration.ordinaire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class StatistiquesDIs extends JobResults<Long, StatistiquesDIs> {

	public enum ErreurType {
		EXCEPTION("une exception est apparue pendant le traitement de la déclaration"); // ----------------

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
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
		public final TypeEtatDocumentFiscal etat;

		public Key(int oid, TypeContribuable typeCtb, TypeEtatDocumentFiscal etat) {
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
		@Override
		public int compareTo(@NotNull Key o) {

			if (oid != o.oid) {
				return Integer.compare(oid, o.oid);
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

	public static final class Value {
		public int nombre;
	}

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final String population;
	public boolean interrompu;

	// Données de processing
	public int nbDIsTotal = 0;
	public final List<Erreur> disEnErrors = new ArrayList<>();
	public final Map<Key, Value> stats = new HashMap<>();

	public StatistiquesDIs(int annee, RegDate dateTraitement, String population, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.dateTraitement = dateTraitement;
		this.population = population;
	}

	public void addStats(int oid, TypeContribuable typeCtb, TypeEtatDocumentFiscal etat) {
		final Key key = new Key(oid, typeCtb, etat);
		final Value value = stats.computeIfAbsent(key, k -> new Value());
		++ value.nombre;
	}

	public void addErrorException(DeclarationImpotOrdinaire di, Exception e) {
		final Tiers tiers = di.getTiers();
		final Long numero = (tiers == null ? null : tiers.getNumero());
		final Integer oid = (tiers == null ? null : tiers.getOfficeImpotId());
		disEnErrors.add(new Erreur(numero != null ? numero : -1, oid, ErreurType.EXCEPTION, e.getMessage(), getNom(numero != null ? numero : -1)));
	}

	@Override
	public void addErrorException(Long numero, Exception e) {
		disEnErrors.add(new Erreur(numero, null, ErreurType.EXCEPTION, e.getMessage(), getNom(numero)));
	}

	@Override
	public void addAll(StatistiquesDIs rapport) {
		this.nbDIsTotal += rapport.nbDIsTotal;
		this.disEnErrors.addAll(rapport.disEnErrors);

		for (Map.Entry<Key, Value> e : rapport.stats.entrySet()) {
			mergeStats(e.getKey(), e.getValue());
		}
	}

	public void mergeStats(Key rightKey, Value rightValue) {
		final Value value = stats.computeIfAbsent(rightKey, k -> new Value());
		value.nombre += rightValue.nombre;
	}
}
