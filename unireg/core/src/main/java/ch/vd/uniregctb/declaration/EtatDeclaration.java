package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscal;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 */
@Entity
public abstract class EtatDeclaration extends EtatDocumentFiscal<EtatDeclaration> implements LinkedEntity {

	public EtatDeclaration() {
		super();
	}

	public EtatDeclaration(RegDate dateObtention) {
		super(dateObtention);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_ET_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public Declaration getDeclaration() {
		// begin-user-code
		return (Declaration) getDocumentFiscal();
		// end-user-code
	}

	public void setDeclaration(Declaration theDeclaration) {
		// begin-user-code
		setDocumentFiscal(theDeclaration);
		// end-user-code
	}

}
