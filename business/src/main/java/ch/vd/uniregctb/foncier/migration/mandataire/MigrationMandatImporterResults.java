package ch.vd.uniregctb.foncier.migration.mandataire;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tiers.Contribuable;

public class MigrationMandatImporterResults extends AbstractJobResults<DonneesMandat, MigrationMandatImporterResults> {

	public static class Erreur {
		public final DonneesMandat mandat;
		public final String erreur;

		public Erreur(DonneesMandat mandat, String erreur) {
			this.mandat = mandat;
			this.erreur = erreur;
		}
	}

	public final RegDate dateDebutMandats;
	public final GenreImpotMandataire genreImpot;

	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<String> lignesIgnorees = new LinkedList<>();
	private final List<DonneesMandat> mandatsCrees = new LinkedList<>();

	public MigrationMandatImporterResults(RegDate dateDebutMandats, GenreImpotMandataire genreImpot) {
		this.dateDebutMandats = dateDebutMandats;
		this.genreImpot = genreImpot;
	}

	@Override
	public void addErrorException(DonneesMandat element, Exception e) {
		erreurs.add(new Erreur(element, ExceptionUtils.extractCallStack(e)));
	}

	@Override
	public void addAll(MigrationMandatImporterResults right) {
		erreurs.addAll(right.erreurs);
		mandatsCrees.addAll(right.mandatsCrees);
		lignesIgnorees.addAll(right.lignesIgnorees);
	}

	public void addContribuableInconnu(DonneesMandat mandat) {
		erreurs.add(new Erreur(mandat, "Contribuable inconnu."));
	}

	public void addNpaLocaliteInconnu(DonneesMandat mandat) {
		erreurs.add(new Erreur(mandat, "Couple NPA/localité inconnu."));
	}

	public void addContribuableNonAcceptable(Contribuable ctb, DonneesMandat mandat) {
		erreurs.add(new Erreur(mandat, "Le contribuable visé n'est pas d'un type acceptable ici (" + ctb.getClass().getSimpleName() + ")"));
	}

	public void addLigneInvalide(String ligne) {
		lignesIgnorees.add(ligne);
	}

	public void addAdresseMandataireCree(DonneesMandat mandat) {
		mandatsCrees.add(mandat);
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<String> getLignesIgnorees() {
		return lignesIgnorees;
	}

	public List<DonneesMandat> getMandatsCrees() {
		return mandatsCrees;
	}
}
