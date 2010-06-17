package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action permettant d'annuler une déclaration d'impôt soit directement soit en créant une tâche d'annulation.
 */
public class DeleteDI extends SynchronizeAction {
	public final DeclarationImpotOrdinaire declaration;

	/**
	 * Vrai si la déclaration peut être annulée directement sans passer par une tâche d'annulation.
	 */
	public final boolean directAnnulation;

	public DeleteDI(DeclarationImpotOrdinaire declaration) {
		this.declaration = declaration;

		// Voir la spécification "Engendrer une tâche en instance" : lorsqu'une DI émise ou sommée (mais pas retournée ni échue) doit être annulée,
		// on l'annule immédiatement (généralisation des cas particuliers des départs HC, des mariages et des divorces).
		final EtatDeclaration dernierEtat = declaration.getDernierEtat();
		this.directAnnulation = (dernierEtat != null && (dernierEtat.getEtat() == TypeEtatDeclaration.EMISE || dernierEtat.getEtat() == TypeEtatDeclaration.SOMMEE));
	}

	@Override
	public void execute(Context context) {

		if (directAnnulation) {
			// Voir la spécification "Engendrer une tâche en instance" : lorsqu'une DI émise ou sommée (mais pas retournée ni échue) doit être annulée,
			// on l'annule immédiatement (généralisation des cas particuliers des départs HC, des mariages et des divorces).
			context.diService.annulationDI(context.contribuable, declaration, RegDate.get());
		}
		else {
			final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, context.contribuable, declaration, context.collectivite);
			context.tacheDAO.save(tache);
		}
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'annulation la déclaration d'impôt %s couvrant la période du %s au %s", declaration.getTypeContribuable().description(),
				RegDateHelper.dateToDisplayString(declaration.getDateDebut()), RegDateHelper.dateToDisplayString(declaration.getDateFin()));
	}
}
