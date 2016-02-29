package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiSommationLRsRapport")
public class EnvoiSommationLRsRapport extends Document {

	private static final long serialVersionUID = -6740593773387720231L;

	public EnvoiSommationLRsRapport() {
	}

	public EnvoiSommationLRsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
