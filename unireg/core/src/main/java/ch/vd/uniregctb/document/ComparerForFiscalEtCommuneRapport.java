package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ComparerForFiscalEtCommuneRapport")
public class ComparerForFiscalEtCommuneRapport extends Document{

	public ComparerForFiscalEtCommuneRapport() {
	}

	public ComparerForFiscalEtCommuneRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
