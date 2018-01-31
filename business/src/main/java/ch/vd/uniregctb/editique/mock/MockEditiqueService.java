package ch.vd.uniregctb.editique.mock;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.technical.esb.ErrorType;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.FormatDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.impl.EditiqueResultatErreurImpl;

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
	public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, boolean archive) throws EditiqueException {
	}

	@Override
	public EditiqueResultat getPDFDeDocumentDepuisArchive(long noContribuable, TypeDocumentEditique typeDocument, String nomDocument) throws EditiqueException {
		return new EditiqueResultatErreurImpl(nomDocument, "Pas implémenté", ErrorType.TECHNICAL, "500");
	}
}
