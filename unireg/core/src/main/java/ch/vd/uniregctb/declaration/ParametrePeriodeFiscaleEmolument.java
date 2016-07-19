package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeDocumentEmolument;

@Entity
@DiscriminatorValue(value = "EMOL")
public class ParametrePeriodeFiscaleEmolument extends ParametrePeriodeFiscale {

	private TypeDocumentEmolument typeDocument;
	private Integer montant;

	protected ParametrePeriodeFiscaleEmolument() {
	}

	public ParametrePeriodeFiscaleEmolument(TypeDocumentEmolument typeDocument, @Nullable Integer montant, PeriodeFiscale pf) {
		super(pf);
		this.typeDocument = typeDocument;
		this.montant = montant;
	}

	@Column(name = "EMOL_TYPE_DOCUMENT", length = LengthConstants.PARAMETRE_PF_TYPE_DOCUMENT_EMOLUMENT)
	@Enumerated(EnumType.STRING)
	public TypeDocumentEmolument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocumentEmolument typeDocument) {
		this.typeDocument = typeDocument;
	}

	@Column(name = "EMOL_MONTANT", nullable = true)
	public Integer getMontant() {
		return montant;
	}

	public void setMontant(Integer montant) {
		this.montant = montant;
	}
}
