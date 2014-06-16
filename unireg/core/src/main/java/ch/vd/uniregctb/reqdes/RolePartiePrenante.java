package ch.vd.uniregctb.reqdes;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@Table(name = "REQDES_ROLE_PARTIE_PRENANTE")
public class RolePartiePrenante extends HibernateEntity {

	private Long id;
	private int noOfsCommune;
	private TypeRole role;

	public RolePartiePrenante() {
	}

	public RolePartiePrenante(int noOfsCommune, TypeRole role) {
		this.noOfsCommune = noOfsCommune;
		this.role = role;
	}

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "OFS_COMMUNE", nullable = false)
	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(int noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	@Column(name = "ROLE", length = LengthConstants.REQDES_ROLE, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeRole getRole() {
		return role;
	}

	public void setRole(TypeRole role) {
		this.role = role;
	}
}
