package ch.vd.vuta.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "SMS_IFD2008")
public class SmsModel {
	
	private Long id;
	
	private String requestUid;
	private String numeroNatel;
	private String operateur;
	private String langue;
	private Date dateReception;
	private String texte;
	private String smsComplet;
	private Integer numeroCTB;
	private String statusString;
	private Integer status;

	

	public SmsModel() {
		// La date de reception c'est la date couorante par défaut
		dateReception = new Date();
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "REQUEST_UID")
	public String getRequestUid() {
		return requestUid;
	}
	public void setRequestUid(String requestUid) {
		this.requestUid = requestUid;
	}

	@Column(name = "NUMERO")
	public String getNumeroNatel() {
		return numeroNatel;
	}
	public void setNumeroNatel(String numeroNatel) {
		this.numeroNatel = numeroNatel;
	}

	@Column(name = "OPERATEUR")
	public String getOperateur() {
		return operateur;
	}
	public void setOperateur(String operateur) {
		this.operateur = operateur;
	}

	@Column(name = "LANGUE")
	public String getLangue() {
		return langue;
	}
	public void setLangue(String langue) {
		this.langue = langue;
	}

	@Column(name = "DATE_RECEPTION", nullable = false)
	public Date getDateReception() {
		return dateReception;
	}
	public void setDateReception(Date dateReception) {
		this.dateReception = dateReception;
	}

	@Column(name = "TEXTE")
	public String getTexte() {
		return texte;
	}
	public void setTexte(String texte) {
		this.texte = texte;
	}
	
	@Column(name = "SMS_COMPLET", nullable = false, length = 2000)
	public String getSmsComplet() {
		return smsComplet;
	}
	public void setSmsComplet(String smsComplet) {
		this.smsComplet = smsComplet;
	}

	@Column(name = "NUMERO_CTB", nullable = true)
	public Integer getNumeroCTB() {
		return numeroCTB;
	}
	public void setNumeroCTB(Integer numeroCTB) {
		this.numeroCTB = numeroCTB;
	}
	
	@Column(name = "STATUS")
	public String getStatusString() {
		return statusString;
	}
	
	public void setStatusString(String status) {
		this.statusString = status;
	}

	@Transient
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Transient
	public boolean getStatusAsBool() {
		if (status.equals(new Integer(1))) {
			return true;
		}
		return false;
	}
	
	@Transient
	public void setStatus(boolean status, String statusStr) {
		if (status) {
			this.status = new Integer(1);
		}
		else {
			this.status = new Integer(0);
		}
		setStatusString(statusStr);
	}

	public String toString() {
		
		String str = "id:"+id+
			"requestUid:"+requestUid+
			" numeroNatel:"+numeroNatel+
			" operateur:"+operateur+
			" langue:"+langue+
			" dateReception:"+dateReception+
			" texte:"+texte+
			" numeroCTB:"+numeroCTB;
		return str;
	}

}
