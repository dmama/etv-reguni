package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Action permettant d'annuler une tâche devenue obsolète.
 */
public class AnnuleTache implements TacheSynchronizeAction {

	public final Tache tache;

	public AnnuleTache(Tache tache) {
		this.tache = tache;
	}

	@Override
	public void execute(Context context) {
		tache.setAnnule(true);
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public int getPeriodeFiscale() {
		if (tache instanceof TacheAnnulationDeclarationImpot) {
			return ((TacheAnnulationDeclarationImpot) tache).getDeclaration().getPeriode().getAnnee();
		}
		else if (tache instanceof TacheAnnulationQuestionnaireSNC) {
			return ((TacheAnnulationQuestionnaireSNC) tache).getDeclaration().getPeriode().getAnnee();
		}
		else if (tache instanceof TacheEnvoiDeclarationImpot) {
			return ((TacheEnvoiDeclarationImpot) tache).getDateFin().year();
		}
		else if (tache instanceof TacheEnvoiQuestionnaireSNC) {
			return ((TacheEnvoiQuestionnaireSNC) tache).getDateFin().year();
		}
		else {
			// qu'est-ce qu'on fait-là, en fait ?
			throw new IllegalArgumentException("Type de tâche non associée à une période fiscale : " + tache.getTypeTache());
		}
	}

	private static String toString(TypeContribuable typeContribuable) {
		if (typeContribuable == null) {
			return "de type inconnu";
		}
		return typeContribuable.description();
	}

	@Override
	public String toString() {
		final String tacheDetail;
		if (tache instanceof TacheAnnulationDeclarationImpot) {
			final TacheAnnulationDeclarationImpot annule = (TacheAnnulationDeclarationImpot) tache;
			final DeclarationImpotOrdinaire di = annule.getDeclaration();
			tacheDetail = String.format("d'annulation de la déclaration d'impôt %s couvrant la période du %s au %s",
			                            toString(di.getTypeContribuable()),
			                            RegDateHelper.dateToDisplayString(di.getDateDebut()),
			                            RegDateHelper.dateToDisplayString(di.getDateFin()));
		}
		else if (tache instanceof TacheAnnulationQuestionnaireSNC) {
			final TacheAnnulationQuestionnaireSNC annule = (TacheAnnulationQuestionnaireSNC) tache;
			final QuestionnaireSNC q = annule.getDeclaration();
			tacheDetail = String.format("d'annulation du questionnaire SNC couvrant la période du %s au %s",
			                            RegDateHelper.dateToDisplayString(q.getDateDebut()),
			                            RegDateHelper.dateToDisplayString(q.getDateFin()));
		}
		else if (tache instanceof TacheEnvoiDeclarationImpotPP) {
			final TacheEnvoiDeclarationImpotPP envoi = (TacheEnvoiDeclarationImpotPP) tache;
			tacheDetail = String.format("d'envoi de la déclaration d'impôt PP %s couvrant la période du %s au %s", toString(envoi.getTypeContribuable()),
					RegDateHelper.dateToDisplayString(envoi.getDateDebut()), RegDateHelper.dateToDisplayString(envoi.getDateFin()));
		}
		else if (tache instanceof TacheEnvoiDeclarationImpotPM) {
			final TacheEnvoiDeclarationImpotPM envoi = (TacheEnvoiDeclarationImpotPM) tache;
			tacheDetail = String.format("d'envoi de la %s %s couvrant la période du %s au %s",
			                            envoi.getTypeDocument().getDescription(),
			                            toString(envoi.getTypeContribuable()),
			                            RegDateHelper.dateToDisplayString(envoi.getDateDebut()),
			                            RegDateHelper.dateToDisplayString(envoi.getDateFin()));
		}
		else if (tache instanceof TacheEnvoiQuestionnaireSNC) {
			final TacheEnvoiQuestionnaireSNC envoi = (TacheEnvoiQuestionnaireSNC) tache;
			tacheDetail = String.format("d'envoi du questionnaire SNC couvrant la période du %s au %s",
			                            RegDateHelper.dateToDisplayString(envoi.getDateDebut()), RegDateHelper.dateToDisplayString(envoi.getDateFin()));
		}
		else if (tache instanceof TacheControleDossier) {
			tacheDetail = "de contrôle de dossier";
		}
		else if (tache instanceof TacheNouveauDossier) {
			tacheDetail = "de nouveau dossier";
		}
		else if (tache instanceof TacheTransmissionDossier) {
			final TacheTransmissionDossier trans = (TacheTransmissionDossier) tache;
			tacheDetail = "de transmission de dossier";
		}
		else {
			throw new IllegalArgumentException("Type de tâche inconnue = [" + tache.getClass().getSimpleName() + ']');
		}

		return "annulation de la tâche " + tacheDetail;
	}
}
