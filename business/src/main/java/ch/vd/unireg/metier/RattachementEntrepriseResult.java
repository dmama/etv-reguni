package ch.vd.unireg.metier;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Classe servant au feedback de l'opération de rattachement. Si des établissements secondaires
 * n'ont pas pu être rapprochés, isPartiel() renvoie true et deux listes sont renseignées. La
 * liste des etablissementsCivilsNonRattaches contient les établissements RCEnt que l'on n'a pu rapprocher d'une
 * part. D'autre part, la liste des etablissementsNonRapproches contient les établissements en
 * base qui n'ont pas trouvé d'équivalent civil.
 *
 * @author Raphaël Marmier, 2016-03-21, <raphael.marmier@vd.ch>
 */
public class RattachementEntrepriseResult {

	private final Entreprise entrepriseRattachee;
	private final List<Etablissement> etablissementsRattaches = new ArrayList<>();
	private final List<Etablissement> etablissementsNonRattaches = new ArrayList<>();
	private final List<EtablissementCivil> etablissementsCivilsNonRattaches = new ArrayList<>();

	public RattachementEntrepriseResult(Entreprise entrepriseRattachee) {
		this.entrepriseRattachee = entrepriseRattachee;
	}

	/**
	 * @return true si des établissements RCEnt n'ont pu être rattachés à un établissement en base.
	 */
	public boolean isPartiel() {
		return !etablissementsCivilsNonRattaches.isEmpty() || !etablissementsNonRattaches.isEmpty();
	}

	public void addEtablissementRattache(Etablissement etablissement) {
		etablissementsRattaches.add(etablissement);
	}

	public void addEtablissementNonRattache(Etablissement etablissement) {
		etablissementsNonRattaches.add(etablissement);
	}

	public void addEtablissementCivilNonRattache(EtablissementCivil etablissement) {
		etablissementsCivilsNonRattaches.add(etablissement);
	}

	public Entreprise getEntrepriseRattachee() {
		return entrepriseRattachee;
	}

	public List<Etablissement> getEtablissementsRattaches() {
		return etablissementsRattaches;
	}

	public List<Etablissement> getEtablissementsNonRattaches() {
		return etablissementsNonRattaches;
	}

	public List<EtablissementCivil> getEtablissementsCivilsNonRattaches() {
		return etablissementsCivilsNonRattaches;
	}
}
