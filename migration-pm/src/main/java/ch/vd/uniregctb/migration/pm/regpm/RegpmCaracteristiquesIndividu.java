package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "CARACT_INDIVIDU")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmCaracteristiquesIndividu extends RegpmEntity implements Comparable<RegpmCaracteristiquesIndividu> {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'individu et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idIndividu;

		public PK() {
		}

		public PK(Integer seqNo, Long idIndividu) {
			this.seqNo = seqNo;
			this.idIndividu = idIndividu;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idIndividu != null ? !idIndividu.equals(pk.idIndividu) : pk.idIndividu != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idIndividu != null ? idIndividu.hashCode() : 0);
			return result;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idIndividu, o.idIndividu);
			if (comparison == 0) {
				comparison = seqNo - o.seqNo;
			}
			return comparison;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_INDNO")
		public Long getIdIndividu() {
			return idIndividu;
		}

		public void setIdIndividu(Long idIndividu) {
			this.idIndividu = idIndividu;
		}
	}

	private PK id;
	private String nom;
	private String prenom;
	private String complementNom;
	private String nomNaissance;
	private String nomCourrier1;
	private String nomCourrier2;
	private String autresPrenoms;
	private String nomPasseport;
	private String nomMere;
	private String prenomsMere;
	private String nomPere;
	private String prenomsPere;
	private RegDate dateValidite;
	private RegDate dateFinValidite;
	private Long noAvs11;
	private String noAvs13;

	@Override
	public int compareTo(@NotNull RegpmCaracteristiquesIndividu o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateValidite, o.dateValidite);
		if (comparison == 0) {
			comparison = id.compareTo(o.id);
		}
		return comparison;
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "NOM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Column(name = "COMPLEMENT_IDENTIF")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String complementNom) {
		this.complementNom = complementNom;
	}

	@Column(name = "NOM_NAISSANCE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	@Column(name = "NOM_COURRIER_1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getNomCourrier1() {
		return nomCourrier1;
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	@Column(name = "NOM_COURRIER_2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getNomCourrier2() {
		return nomCourrier2;
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier2 = nomCourrier2;
	}

	@Column(name = "AUTRES_PRENOMS")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public void setAutresPrenoms(String autresPrenoms) {
		this.autresPrenoms = autresPrenoms;
	}

	@Column(name = "NOM_PASSEPORT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "100"))
	public String getNomPasseport() {
		return nomPasseport;
	}

	public void setNomPasseport(String nomPasseport) {
		this.nomPasseport = nomPasseport;
	}

	@Column(name = "NOM_MERE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "100"))
	public String getNomMere() {
		return nomMere;
	}

	public void setNomMere(String nomMere) {
		this.nomMere = nomMere;
	}

	@Column(name = "PRENOMS_MERE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "100"))
	public String getPrenomsMere() {
		return prenomsMere;
	}

	public void setPrenomsMere(String prenomsMere) {
		this.prenomsMere = prenomsMere;
	}

	@Column(name = "NOM_PERE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "100"))
	public String getNomPere() {
		return nomPere;
	}

	public void setNomPere(String nomPere) {
		this.nomPere = nomPere;
	}

	@Column(name = "PRENOMS_PERE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "100"))
	public String getPrenomsPere() {
		return prenomsPere;
	}

	public void setPrenomsPere(String prenomsPere) {
		this.prenomsPere = prenomsPere;
	}

	@Column(name = "DA_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateValidite() {
		return dateValidite;
	}

	public void setDateValidite(RegDate dateValidite) {
		this.dateValidite = dateValidite;
	}

	@Column(name = "DAF_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Column(name = "NO_AVS")
	public Long getNoAvs11() {
		return noAvs11;
	}

	public void setNoAvs11(Long noAvs11) {
		this.noAvs11 = noAvs11;
	}

	@Column(name = "NN_AVS")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "13"))
	public String getNoAvs13() {
		return noAvs13;
	}

	public void setNoAvs13(String noAvs13) {
		this.noAvs13 = noAvs13;
	}
}
