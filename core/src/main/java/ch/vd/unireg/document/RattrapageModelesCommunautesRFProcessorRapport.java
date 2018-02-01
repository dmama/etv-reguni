package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de rattrapage des regroupements des communautés RF.
 */
@Entity
@DiscriminatorValue("RattrapageModelesCommunautesRFProcessorRapport")
public class RattrapageModelesCommunautesRFProcessorRapport extends Document {

	public RattrapageModelesCommunautesRFProcessorRapport() {
	}

	public RattrapageModelesCommunautesRFProcessorRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}