package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("StatsEvenementsRapport")
public class StatistiquesEvenementsRapport extends Document {

	public StatistiquesEvenementsRapport() {
	}

	public StatistiquesEvenementsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
