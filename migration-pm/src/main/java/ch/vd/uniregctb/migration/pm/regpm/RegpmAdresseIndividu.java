package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.LongAsFixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeAdresseIndividuUserType;

@Entity
@Table(name = "ADR_INDIVIDU")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "LongAsFixedChar", typeClass = LongAsFixedCharUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "TypeAdresseIndividu", typeClass = TypeAdresseIndividuUserType.class)
          })
public class RegpmAdresseIndividu extends RegpmEntity implements AdresseAvecRue {

	@Embeddable
	public static class PK implements Serializable {

		private Integer noSequence;
		private Long noIndividu;

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSequence() {
			return noSequence;
		}

		public void setNoSequence(Integer noSequence) {
			this.noSequence = noSequence;
		}

		@Column(name = "FK_INDNO")
		public Long getNoIndividu() {
			return noIndividu;
		}

		public void setNoIndividu(Long noIndividu) {
			this.noIndividu = noIndividu;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(noIndividu != null ? !noIndividu.equals(pk.noIndividu) : pk.noIndividu != null) && !(noSequence != null ? !noSequence.equals(pk.noSequence) : pk.noSequence != null);
		}

		@Override
		public int hashCode() {
			int result = noSequence != null ? noSequence.hashCode() : 0;
			result = 31 * result + (noIndividu != null ? noIndividu.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegpmTypeAdresseIndividu type;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegDate dateAnnulation;
	private String chez;
	private String nomRue;
	private String noPolice;
	private String lieu;
	private Long egid;
	private Long ewid;
	private String noPostalEtranger;
	private RegpmLocalitePostale localitePostale;
	private RegpmRue rue;
	private Integer ofsPays;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "TY_ADRESSE")
	@Type(type = "TypeAdresseIndividu")
	public RegpmTypeAdresseIndividu getType() {
		return type;
	}

	public void setType(RegpmTypeAdresseIndividu type) {
		this.type = type;
	}

	@Column(name = "DA_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DAF_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "DA_ANNULATION")
	@Type(type = "RegDate")
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	@Column(name = "CHEZ")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value ="30"))
	public String getChez() {
		return chez;
	}

	public void setChez(String chez) {
		this.chez = chez;
	}

	@Column(name = "RUE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value ="30"))
	public String getNomRue() {
		return nomRue;
	}

	public void setNomRue(String nomRue) {
		this.nomRue = nomRue;
	}

	@Column(name = "NO_POLICE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value ="10"))
	public String getNoPolice() {
		return noPolice;
	}

	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}

	@Column(name = "LIEU")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value ="35"))
	public String getLieu() {
		return lieu;
	}

	public void setLieu(String lieu) {
		this.lieu = lieu;
	}

	@Column(name = "EGID")
	@Type(type = "LongAsFixedChar")
	public Long getEgid() {
		return egid;
	}

	public void setEgid(Long egid) {
		this.egid = egid;
	}

	@Column(name = "EWID")
	@Type(type = "LongAsFixedChar")
	public Long getEwid() {
		return ewid;
	}

	public void setEwid(Long ewid) {
		this.ewid = ewid;
	}

	@Column(name = "NO_POSTAL_ETRANG")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value ="15"))
	public String getNoPostalEtranger() {
		return noPostalEtranger;
	}

	public void setNoPostalEtranger(String noPostalEtranger) {
		this.noPostalEtranger = noPostalEtranger;
	}

	@ManyToOne
	@JoinColumn(name = "FK_LOC_POSTNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}

	@ManyToOne
	@JoinColumn(name = "FK_RUENO")
	public RegpmRue getRue() {
		return rue;
	}

	public void setRue(RegpmRue rue) {
		this.rue = rue;
	}

	@Column(name = "FK_PAYSNO_OFS")
	public Integer getOfsPays() {
		return ofsPays;
	}

	public void setOfsPays(Integer ofsPays) {
		this.ofsPays = ofsPays;
	}
}
