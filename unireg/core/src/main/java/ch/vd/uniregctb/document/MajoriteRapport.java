package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("MajoriteRapport")
public class MajoriteRapport extends Document {


	/**
	 *
	 */
	private static final long serialVersionUID = 5274822372533490368L;

	public MajoriteRapport() {
	}

	public MajoriteRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
