package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action de synchronization pour l'ajout d'un nouveau questionnaire SNC
 */
public class AddQSNC implements TacheSynchronizeAction {

	public final DateRange range;

	public AddQSNC(DateRange range) {
		this.range = range;
	}

	@Override
	public void execute(Context context) {
		final RegDate today = getToday();
		final RegDate dateEcheance = TacheEnvoiQuestionnaireSNC.getDefaultEcheance(today);
		final TacheEnvoiQuestionnaireSNC tache = new TacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE,
		                                                                        dateEcheance,
		                                                                        (Entreprise) context.contribuable,
		                                                                        range.getDateDebut(),
		                                                                        range.getDateFin(),
		                                                                        context.tiersService.getCategorieEntreprise((Entreprise) context.contribuable, range.getDateFin()),
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
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public int getPeriodeFiscale() {
		return range.getDateFin().year();
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'émission de %s couvrant la période du %s au %s",
		                     TypeDocument.QUESTIONNAIRE_SNC.getDescription(),
		                     RegDateHelper.dateToDisplayString(range.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(range.getDateFin()));
	}
}
