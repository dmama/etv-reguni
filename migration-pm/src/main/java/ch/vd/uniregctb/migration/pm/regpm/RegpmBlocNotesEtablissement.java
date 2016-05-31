package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "BLOC_NOTES_ETAB_RI")
public class RegpmBlocNotesEtablissement extends RegpmBlocNotes implements Comparable<RegpmBlocNotesEtablissement> {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'établissement et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEtablissement;

		public PK() {
		}

		public PK(Integer seqNo, Long idEtablissement) {
			this.seqNo = seqNo;
			this.idEtablissement = idEtablissement;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEtablissement != null ? !idEtablissement.equals(pk.idEtablissement) : pk.idEtablissement != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEtablissement != null ? idEtablissement.hashCode() : 0);
			return result;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEtablissement, o.idEtablissement);
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

		@Column(name = "FK_ETABNO")
		public Long getIdEtablissement() {
			return idEtablissement;
		}

		public void setIdEtablissement(Long idEtablissement) {
			this.idEtablissement = idEtablissement;
		}
	}

	private PK id;

	@Override
	public int compareTo(@NotNull RegpmBlocNotesEtablissement o) {
		return id.compareTo(o.id);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}
}
