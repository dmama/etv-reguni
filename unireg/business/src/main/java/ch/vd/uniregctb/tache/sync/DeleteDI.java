package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action permettant d'annuler une déclaration d'impôt soit directement soit en créant une tâche d'annulation.
 */
public class DeleteDI extends SynchronizeAction {
	public final Long diId;

	// quelques données pour le toString
	private final TypeContribuable typeContribuable;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	/**
	 * Vrai si la déclaration peut être annulée directement sans passer par une tâche d'annulation.
	 */
	public final boolean directAnnulation;

	public DeleteDI(DeclarationImpotOrdinaire declaration) {
		this.diId = declaration.getId();
		this.typeContribuable = declaration.getTypeContribuable();
		this.dateDebut = declaration.getDateDebut();
		this.dateFin = declaration.getDateFin();

		// Voir la spécification "Engendrer une tâche en instance" : lorsqu'une DI émise ou sommée (mais pas retournée ni échue) doit être annulée,
		// on l'annule immédiatement (généralisation des cas particuliers des départs HC, des mariages et des divorces).
		final EtatDeclaration dernierEtat = declaration.getDernierEtat();
		this.directAnnulation = (dernierEtat != null && (dernierEtat.getEtat() == TypeEtatDeclaration.EMISE || dernierEtat.getEtat() == TypeEtatDeclaration.SOMMEE));
	}

	@Override
	public void execute(Context context) {

		final DeclarationImpotOrdinaire declaration = context.diDAO.get(diId);
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
	public boolean willChangeEntity() {
		return directAnnulation;
	}

	@Override
	public String toString() {
		// [UNIREG-3031] Certaines anciennes DIs ne possèdent pas de type de contribuable connu
		final String descriptionTypeContribuable = (typeContribuable == null ? "de type inconnu" : typeContribuable.description());
		return String.format("création d'une tâche d'annulation la déclaration d'impôt %s couvrant la période du %s au %s", descriptionTypeContribuable,
				RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin));
	}
}
