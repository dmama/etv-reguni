package ch.vd.unireg.editique.mock;

import org.apache.xmlbeans.XmlObject;

import ch.vd.technical.esb.ErrorType;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.editique.FormatDocumentEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.editique.impl.EditiqueResultatErreurImpl;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public class MockEditiqueService implements EditiqueService {

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive) throws EditiqueException {
		return null;
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, FichierImpression document, boolean archive) throws EditiqueException {
		return null;
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive, String description) throws EditiqueException {
		return null;
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, FichierImpression document, boolean archive, String description) throws EditiqueException {
		return null;
	}

	@Override
	public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
	}

	@Override
	public String creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, boolean archive) throws EditiqueException {
		return nomDocument;
	}

	@Override
	public EditiqueResultat getPDFDeDocumentDepuisArchive(long noContribuable, TypeDocumentEditique typeDocument, String nomDocument) throws EditiqueException {
		return new EditiqueResultatErreurImpl(nomDocument, "Pas implémenté", ErrorType.TECHNICAL, "500");
	}
}
