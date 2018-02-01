package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ReinitDoubleGainRapport")
public class ReinitialiserBaremeDoubleGainRapport extends Document {

	public ReinitialiserBaremeDoubleGainRapport() {
	}

	public ReinitialiserBaremeDoubleGainRapport(String nom, String fileExtension, String description, String fileName, String subPath,
			long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
