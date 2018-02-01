package ch.vd.unireg.situationfamille;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.TiersService;

public class ComparerSituationFamilleResults extends JobResults<Long, ComparerSituationFamilleResults> {
	public boolean isInterrompu() {
		return interrompu;
	}

	public static class InfoSituation {
		public final long id;


		public InfoSituation(long id) {
			this.id = id;

		}
	}

	public static class SituationsDifferentes extends InfoSituation {

		public final long numeroContribuable;
		public final String etatCivil;
		public final RegDate dateDebutEtatCivil;

		public final String etatCivilHost;
		public final RegDate dateDebutEtatCivilHost;


		public SituationsDifferentes(long id, long numeroContribuable, String etatCivil, RegDate dateDebutEtatCivil, String etatCivilHost,
		                             RegDate dateDebutEtatCivilHost) {

			super(id);
			this.numeroContribuable = numeroContribuable;
			this.etatCivil = etatCivil;
			this.dateDebutEtatCivil = dateDebutEtatCivil;

			this.etatCivilHost = etatCivilHost;
			this.dateDebutEtatCivilHost = dateDebutEtatCivilHost;

		}
	}

	public static class Erreur extends InfoSituation {
		public final String message;

		public Erreur(long id, String message) {
			super(id);
			this.message = message;
		}
	}

	public final List<SituationsDifferentes> listeSituationsDifferentes = new ArrayList<>();
	public boolean interrompu;
	public final List<Erreur> erreurs = new ArrayList<>();
	public int nbSituationTotal;
	public final RegDate dateTraitement;

	public ComparerSituationFamilleResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element, e.getMessage()));
	}

	@Override
	public void addAll(ComparerSituationFamilleResults right) {
		this.nbSituationTotal += right.nbSituationTotal;
		listeSituationsDifferentes.addAll(right.listeSituationsDifferentes);
		erreurs.addAll(right.erreurs);
	}

	public void addSituationsDifferentes(SituationFamilleMenageCommun situation, EtatCivil etatCivil) {
		final Long id = situation.getId();
		final Long contribuableId = situation.getContribuablePrincipalId();
		final String etatCivilLocal = situation.getEtatCivil().name();
		final RegDate debutEtatCivilLocal = situation.getDateDebut();
		final String etatCivilHost = EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil()).name();
		final RegDate debutEtatCivilHost = etatCivil.getDateDebut();
		listeSituationsDifferentes.add(new SituationsDifferentes(id, contribuableId, etatCivilLocal, debutEtatCivilLocal, etatCivilHost, debutEtatCivilHost));

	}
}
