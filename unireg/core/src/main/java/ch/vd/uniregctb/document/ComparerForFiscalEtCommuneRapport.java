package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ComparerForFiscalEtCommuneRapport")
public class ComparerForFiscalEtCommuneRapport extends Document{

	private static final long serialVersionUID = 8809695370053422078L;

	public ComparerForFiscalEtCommuneRapport() {
	}

	public ComparerForFiscalEtCommuneRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
