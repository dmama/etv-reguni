package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action permettant d'ajouter une tâche d'envoi de déclaration d'impôt PP.
 */
public class AddDIPP extends AddDI<PeriodeImpositionPersonnesPhysiques> {

	public AddDIPP(PeriodeImpositionPersonnesPhysiques periodeImposition) {
		super(periodeImposition);
	}

	@Override
	public void execute(Context context) {

		final RegDate dateEcheance;
		final CollectiviteAdministrative collectivite;
		if (periodeImposition.getCauseFermeture() == PeriodeImposition.CauseFermeture.VEUVAGE_DECES) {
			// [UNIREG-2305] En cas de décès, l'échéance de la tâche est poussée 30 jours plus tard et on assigne la tâche à la collectivité ad'hoc
			dateEcheance = periodeImposition.getDateFin().addDays(30);
			collectivite = context.caApresDeces;
		}
		else {
			final RegDate today = getToday();
			final int todaysYear = today.year();
			final int year = periodeImposition.getPeriodeFiscale();
			final PeriodeFiscale pf = context.periodeFiscaleDAO.getPeriodeFiscaleByYear(year);
			final RegDate dateFinEnvoiMasseDI = pf.getParametrePeriodeFiscalePP(periodeImposition.getTypeContribuable()).getDateFinEnvoiMasseDI();
			if (todaysYear == year + 1 && RegDateHelper.isBefore(today, dateFinEnvoiMasseDI, NullDateBehavior.LATEST)) {
				// si on est dans la période de début d'année d'envoi des déclarations d'impôt, l'échéance de la tâche doit être placée en conséquence
				// (sauf si cela fait arriver la tâche à échéance avant le processus normal)
				dateEcheance = RegDateHelper.maximum(dateFinEnvoiMasseDI, Tache.getDefaultEcheance(today), NullDateBehavior.EARLIEST);
			}
			else {
				// autrement, on prend les valeurs par défaut
				dateEcheance = TacheEnvoiDeclarationImpot.getDefaultEcheance(today);
			}
			collectivite = context.collectivite;
		}

		final TacheEnvoiDeclarationImpot tache =
				new TacheEnvoiDeclarationImpotPP(TypeEtatTache.EN_INSTANCE, dateEcheance, (ContribuableImpositionPersonnesPhysiques) context.contribuable, periodeImposition.getDateDebut(), periodeImposition.getDateFin(),
				                                 periodeImposition.getTypeContribuable(), periodeImposition.getTypeDocumentDeclaration(), null, periodeImposition.getCodeSegment(),
				                                 periodeImposition.getAdresseRetour(), collectivite);
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
		return String.format("création d'une tâche d'émission de déclaration d'impôt PP %s couvrant la période du %s au %s",
		                     periodeImposition.getTypeContribuable().description(),
		                     RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}
}
