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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "APPART_GROUPE")
@TypeDefs({
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmAppartenanceGroupeProprietaire extends RegpmEntity implements DateRange {

	/**
	 * Ils ont fait une clé primaire avec le numéro du groupe et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable {

		private Integer seqNo;
		private Long idGroupeProprietaire;

		public PK() {
		}

		public PK(Integer seqNo, Long idGroupeProprietaire) {
			this.seqNo = seqNo;
			this.idGroupeProprietaire = idGroupeProprietaire;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null) return false;
			return !(idGroupeProprietaire != null ? !idGroupeProprietaire.equals(pk.idGroupeProprietaire) : pk.idGroupeProprietaire != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idGroupeProprietaire != null ? idGroupeProprietaire.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_GRPPROPNO")
		public Long getIdGroupeProprietaire() {
			return idGroupeProprietaire;
		}

		public void setIdGroupeProprietaire(Long idGroupeProprietaire) {
			this.idGroupeProprietaire = idGroupeProprietaire;
		}
	}

	private PK id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private boolean leader;
	private RegpmGroupeProprietaire groupeProprietaire;

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

	@Column(name = "DAD_VALIDITE")
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

	@Column(name = "LEADER")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isLeader() {
		return leader;
	}

	public void setLeader(boolean leader) {
		this.leader = leader;
	}

	@ManyToOne
	@JoinColumn(name = "FK_GRPPROPNO", insertable = false, updatable = false)
	public RegpmGroupeProprietaire getGroupeProprietaire() {
		return groupeProprietaire;
	}

	public void setGroupeProprietaire(RegpmGroupeProprietaire groupeProprietaire) {
		this.groupeProprietaire = groupeProprietaire;
	}
}
