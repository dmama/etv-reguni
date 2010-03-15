package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DetDIsRapport")
public class DeterminationDIsRapport extends Document {

	private static final long serialVersionUID = 5775674572053819435L;

	public DeterminationDIsRapport() {
	}

	public DeterminationDIsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
