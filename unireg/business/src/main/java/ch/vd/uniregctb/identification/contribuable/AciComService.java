package ch.vd.uniregctb.identification.contribuable;

import java.io.File;

import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.identification.contribuable.FichierOrigine;

public interface AciComService {

	public FichierOrigine getMessageFile(String businessId);
}
