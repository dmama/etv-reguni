package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ImportImmeublesRapport")
public class ImportImmeublesRapport extends Document {

	public ImportImmeublesRapport() {
	}

	public ImportImmeublesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
