package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DatabaseIndexationRapport")
public class DatabaseIndexationRapport extends Document {

	public DatabaseIndexationRapport() {
	}
}
