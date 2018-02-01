package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.unireg.common.HibernateEntity;

@Entity
@Table(name = "MODELE_FEUILLE_DOC")
public class ModeleFeuilleDocument extends HibernateEntity {

	private Long id;
	private String intituleFeuille;
	private ModeleDocument modeleDocument;
	private int noCADEV;
	private Integer noFormulaireACI;

	/**
	 * Index qui permet d'ordonner les feuilles pour un modèle donné (SIFISC-2066).
	 */
	private Integer index;

	/**
	 * Booléen qui indique si la feuille est la feuille principale d'un groupe
	 */
	private boolean principal;

	public ModeleFeuilleDocument() {
	}

	public ModeleFeuilleDocument(ModeleFeuilleDocument source, ModeleDocument modeleDoc) {
		this.intituleFeuille = source.intituleFeuille;
		this.modeleDocument = modeleDoc;
		this.noCADEV = source.noCADEV;
		this.noFormulaireACI = source.noFormulaireACI;
		this.index = source.index;
		this.principal = source.principal;
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

	@Column(name = "INTITULE_FEUILLE")
	public String getIntituleFeuille() {
		return intituleFeuille;
	}

	public void setIntituleFeuille(String theIntituleFeuille) {
		intituleFeuille = theIntituleFeuille;
	}

	@Column(name = "NO_CADEV", nullable = false, precision = 4)
	public int getNoCADEV() {
		return noCADEV;
	}

	public void setNoCADEV(int noCADEV) {
		this.noCADEV = noCADEV;
	}

	@Column(name = "NO_FORMULAIRE_ACI", precision = 5)
	public Integer getNoFormulaireACI() {
		return noFormulaireACI;
	}

	public void setNoFormulaireACI(Integer noFormulaireACI) {
		this.noFormulaireACI = noFormulaireACI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "MODELE_ID", insertable = true, updatable = false)
	public ModeleDocument getModeleDocument() {
		return modeleDocument;
	}

	public void setModeleDocument(ModeleDocument theModeleDocument) {
		modeleDocument = theModeleDocument;
	}

	@Column(name = "SORT_INDEX")
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	@Column(name = "PRINCIPAL")
	public boolean isPrincipal() {
		return principal;
	}

	public void setPrincipal(boolean principal) {
		this.principal = principal;
	}
}
