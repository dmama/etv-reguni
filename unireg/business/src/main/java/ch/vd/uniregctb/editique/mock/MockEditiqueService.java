package ch.vd.uniregctb.editique.mock;

import javax.jms.JMSException;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;

public class MockEditiqueService implements EditiqueService {

	public EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException, JMSException {
		return null;
	}

	public void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException {
	}

	public byte[] getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException {
		return new byte[0];
	}
}
