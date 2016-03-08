package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.common.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.param.ModeleFeuilleDocumentComparator;
import ch.vd.uniregctb.type.TypeDocument;

@SuppressWarnings("UnusedDeclaration")
public class ImprimerDuplicataDeclarationImpotView {

	private Long idDI;
	private TypeDocument selectedTypeDocument;
	private List<ModeleDocumentView> modelesDocumentView;
	private Boolean toSave;

	public ImprimerDuplicataDeclarationImpotView() {
	}

	public ImprimerDuplicataDeclarationImpotView(DeclarationImpotOrdinaire di, ModeleDocumentDAO modeleDocumentDAO) {
		this.idDI = di.getId();
		this.selectedTypeDocument = initTypeDocument(di);
		this.setModelesDocumentView(initModelesDocuments(di, modeleDocumentDAO));
		this.toSave = null;
	}

	private static List<ModeleDocumentView> initModelesDocuments(DeclarationImpotOrdinaire di, ModeleDocumentDAO modeleDocumentDAO) {

		final List<ModeleDocument> modelesDocument = modeleDocumentDAO.getByPeriodeFiscale(di.getPeriode());

		// [UNIREG-2001] si une annexe est dans la partie "LOCAL" mais pas dans la partie "BATCH", on ne la demande pas par défaut
		// (on stocke ici donc tous les formulaires existants dans la partie "BATCH" pour un contrôle rapide)
		final ModeleDocument modeleDocumentCompleteBatch = findModeleOfType(modelesDocument, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		final Set<Integer> numerosCADEVBatch = new HashSet<>();
		if (modeleDocumentCompleteBatch != null) {
			for (ModeleFeuilleDocument modeleFeuille : modeleDocumentCompleteBatch.getModelesFeuilleDocument()) {
				numerosCADEVBatch.add(modeleFeuille.getNoCADEV());
			}
		}

		final List<ModeleDocumentView> modelesDocumentView = new ArrayList<>();
		for (ModeleDocument modele : modelesDocument) {
			if (modele.getTypeDocument() != TypeDocument.LISTE_RECAPITULATIVE && modele.getTypeDocument() != TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {

				// [SIFISC-2066] on trie les feuilles dans l'ordre spécifié dans la paramétrisation
				final List<ModeleFeuilleDocument> modelesFeuilleDocument = new ArrayList<>(modele.getModelesFeuilleDocument());
				Collections.sort(modelesFeuilleDocument, new ModeleFeuilleDocumentComparator());

				final List<ModeleFeuilleDocumentEditique> modelesFeuilleDocumentView = new ArrayList<>();
				for (ModeleFeuilleDocument modeleFeuilleDocument : modelesFeuilleDocument) {

					// [UNIREG-2001] si une annexe est dans la partie "LOCAL" mais pas dans la partie "BATCH", on ne la demande pas par défaut
					final int nbreFeuilles;
					if (!numerosCADEVBatch.isEmpty() && modele.getTypeDocument() == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
						if (numerosCADEVBatch.contains(modeleFeuilleDocument.getNoCADEV())) {
							nbreFeuilles = 1;
						}
						else {
							nbreFeuilles = 0;
						}
					}
					else {
						nbreFeuilles = 1;
					}

					final ModeleFeuilleDocumentEditique modeleFeuilleDocumentView = new ModeleFeuilleDocumentEditique(modeleFeuilleDocument, nbreFeuilles);
					modelesFeuilleDocumentView.add(modeleFeuilleDocumentView);
				}

				final ModeleDocumentView modeleView = new ModeleDocumentView();
				modeleView.setTypeDocument(modele.getTypeDocument());
				modeleView.setModelesFeuilles(modelesFeuilleDocumentView);
				modelesDocumentView.add(modeleView);
			}
		}
		return modelesDocumentView;
	}

	/**
	 * Renvoie le modèle de document du type recherché présent dans la liste, ou <code>null</code> s'il n'y en a pas
	 *
	 * @param modeles les modèles à fouiller
	 * @param type    le type recherché
	 * @return le premier modèle trouvé dans la liste correspondant au type recherché
	 */
	private static ModeleDocument findModeleOfType(Collection<ModeleDocument> modeles, TypeDocument type) {
		for (ModeleDocument modele : modeles) {
			if (modele.getTypeDocument() == type) {
				return modele;
			}
		}
		return null;
	}

	private static TypeDocument initTypeDocument(DeclarationImpotOrdinaire di) {
		final TypeDocument t;
		if (di.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
			t = TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL;
		}
		else {
			t = di.getTypeDeclaration();
		}
		return t;
	}

	public Long getIdDI() {
		return idDI;
	}

	public void setIdDI(Long idDI) {
		this.idDI = idDI;
	}

	public TypeDocument getSelectedTypeDocument() {
		return selectedTypeDocument;
	}

	public void setSelectedTypeDocument(TypeDocument selectedTypeDocument) {
		this.selectedTypeDocument = selectedTypeDocument;
	}

	public List<ModeleDocumentView> getModelesDocumentView() {
		return modelesDocumentView;
	}

	public void setModelesDocumentView(List<ModeleDocumentView> modelesDocumentView) {
		this.modelesDocumentView = modelesDocumentView;
	}

	public Boolean getToSave() {
		return toSave;
	}

	public void setToSave(Boolean toSave) {
		this.toSave = toSave;
	}

	public List<ModeleFeuilleDocumentEditique> getSelectedAnnexes() {
		List<ModeleFeuilleDocumentEditique> annexes = null;
		if (modelesDocumentView != null) {
			for (ModeleDocumentView modeleView : modelesDocumentView) {
				if (modeleView.getTypeDocument() == selectedTypeDocument) {
					annexes = modeleView.getModelesFeuilles();
					break;
				}
			}
		}
		return annexes;
	}
}