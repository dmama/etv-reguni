package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

/**
 * Action permettant de mettre-à-jour les dates de début/fin et le type de contribuable d'une déclaration d'impôt.
 */
public class UpdateDI extends SynchronizeAction {
	public final PeriodeImposition periodeImposition;
	public final DeclarationImpotOrdinaire declaration;

	public UpdateDI(PeriodeImposition periodeImposition, DeclarationImpotOrdinaire declaration) {
		this.periodeImposition = periodeImposition;
		this.declaration = declaration;
	}

	@Override
	public void execute(Context context) {
		// [UNIREG-1303] Autant que faire se peut, on évite de créer des tâches d'envoi/annulation de DI et on met-à-jour les DIs existantes. L'idée est d'éviter d'incrémenter le numéro de
		// séquence des DIs parce que cela pose des problèmes lors du quittancement, et de toutes façons la période exacte n'est pas imprimée sur les DIs.
		declaration.setDateDebut(periodeImposition.getDateDebut());
		declaration.setDateFin(periodeImposition.getDateFin());
		declaration.setTypeContribuable(periodeImposition.getTypeContribuable());
	}
}
