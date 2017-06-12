package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Raphaël Marmier, 2017-06-06, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue("DesappariementREERapport")
public class DesappariementREERapport extends Document {

	public DesappariementREERapport() {
	}

	public DesappariementREERapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
