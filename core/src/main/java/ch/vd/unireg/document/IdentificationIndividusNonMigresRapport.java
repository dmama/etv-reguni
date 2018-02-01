package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IdentIndividusNonMigresRapport")
public class IdentificationIndividusNonMigresRapport extends Document {

	public IdentificationIndividusNonMigresRapport() {
	}
}
