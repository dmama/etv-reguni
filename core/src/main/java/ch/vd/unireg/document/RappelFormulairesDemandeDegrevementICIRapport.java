package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@SuppressWarnings("UnusedDeclaration")
@Entity
@DiscriminatorValue("RappelFormulairesDemandeDegrevementICIRapport")
public class RappelFormulairesDemandeDegrevementICIRapport extends Document {

	public RappelFormulairesDemandeDegrevementICIRapport() {
	}

	public RappelFormulairesDemandeDegrevementICIRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
