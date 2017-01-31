package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class StatistiquesCtbs extends JobResults<Long, StatistiquesCtbs> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);

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

	/**
	 * Version spécialisé pour les statistiques des contribuables.
	 */
	public enum TypeContribuable {
		VAUDOIS_ORDINAIRE("ordinaire"),
		VAUDOIS_DEPENSE("à la dépense"),
		HORS_CANTON("hors canton"),
		HORS_SUISSE("hors Suisse"),
		SOURCIER_PUR("sourcier");

		private final String description;

		TypeContribuable(String description) {
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
			result = prime * result + ((commune == null) ? 0 : commune.getNoOFS());
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
			return commune.getNoOFS() == key.commune.getNoOFS();
		}

		/**
		 * Ordre de tri naturel: oid, commune et typeCtb.
		 */
		@Override
		public int compareTo(Key o) {

			if (oid != null && o.oid != null && !oid.equals(o.oid)) {
				return oid - o.oid;
			}
			else if (oid == null && o.oid != null) {
				return 1;
			}
			else if (oid != null && o.oid == null) {
				return -1;
			}

			if ((commune == null && o.commune != null) || (commune != null && o.commune == null) || (commune != null && o.commune != null && commune.getNoOFS() != o.commune.getNoOFS())) {
				final String nomCommune1 = commune != null ? commune.getNomOfficiel() : "";
				final String nomCommune2 = o.commune != null ? o.commune.getNomOfficiel() : "";
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

	public static final class Value {
		public int nombre;
	}

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public boolean interrompu;

	// Données de processing
	public int nbCtbsTotal = 0;
	public final List<Erreur> ctbsEnErrors = new ArrayList<>();
	public final Map<Key, Value> stats = new HashMap<>();

	public StatistiquesCtbs(int annee, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.dateTraitement = dateTraitement;
	}

	public void addStats(Integer oid, Commune commune, TypeContribuable typeCtb) {
		final Key key = new Key(oid, commune, typeCtb);
		final Value value = stats.computeIfAbsent(key, k -> new Value());
		++ value.nombre;
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		Long numero = (ctb == null ? null : ctb.getNumero());
		Integer officeImpotId = (ctb == null ? null : ctb.getOfficeImpotId());
		ctbsEnErrors.add(new Erreur(numero != null ? numero : -1, officeImpotId, ErreurType.EXCEPTION, e.getMessage(), getNom(numero != null ? numero : -1)));
	}

	@Override
	public void addErrorException(Long numero, Exception e) {
		ctbsEnErrors.add(new Erreur(numero, null, ErreurType.EXCEPTION, e.getMessage(), getNom(numero)));
	}

	@Override
	public void addAll(StatistiquesCtbs rapport) {
		this.nbCtbsTotal += rapport.nbCtbsTotal;
		this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);

		for (Map.Entry<Key, Value> e : rapport.stats.entrySet()) {
			mergeStats(e.getKey(), e.getValue());
		}
	}

	public void mergeStats(Key rightKey, Value rightValue) {
		final Value value = stats.computeIfAbsent(rightKey, k -> new Value());
		value.nombre += rightValue.nombre;
	}
}
