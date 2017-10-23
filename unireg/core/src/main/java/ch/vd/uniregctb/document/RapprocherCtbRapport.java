package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RapprocherCtbRapport")
@Deprecated
public class RapprocherCtbRapport extends Document {

	public RapprocherCtbRapport() {
	}

	public RapprocherCtbRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);

	}

}
