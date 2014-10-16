package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ImportCodesSegmentRapport")
public class ImportCodesSegmentRapport extends Document {

	private static final long serialVersionUID = 8809638700534747648L;

	public ImportCodesSegmentRapport() {
	}

	public ImportCodesSegmentRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
