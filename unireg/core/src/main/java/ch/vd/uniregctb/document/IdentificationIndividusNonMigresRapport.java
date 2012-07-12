package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IdentIndividusNonMigresRapport")
public class IdentificationIndividusNonMigresRapport extends Document {

	public IdentificationIndividusNonMigresRapport() {
	}
}
