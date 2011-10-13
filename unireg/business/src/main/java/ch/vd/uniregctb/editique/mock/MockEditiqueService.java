package ch.vd.uniregctb.editique.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public class MockEditiqueService implements EditiqueService {

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(String nomDocument, TypeDocumentEditique typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException {
		return null;
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive, String description) throws EditiqueException {
		return null;
	}

	@Override
	public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
	}

	@Override
	public InputStream getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument, String contexte) throws EditiqueException {
		return new ByteArrayInputStream(new byte[0]);
	}
}
