package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiSommationsDIsRapport")
public class EnvoiSommationsDIsRapport extends Document {

	private static final long serialVersionUID = 752743137533620814L;

	public EnvoiSommationsDIsRapport() {
	}

	public EnvoiSommationsDIsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
