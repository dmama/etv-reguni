package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ImportCodesSegmentRapport")
public class ImportCodesSegmentRapport extends Document {

	public ImportCodesSegmentRapport() {
	}

	public ImportCodesSegmentRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
