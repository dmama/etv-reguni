package ch.vd.uniregctb.document;

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

	private static final long serialVersionUID = -5309096750654428461L;

	public ListeTachesEnIsntanceParOIDRapport() {
	}

	public ListeTachesEnIsntanceParOIDRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
