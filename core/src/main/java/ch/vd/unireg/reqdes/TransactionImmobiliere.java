package ch.vd.unireg.reqdes;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "REQDES_TRANSACTION_IMMOBILIERE")
public class TransactionImmobiliere extends HibernateEntity {

	private Long id;
	private String description;
	private ModeInscription modeInscription;
	private TypeInscription typeInscription;
	private int ofsCommune;
	private EvenementReqDes evenementReqDes;

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

	@Column(name = "DESCRIPTION", length = LengthConstants.REQDES_TRANSACTION_DESCRIPTION)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "MODE_INSCRIPTION", length = LengthConstants.REQDES_MODE_INSCRIPTION, nullable = false)
	@Enumerated(EnumType.STRING)
	public ModeInscription getModeInscription() {
		return modeInscription;
	}

	public void setModeInscription(ModeInscription modeInscription) {
		this.modeInscription = modeInscription;
	}

	@Column(name = "TYPE_INSCRIPTION", length = LengthConstants.REQDES_TYPE_INSCRIPTION, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeInscription getTypeInscription() {
		return typeInscription;
	}

	public void setTypeInscription(TypeInscription typeInscription) {
		this.typeInscription = typeInscription;
	}

	@Column(name = "OFS_COMMUNE", nullable = false)
	public int getOfsCommune() {
		return ofsCommune;
	}

	public void setOfsCommune(int ofsCommune) {
		this.ofsCommune = ofsCommune;
	}

	@ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	@JoinColumn(name = "EVENEMENT_ID", nullable = false)
	@ForeignKey(name = "FK_REQDES_TI_EVT_ID")
	public EvenementReqDes getEvenementReqDes() {
		return evenementReqDes;
	}

	public void setEvenementReqDes(EvenementReqDes evenementReqDes) {
		this.evenementReqDes = evenementReqDes;
	}
}
