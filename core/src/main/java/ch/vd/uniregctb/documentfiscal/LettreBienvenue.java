package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

@Entity
@DiscriminatorValue(value = "LettreBienvenue")
public class LettreBienvenue extends AutreDocumentFiscalAvecSuivi {

	private TypeLettreBienvenue type;

	@Column(name = "LB_TYPE", length = LengthConstants.LETTRE_BIENVENUE_TYPE)
	@Enumerated(EnumType.STRING)
	public TypeLettreBienvenue getType() {
		return type;
	}

	public void setType(TypeLettreBienvenue type) {
		this.type = type;
	}
}
