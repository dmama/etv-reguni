package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeDocument;

@Entity
@Table(name = "MODELE_DOCUMENT")
public class ModeleDocument extends HibernateEntity {

	private Long id;
	private Set<ModeleFeuilleDocument> modelesFeuilleDocument;
	private TypeDocument typeDocument;
	private PeriodeFiscale periodeFiscale;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_DOCUMENT", length = LengthConstants.MODELEDOC_TYPE)
	@Type(type = "ch.vd.unireg.hibernate.TypeDocumentUserType")
	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument theTypeDocument) {
		typeDocument = theTypeDocument;
	}

	/**
	 * Ajoute le modèle spécifié à la période fiscale.
	 */
	public boolean addModeleFeuilleDocument(ModeleFeuilleDocument feuille) {
		if (modelesFeuilleDocument == null) {
			modelesFeuilleDocument = new HashSet<>();
		}
		return modelesFeuilleDocument.add(feuille);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "MODELE_ID", foreignKey = @ForeignKey(name = "FK_FLLE_MODOC_ID"))
	public Set<ModeleFeuilleDocument> getModelesFeuilleDocument() {
		return modelesFeuilleDocument;
	}

	public void setModelesFeuilleDocument(Set<ModeleFeuilleDocument> theModelesFeuilleDocument) {
		modelesFeuilleDocument = theModelesFeuilleDocument;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "PERIODE_ID", insertable = false, updatable = false)
	public PeriodeFiscale getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(PeriodeFiscale thePeriodeFiscale) {
		periodeFiscale = thePeriodeFiscale;
	}

	@Transient
	public boolean possedeModeleFeuilleDocument(int noCADEV){
		for (ModeleFeuilleDocument feuilleDocument : modelesFeuilleDocument) {
			if (noCADEV == feuilleDocument.getNoCADEV()) {
				return true;
			}
		}
		return false;
	}
}
