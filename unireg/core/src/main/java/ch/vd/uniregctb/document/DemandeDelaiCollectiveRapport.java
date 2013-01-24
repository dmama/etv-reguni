package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DemDelaiCollRapport")
public class DemandeDelaiCollectiveRapport extends Document {

	private static final long serialVersionUID = 6265360263330461631L;

	public DemandeDelaiCollectiveRapport() {
	}

	public DemandeDelaiCollectiveRapport(String nom, String fileExtension, String description, String fileName, String subPath,
			long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
