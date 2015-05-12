package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer numeroOfs;

	private String enseigne;
	private boolean principal;

	@Column(name = "NUMERO_ETABLISSEMENT")
	@Index(name = "IDX_TIERS_NO_ETABLISSEMENT")
	public Long getNumeroEtablissement() {
		return numeroEtablissement;
	}

	public void setNumeroEtablissement(Long theNumeroEtablissement) {
		numeroEtablissement = theNumeroEtablissement;
	}

	@Column(name = "ETB_TYPE_AUT_FISC", length = LengthConstants.ETB_AUTORITEFISCALE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAutoriteFiscaleUserType")
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	@Column(name = "ETB_NUMERO_OFS_AUT_FISC")
	public Integer getNumeroOfs() {
		return numeroOfs;
	}

	public void setNumeroOfs(Integer numeroOfs) {
		this.numeroOfs = numeroOfs;
	}

	@Column(name = "ETB_ENSEIGNE", length = LengthConstants.ETB_ENSEIGNE)
	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	@Column(name = "ETB_PRINCIPAL")
	public boolean isPrincipal() {
		return principal;
	}

	public void setPrincipal(boolean principal) {
		this.principal = principal;
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

		Etablissement other = (Etablissement) obj;
		if (numeroEtablissement == null) {
			if (other.numeroEtablissement != null)
				return false;
		}
		else if (!numeroEtablissement.equals(other.numeroEtablissement))
			return false;
		if (typeAutoriteFiscale == null) {
			if (other.typeAutoriteFiscale != null)
				return false;
		}
		else if (!typeAutoriteFiscale.equals(other.typeAutoriteFiscale))
			return false;
		if (numeroOfs == null) {
			if (other.numeroOfs != null)
				return false;
		}
		else if (!numeroOfs.equals(other.numeroOfs))
			return false;
		if (enseigne == null) {
			if (other.enseigne != null)
				return false;
		}
		else if (!enseigne.equals(other.enseigne))
			return false;
		return principal == other.principal;
	}
}
