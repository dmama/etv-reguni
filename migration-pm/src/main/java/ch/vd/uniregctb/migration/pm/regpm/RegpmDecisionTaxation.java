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

import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeEtatDecisionTaxationUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeNatureDecisionTaxationUserType;

@Entity
@Table(name = "DECISION_TAXATION")
@TypeDefs({
		@TypeDef(name = "TypeNatureDecisionTaxation", typeClass = TypeNatureDecisionTaxationUserType.class),
		@TypeDef(name = "TypeEtatDecisionTaxation", typeClass = TypeEtatDecisionTaxationUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RegpmDecisionTaxation extends RegpmEntity {

	@Embeddable
	public static final class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEntreprise;
		private Integer anneeFiscale;
		private Integer envTaxationNoSeq;

		public PK() {
		}

		public PK(Integer seqNo, Long idEntreprise, Integer anneeFiscale, Integer envTaxationNoSeq) {
			this.seqNo = seqNo;
			this.idEntreprise = idEntreprise;
			this.anneeFiscale = anneeFiscale;
			this.envTaxationNoSeq = envTaxationNoSeq;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = anneeFiscale - o.anneeFiscale;
			}
			if (comparison == 0) {
				comparison = envTaxationNoSeq - o.envTaxationNoSeq;
			}
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

		@Column(name = "FK_ETX_FKENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Column(name = "FK_ETXANFIS")
		public Integer getAnneeFiscale() {
			return anneeFiscale;
		}

		public void setAnneeFiscale(Integer anneeFiscale) {
			this.anneeFiscale = anneeFiscale;
		}

		@Column(name = "FK_ETXNOSEQ")
		public Integer getEnvTaxationNoSeq() {
			return envTaxationNoSeq;
		}

		public void setEnvTaxationNoSeq(Integer envTaxationNoSeq) {
			this.envTaxationNoSeq = envTaxationNoSeq;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null) return false;
			if (idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) return false;
			if (anneeFiscale != null ? !anneeFiscale.equals(pk.anneeFiscale) : pk.anneeFiscale != null) return false;
			return !(envTaxationNoSeq != null ? !envTaxationNoSeq.equals(pk.envTaxationNoSeq) : pk.envTaxationNoSeq != null);

		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			result = 31 * result + (anneeFiscale != null ? anneeFiscale.hashCode() : 0);
			result = 31 * result + (envTaxationNoSeq != null ? envTaxationNoSeq.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegpmTypeNatureDecisionTaxation natureDecision;
	private RegpmTypeEtatDecisionTaxation etatCourant;
	private boolean derniereTaxation;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "CO_NATURE_DECISION")
	@Type(type = "TypeNatureDecisionTaxation")
	public RegpmTypeNatureDecisionTaxation getNatureDecision() {
		return natureDecision;
	}

	public void setNatureDecision(RegpmTypeNatureDecisionTaxation natureDecision) {
		this.natureDecision = natureDecision;
	}

	@Column(name = "CO_ETAT")
	@Type(type = "TypeEtatDecisionTaxation")
	public RegpmTypeEtatDecisionTaxation getEtatCourant() {
		return etatCourant;
	}

	public void setEtatCourant(RegpmTypeEtatDecisionTaxation etatCourant) {
		this.etatCourant = etatCourant;
	}

	@Column(name = "CO_DER_TAXATION")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isDerniereTaxation() {
		return derniereTaxation;
	}

	public void setDerniereTaxation(boolean derniereTaxation) {
		this.derniereTaxation = derniereTaxation;
	}
}
