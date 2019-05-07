package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiFormulairesDemandeDegrevementICIRapport")
public class EnvoiFormulairesDemandeDegrevementICIRapport extends Document {

	public EnvoiFormulairesDemandeDegrevementICIRapport() {
	}

	public EnvoiFormulairesDemandeDegrevementICIRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
