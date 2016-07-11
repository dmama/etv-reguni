package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.ObjectImpotUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ALLEGEMENT_FISCAL")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "ObjectImpot", typeClass = ObjectImpotUserType.class)
          })
public class RegpmAllegementFiscal extends RegpmEntity implements DateRange {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable {

		private Integer seqNo;
		private Long idEntreprise;

		public PK() {
		}

		public PK(Integer seqNo, Long idEntreprise) {
			this.seqNo = seqNo;
			this.idEntreprise = idEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_ENTNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private BigDecimal pourcentage;
	private RegDate dateAnnulation;
	private RegpmCommune commune;
	private RegpmObjectImpot objectImpot;
	private RegpmTypeContribution typeContribution;

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAD")
	@Type(type = "RegDate")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DAF")
	@Type(type = "RegDate")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "POURCENTAGE")
	public BigDecimal getPourcentage() {
		return pourcentage;
	}

	public void setPourcentage(BigDecimal pourcentage) {
		this.pourcentage = pourcentage;
	}

	@Column(name = "DAA")
	@Type(type = "RegDate")
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	@ManyToOne
	@JoinColumn(name = "FK_COMNO")
	public RegpmCommune getCommune() {
		return commune;
	}

	public void setCommune(RegpmCommune commune) {
		this.commune = commune;
	}

	@Column(name = "FK_OBJIMPCO")
	@Type(type = "ObjectImpot")
	public RegpmObjectImpot getObjectImpot() {
		return objectImpot;
	}

	public void setObjectImpot(RegpmObjectImpot objectImpot) {
		this.objectImpot = objectImpot;
	}

	@ManyToOne
	@JoinColumn(name = "FK_TYCONTRNO")
	public RegpmTypeContribution getTypeContribution() {
		return typeContribution;
	}

	public void setTypeContribution(RegpmTypeContribution typeContribution) {
		this.typeContribution = typeContribution;
	}
}
