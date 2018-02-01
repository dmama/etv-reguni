package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DumpPeriodesImpositionImpotSourceRapport")
public class DumpPeriodesImpositionImpotSourceRapport extends Document {

	public DumpPeriodesImpositionImpotSourceRapport() {
	}

	public DumpPeriodesImpositionImpotSourceRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}