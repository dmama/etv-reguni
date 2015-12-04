package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action permettant d'ajouter une tâche d'envoi de déclaration d'impôt PM.
 */
public class AddDIPM extends AddDI<PeriodeImpositionPersonnesMorales> {

	public AddDIPM(PeriodeImpositionPersonnesMorales periodeImposition) {
		super(periodeImposition);
	}

	@Override
	public void execute(Context context) {
		final RegDate today = getToday();
		final RegDate dateEcheance = TacheEnvoiDeclarationImpot.getDefaultEcheance(today);
		final TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpotPM(TypeEtatTache.EN_INSTANCE,
		                                                                          dateEcheance,
		                                                                          (ContribuableImpositionPersonnesMorales) context.contribuable,
		                                                                          periodeImposition.getDateDebut(),
		                                                                          periodeImposition.getDateFin(),
		                                                                          periodeImposition.getTypeContribuable(),
		                                                                          periodeImposition.getTypeDocumentDeclaration(),
		                                                                          context.collectivite);
		context.tacheDAO.save(tache);
	}

	/**
	 * Surchargeable pour les tests afin de choisir quelle date est choisie comme date de référence (pour le calcul de la date d'échéance de la tâche)
	 * @return la date qui doit être considérée comme la date du jour
	 */
	protected RegDate getToday() {
		return RegDate.get();
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'émission de %s %s couvrant la période du %s au %s",
		                     periodeImposition.getTypeDocumentDeclaration().getDescription(),
		                     periodeImposition.getTypeContribuable().description(),
		                     RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}
}
