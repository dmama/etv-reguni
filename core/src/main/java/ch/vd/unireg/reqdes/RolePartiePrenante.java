package ch.vd.unireg.reqdes;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "REQDES_ROLE_PARTIE_PRENANTE")
public class RolePartiePrenante extends HibernateEntity {

	private Long id;
	private TransactionImmobiliere transaction;
	private TypeRole role;

	public RolePartiePrenante() {
	}

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID")
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ROLE", length = LengthConstants.REQDES_ROLE, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeRole getRole() {
		return role;
	}

	public void setRole(TypeRole role) {
		this.role = role;
	}

	@ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	@JoinColumn(name = "TRANSACTION_IMMOBILIERE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_REQDES_RPP_TI_ID"))
	public TransactionImmobiliere getTransaction() {
		return transaction;
	}

	public void setTransaction(TransactionImmobiliere transaction) {
		this.transaction = transaction;
	}
}
