package ch.vd.uniregctb.migration.pm.mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "MIGRATION_PM_MAPPING", uniqueConstraints = @UniqueConstraint(name = "IDX_MIGRATION_PM_ENTITE", columnNames = {"TYPE_ENTITE", "ID_REGPM"}))
@SequenceGenerator(name = "S_MIGR_PM", sequenceName = "S_MIGR_PM")
public class MigrationPmMapping {

	public enum TypeEntite {
		ENTREPRISE,
		ETABLISSEMENT,
		INDIVIDU
	}

	private Long id;
	private TypeEntite typeEntite;
	private Long idRegpm;
	private Long idUnireg;

	public MigrationPmMapping() {
	}

	public MigrationPmMapping(TypeEntite typeEntite, Long idRegpm, Long idUnireg) {
		this.typeEntite = typeEntite;
		this.idRegpm = idRegpm;
		this.idUnireg = idUnireg;
	}

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "S_MIGR_PM")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_ENTITE", nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeEntite getTypeEntite() {
		return typeEntite;
	}

	public void setTypeEntite(TypeEntite typeEntite) {
		this.typeEntite = typeEntite;
	}

	@Column(name = "ID_REGPM", nullable = false)
	public Long getIdRegpm() {
		return idRegpm;
	}

	public void setIdRegpm(Long idRegpm) {
		this.idRegpm = idRegpm;
	}

	@Column(name = "ID_UNIREG", nullable = false)
	public Long getIdUnireg() {
		return idUnireg;
	}

	public void setIdUnireg(Long idUnireg) {
		this.idUnireg = idUnireg;
	}
}
