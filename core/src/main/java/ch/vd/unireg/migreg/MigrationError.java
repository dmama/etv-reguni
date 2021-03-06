package ch.vd.unireg.migreg;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.type.TypeMigRegError;

@Entity
@Table(name = "MIGREG_ERROR")
@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
public class MigrationError {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationError.class);

	private static final int MESSAGE_MAX_LENGTH = 255;

	private String message;
	private Long id;
	private Long noContribuable;
	private Integer noIndividu;
	private TypeMigRegError typeErreur;
	private String nomContribuable;
	private String forPrincipalCtb;
	private String nomIndividu;

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "LIBELLE_MESSAGE", length = MESSAGE_MAX_LENGTH)
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		if (message != null && message.length() > MESSAGE_MAX_LENGTH) {
			LOGGER.warn("Le message [" + message + "] dépasse la longueur maximale autorisé de [" + MESSAGE_MAX_LENGTH
					+ "] et sera coupé en base de données");
			message = message.substring(0, MESSAGE_MAX_LENGTH - 1);
		}
		this.message = message;
	}

	@Column(name = "NO_CONTRIBUABLE", unique = true)
	public Long getNoContribuable() {
		return noContribuable;
	}

	public void setNoContribuable(Long noContribuable) {
		this.noContribuable = noContribuable;
	}

	@Column(name = "NO_INDIVIDU", unique = true)
	public Integer getNoIndividu() {
		return noIndividu;
	}

	public void setNoIndividu(Integer noIndividu) {
		this.noIndividu = noIndividu;
	}

	@Column(name = "TYPE_ERREUR")
	public TypeMigRegError getTypeErreur() {
		return typeErreur;
	}

	public void setTypeErreur(TypeMigRegError typeErreur) {
		this.typeErreur = typeErreur;
	}

	@Column(name = "NOM_CONTRIBUABLE")
	public String getNomContribuable() {
		return nomContribuable;
	}

	public void setNomContribuable(String nomContribuable) {
		this.nomContribuable = nomContribuable;
	}

	@Column(name = "FOR_PRINCIPAL_CTB")
	public String getForPrincipalCtb() {
		return forPrincipalCtb;
	}

	public void setForPrincipalCtb(String forPrincipalCtb) {
		this.forPrincipalCtb = forPrincipalCtb;
	}

	@Column(name = "NOM_INDIVIDU")
	public String getNomIndividu() {
		return nomIndividu;
	}

	public void setNomIndividu(String nomIndividu) {
		this.nomIndividu = nomIndividu;
	}

}
