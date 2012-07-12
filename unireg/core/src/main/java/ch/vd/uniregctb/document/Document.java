package ch.vd.uniregctb.document;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * Classe d'indexe pour les documents générés par l'application et devant être stockés sur le disque du serveur pour consultation
 * ultérieure.
 */
@Entity
@Table(name = "DOC_INDEX")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOC_TYPE", discriminatorType = DiscriminatorType.STRING, length = 50)
public abstract class Document extends HibernateEntity {

	private static final long serialVersionUID = 6233482437200891946L;

	// Données utilisateur
	private String nom;
	private String description;

	// Données système
	private Long id;
	private String fileName;
	private String fileExtension;
	private String subPath;
	private long fileSize;

	public Document() {
	}

	public Document(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		this.nom = nom;
		this.description = description;
		this.fileName = fileName;
		this.fileExtension = fileExtension;
		this.subPath = subPath;
		this.fileSize = fileSize;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return le nom utilisateur
	 */
	@Column(name = "NOM", length = LengthConstants.DOCINDEX_NOM, nullable = false)
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "DESCRIPTION", length = LengthConstants.DOCINDEX_DESC)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return le nom du fichier sur le filesystem du serveur (sans l'extension)
	 */
	@Column(name = "FILE_NAME", nullable = false)
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	/**
	 * @return l'extension du fichier sur le filesystem du serveur
	 */
	@Column(name = "FILE_EXT", nullable = false)
	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	/**
	 * @return le chemin relatif du document par rapport au répertoire de stockage sur le serveur.
	 */
	@Column(name = "SUB_PATH", nullable = false)
	public String getSubPath() {
		return subPath;
	}

	public void setSubPath(String subPath) {
		this.subPath = subPath;
	}

	/**
	 * @return la taille sur le disque du document
	 */
	@Column(name = "FILE_SIZE", nullable = false)
	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
}
