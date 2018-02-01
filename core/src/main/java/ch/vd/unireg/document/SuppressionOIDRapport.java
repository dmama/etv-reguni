package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("SuppressionOIDRapport ")
public class SuppressionOIDRapport extends Document {

	public SuppressionOIDRapport() {
	}

	public SuppressionOIDRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
