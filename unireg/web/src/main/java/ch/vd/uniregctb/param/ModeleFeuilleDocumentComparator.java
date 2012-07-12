package ch.vd.uniregctb.param;

import java.util.Comparator;

import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;

public class ModeleFeuilleDocumentComparator implements Comparator<ModeleFeuilleDocument> {
	@Override
	public int compare(ModeleFeuilleDocument o1, ModeleFeuilleDocument o2) {
		if (o1.getIndex() == null || o2.getIndex() == null) {
			// pas d'indexe renseigné (données historiques) : on se rabat sur les numéros de formulaire
			if (o1.getNumeroFormulaire() == null && o2.getNumeroFormulaire() == null) {
				return 0;
			}
			if (o1.getNumeroFormulaire() == null) {
				return -1;
			}
			if (o2.getNumeroFormulaire() == null) {
				return 1;
			}
			return o1.getNumeroFormulaire().compareTo(o2.getNumeroFormulaire());
		}
		else {
			// [SIFISC-2066] on trie par numéro d'index croissant
			return o1.getIndex().compareTo(o2.getIndex());
		}
	}
}
