package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeAdresseEntrepriseUserType;

@Entity
@Table(name = "ADR_ENTREPRISE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "TypeAdresseEntreprise", typeClass = TypeAdresseEntrepriseUserType.class)
})
public class RegpmAdresseEntreprise extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et le type d'adresse (-> pas d'historique...)
	 */
	@Embeddable
	public static class PK implements Serializable {

		private RegpmTypeAdresseEntreprise typeAdresse;
		private Long idEntreprise;

		public PK() {
		}

		public PK(RegpmTypeAdresseEntreprise typeAdresse, Long idEntreprise) {
			this.typeAdresse = typeAdresse;
			this.idEntreprise = idEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(typeAdresse != null ? !typeAdresse.equals(pk.typeAdresse) : pk.typeAdresse != null);
		}

		@Override
		public int hashCode() {
			int result = typeAdresse != null ? typeAdresse.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
		}

		@Column(name = "TY_ADRESSE")
		@Type(type = "TypeAdresseEntreprise")
		public RegpmTypeAdresseEntreprise getTypeAdresse() {
			return typeAdresse;
		}

		public void setTypeAdresse(RegpmTypeAdresseEntreprise typeAdresse) {
			this.typeAdresse = typeAdresse;
		}

		@Column(name = "FK_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateValidite;
	private String chez;
	private String nomRue;
	private String noPolice;
	private String lieu;
	private RegpmLocalitePostale localitePostale;
	private Integer ofsPays;
	private RegpmRue rue;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Transient
	public RegpmTypeAdresseEntreprise getTypeAdresse() {
		return id != null ? id.getTypeAdresse() : null;
	}

	@Column(name = "DA_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateValidite() {
		return dateValidite;
	}

	public void setDateValidite(RegDate dateValidite) {
		this.dateValidite = dateValidite;
	}

	@Column(name = "CHEZ")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getChez() {
		return chez;
	}

	public void setChez(String chez) {
		this.chez = chez;
	}

	@Column(name = "RUE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomRue() {
		return nomRue;
	}

	public void setNomRue(String nomRue) {
		this.nomRue = nomRue;
	}

	@Column(name = "NO_POLICE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getNoPolice() {
		return noPolice;
	}

	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}

	@Column(name = "LIEU")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "20"))
	public String getLieu() {
		return lieu;
	}

	public void setLieu(String lieu) {
		this.lieu = lieu;
	}

	@ManyToOne
	@JoinColumn(name = "FK_LOC_POSTNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}

	@Column(name = "FK_PAYSNO_OFS")
	public Integer getOfsPays() {
		return ofsPays;
	}

	public void setOfsPays(Integer ofsPays) {
		this.ofsPays = ofsPays;
	}

	@ManyToOne
	@JoinColumn(name = "FK_RUENO")
	public RegpmRue getRue() {
		return rue;
	}

	public void setRue(RegpmRue rue) {
		this.rue = rue;
	}
}
