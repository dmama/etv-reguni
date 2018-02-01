package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de la liste des taches en instance par OID.
 *
 * @author baba NGOM
 */
@Entity
@DiscriminatorValue("ListeTachesEnIsntanceParOIDRapport")
public class ListeTachesEnIsntanceParOIDRapport extends Document {

	public ListeTachesEnIsntanceParOIDRapport() {
	}

	public ListeTachesEnIsntanceParOIDRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
