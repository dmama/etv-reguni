package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "SOCIETE_DIRECTION")
@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
public class RegpmSocieteDirection extends RegpmEntity implements Comparable<RegpmSocieteDirection> {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEntreprise;

		public PK() {
		}

		public PK(Integer seqNo, Long idEntreprise) {
			this.seqNo = seqNo;
			this.idEntreprise = idEntreprise;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = seqNo - o.seqNo;
			}
			return comparison;
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

		@Column(name = "FK_D_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateValidite;
	private RegpmEntreprise direction;
	private RegpmEntreprise fonds;

	@Override
	public int compareTo(@NotNull RegpmSocieteDirection o) {
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

	@Column(name = "DA_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateValidite() {
		return dateValidite;
	}

	public void setDateValidite(RegDate dateValidite) {
		this.dateValidite = dateValidite;
	}

	@ManyToOne
	@JoinColumn(name = "FK_E_ENTPRNO")
	public RegpmEntreprise getDirection() {
		return direction;
	}

	public void setDirection(RegpmEntreprise direction) {
		this.direction = direction;
	}

//	@ManyToOne
//	@JoinColumn(name = "FK_D_ENTPRNO", insertable = false, updatable = false)
//	public RegpmEntreprise getFonds() {
//		return fonds;
//	}
//
//	public void setFonds(RegpmEntreprise fonds) {
//		this.fonds = fonds;
//	}
}
