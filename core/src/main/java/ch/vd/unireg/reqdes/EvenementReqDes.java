package ch.vd.unireg.reqdes;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "EVENEMENT_REQDES")
public class EvenementReqDes extends HibernateEntity {

	private Long id;
	private String xml;
	private boolean doublon;
	private RegDate dateActe;
	private String numeroMinute;
	private InformationsActeur notaire;
	private InformationsActeur operateur;
	private Long noAffaire;
	private Set<TransactionImmobiliere> transactions;

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

	@Column(name = "XML", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.StringAsClobUserType")
	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	@Column(name = "DOUBLON", nullable = false)
	public boolean isDoublon() {
		return doublon;
	}

	public void setDoublon(boolean doublon) {
		this.doublon = doublon;
	}

	@Column(name = "DATE_ACTE", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateActe() {
		return dateActe;
	}

	public void setDateActe(RegDate dateActe) {
		this.dateActe = dateActe;
	}

	@Column(name = "NUMERO_MINUTE", length = LengthConstants.REQDES_NUMERO_MINUTE, nullable = false)
	public String getNumeroMinute() {
		return numeroMinute;
	}

	public void setNumeroMinute(String numeroMinute) {
		this.numeroMinute = numeroMinute;
	}

	@Column(name = "NO_AFFAIRE")
	public Long getNoAffaire() {
		return noAffaire;
	}

	public void setNoAffaire(Long noAffaire) {
		this.noAffaire = noAffaire;
	}

	@Embedded
	@AttributeOverrides({@AttributeOverride(name = "visa", column = @Column(name = "VISA_NOTAIRE", nullable = false, length = LengthConstants.HIBERNATE_LOGUSER)), @AttributeOverride(name = "nom", column = @Column(name = "NOM_NOTAIRE", nullable = false, length = LengthConstants.ADRESSE_NOM)), @AttributeOverride(name = "prenom", column = @Column(name = "PRENOM_NOTAIRE", nullable = false, length = LengthConstants.ADRESSE_NOM))})
	public InformationsActeur getNotaire() {
		return notaire;
	}

	public void setNotaire(InformationsActeur notaire) {
		this.notaire = notaire;
	}

	@Embedded
	@AttributeOverrides({@AttributeOverride(name = "visa", column = @Column(name = "VISA_OPERATEUR", length = LengthConstants.HIBERNATE_LOGUSER, nullable = true)), @AttributeOverride(name = "nom", column = @Column(name = "NOM_OPERATEUR", nullable = true, length = LengthConstants.ADRESSE_NOM)), @AttributeOverride(name = "prenom", column = @Column(name = "PRENOM_OPERATEUR", nullable = true, length = LengthConstants.ADRESSE_NOM))})
	public InformationsActeur getOperateur() {
		return operateur;
	}

	public void setOperateur(InformationsActeur operateur) {
		this.operateur = operateur;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "evenementReqDes")
	public Set<TransactionImmobiliere> getTransactions() {
		return transactions;
	}

	public void setTransactions(Set<TransactionImmobiliere> transactions) {
		this.transactions = transactions;
	}
}
