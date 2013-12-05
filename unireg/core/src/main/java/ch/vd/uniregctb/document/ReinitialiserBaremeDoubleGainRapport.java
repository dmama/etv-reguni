package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ReinitDoubleGainRapport")
public class ReinitialiserBaremeDoubleGainRapport extends Document {

	private static final long serialVersionUID = 3640102509362376402L;

	public ReinitialiserBaremeDoubleGainRapport() {
	}

	public ReinitialiserBaremeDoubleGainRapport(String nom, String fileExtension, String description, String fileName, String subPath,
			long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
