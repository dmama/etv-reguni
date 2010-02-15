package ch.vd.uniregctb.editique.mock;

import java.util.List;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.DelegateEditique;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.impl.EditiqueResultatImpl;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class MockEditiqueService implements EditiqueService {

	public String creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException {
		return null;
	}

	public void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException {
	}

	public EditiqueResultat getDocument(String nomFichier, boolean appliqueDelai) {
		EditiqueResultat editiqueResultat = new EditiqueResultatImpl();

		return editiqueResultat;
	}

	public byte[] getPDFDocument(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException {
		return null;
	}

	public int getReceiveTimeout() {
		return 0;
	}

	public void setDelegate(DelegateEditique delegate) {
	}

	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
		return "";
	}

	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
			List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException {
		return "";
	}

	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
	}

	public String imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, InfrastructureException {
		return "";
	}

	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
	}

	public String imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
		return "";

	}

	public String imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException {
		// TODO Auto-generated method stub
		return "";
	}

	public String imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di,
			DelaiDeclaration delai) {
		return "";
	}
	public String imprimeTaxationOfficeOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		return "";
	}

	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	public String imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		return "";
	}

	public void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	public void imprimeTaxationOfficeBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException {
	}

	public String envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) {
		return "";
	}
}
