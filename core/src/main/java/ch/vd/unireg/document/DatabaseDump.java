package ch.vd.unireg.document;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DBDump")
public class DatabaseDump extends Document {

	private int nbTiers;

	public DatabaseDump() {
	}

	public DatabaseDump(String nom, String fileExtension, String description, int nbTiers, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
		this.nbTiers = nbTiers;
	}

	@Column(name = "NB_TIERS")
	public int getNbTiers() {
		return nbTiers;
	}

	public void setNbTiers(int nbTiers) {
		this.nbTiers = nbTiers;
	}
}
