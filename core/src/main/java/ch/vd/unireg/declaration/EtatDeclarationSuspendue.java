package ch.vd.unireg.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Déclaration suspendue, aucun rappel, aucune sommation ni échéance ne doit pouvoir être généré
 * tant qu'un tel état non-annulé existe
 */
@Entity
@DiscriminatorValue("DI_SUSPENDUE")
public class EtatDeclarationSuspendue extends EtatDeclaration {

	public EtatDeclarationSuspendue() {
		super();
	}

	public EtatDeclarationSuspendue(RegDate dateObtention) {
		super(dateObtention);
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.SUSPENDU;
	}
}
