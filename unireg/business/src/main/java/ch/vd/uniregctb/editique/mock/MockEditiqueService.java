package ch.vd.uniregctb.editique.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;

public class MockEditiqueService implements EditiqueService {

	public EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException {
		return null;
	}

	public void creerDocumentParBatch(String nomDocument, String typeDocument, XmlObject document, boolean archive) throws EditiqueException {
	}

	public InputStream getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument, String contexte) throws EditiqueException {
		return new ByteArrayInputStream(new byte[0]);
	}
}
