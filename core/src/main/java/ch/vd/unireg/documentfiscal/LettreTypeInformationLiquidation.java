package ch.vd.unireg.documentfiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "LettreTypeInfoLiquidation")
public class LettreTypeInformationLiquidation extends AutreDocumentFiscal {
}
