package ch.vd.uniregctb.documentfiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "LettreLiquidation")
public class LettreLiquidation extends AutreDocumentFiscal {
}
