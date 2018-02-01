package ch.vd.unireg.evenement.ide;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
@Entity
@Table(name = "REFERENCE_ANNONCE_IDE")
public class ReferenceAnnonceIDE extends HibernateEntity {

	/**
	 * Identifiant de l'annonce.
	 */
	private long id;

	/**
	 * L'établissement concerné par cette annonce.
	 */
	private Etablissement etablissement;

	/**
	 * Identifiant de correllation métier du message esb et de la réponse.
	 */
	private String msgBusinessId;

	public ReferenceAnnonceIDE() {}

	public ReferenceAnnonceIDE(String msgBusinessId, Etablissement etablissement) {
		this.msgBusinessId = msgBusinessId;
		this.etablissement = etablissement;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@JoinColumn(name = "ETABLISSEMENT_ID", nullable = false)
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_REFANNIDE_ETAB_ID")
	@Index(name = "IDX_EVTANNIDE_ETAB_ID")
	public Etablissement getEtablissement() {
		return etablissement;
	}

	public void setEtablissement(Etablissement etablissement) {
		this.etablissement = etablissement;
	}

	@Column(name = "MSG_BUSINESS_ID", length = LengthConstants.REFANNONCEIDE_BUSINESS_ID)
	@Index(name = "IDX_EVTANNIDE_BUSINESS_ID")
	public String getMsgBusinessId() {
		return msgBusinessId;
	}

	public void setMsgBusinessId(String msgBusinessId) {
		this.msgBusinessId = msgBusinessId;
	}

	@Override
	public String toString() {
		return String.format("%s{id=%d, businessId=%s, etablissementId=%d}", getClass().getSimpleName(), id, msgBusinessId, etablissement.getNumero());
	}

}
