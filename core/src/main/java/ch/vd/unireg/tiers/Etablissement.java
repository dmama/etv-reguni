package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;

/**
 * Installation matérielle au moyen de laquelle s'exerce une part de l'activité d'une entreprise (succursale, agence, dépôt), connu du registre des personnes morales de l'ACI.
 * Une raison individuelle est l'établissement d'une personne physique.
 * L'établissement en lui-même n'est pas contribuable, mais peut être constitutif d'un for fiscal (établissement stable).
 */
@Entity
@DiscriminatorValue("Etablissement")
public class Etablissement extends Contribuable {

	// Numéros (de tiers) générés pour les établissements
	public static final int ETB_GEN_FIRST_ID = 3000000;
	public static final int ETB_GEN_LAST_ID = 3999999;

	/**
	 * Identifiant cantonal (= dans RCEnt)
	 */
	private Long numeroEtablissement;
	private String enseigne;
	private String raisonSociale;
	private Set<DomicileEtablissement> domiciles;

	@Column(name = "NUMERO_ETABLISSEMENT")
	@Index(name = "IDX_TIERS_NO_ETABLISSEMENT")
	public Long getNumeroEtablissement() {
		return numeroEtablissement;
	}

	public void setNumeroEtablissement(Long theNumeroEtablissement) {
		numeroEtablissement = theNumeroEtablissement;
	}

	@Column(name = "ETB_ENSEIGNE", length = LengthConstants.ETB_ENSEIGNE)
	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	@Column(name = "ETB_RAISON_SOCIALE", length = LengthConstants.ETB_RAISON_SOCIALE)
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "ETABLISSEMENT_ID")
	public Set<DomicileEtablissement> getDomiciles() {
		return domiciles;
	}

	public void setDomiciles(Set<DomicileEtablissement> domiciles) {
		this.domiciles = domiciles;
	}

	/**
	 * @param aussiAnnules <code>true</code> si tous les domiciles sont pris en compte, <code>false</code> si seuls les domiciles non-annulés doivent l'être
	 * @return une liste triée chronologiquement des domiciles (si les annulés sont présents, ils sont de toute façon à la fin)
	 */
	@Transient
	@NotNull
	public List<DomicileEtablissement> getSortedDomiciles(boolean aussiAnnules) {
		// collecte...
		final List<DomicileEtablissement> liste;
		if (domiciles == null || domiciles.isEmpty()) {
			liste = Collections.emptyList();
		}
		else if (aussiAnnules) {
			liste = new ArrayList<>(domiciles);
		}
		else {
			liste = new ArrayList<>(domiciles.size());
			for (DomicileEtablissement domicile : domiciles) {
				if (!domicile.isAnnule()) {
					liste.add(domicile);
				}
			}
		}

		// ... puis tri (les annulés à la fin, sinon par dates)
		liste.sort(new DateRangeComparator<DomicileEtablissement>() {
			@Override
			public int compare(DomicileEtablissement o1, DomicileEtablissement o2) {
				int comparison = Boolean.compare(o1.isAnnule(), o2.isAnnule());
				if (comparison == 0) {
					comparison = super.compare(o1, o2);
				}
				return comparison;
			}
		});
		return liste;
	}

	public void addDomicile(DomicileEtablissement domicile) {
		if (this.domiciles == null) {
			this.domiciles = new HashSet<>();
		}
		domicile.setEtablissement(this);
		this.domiciles.add(domicile);
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Etablissement";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.Etablissement;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.ETABLISSEMENT;
	}

	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final Etablissement other = (Etablissement) obj;
		return ComparisonHelper.areEqual(numeroEtablissement, other.numeroEtablissement)
				&& ComparisonHelper.areEqual(domiciles, other.domiciles)
				&& ComparisonHelper.areEqual(enseigne, other.enseigne);
	}

	@Transient
	public boolean isConnuAuCivil() {
		return numeroEtablissement != null;
	}
}
