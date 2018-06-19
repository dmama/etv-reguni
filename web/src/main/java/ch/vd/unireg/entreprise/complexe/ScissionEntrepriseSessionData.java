package ch.vd.unireg.entreprise.complexe;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * Structure de données posée en session sous le nom {@link ScissionEntrepriseController#SCISSION_NAME}
 */
public class ScissionEntrepriseSessionData implements Serializable {

	private static final long serialVersionUID = -5716475454427178862L;

	private final long idEntrepriseScindee;
	private final RegDate dateContratScission;
	private final Map<Long, EntrepriseResultante> entreprisesResultantes = new LinkedHashMap<>();

	public ScissionEntrepriseSessionData(long idEntrepriseScindee, RegDate dateContratScission) {
		this.idEntrepriseScindee = idEntrepriseScindee;
		this.dateContratScission = dateContratScission;
	}

	public Iterable<EntrepriseResultante> getEntreprisesResultantes() {
		return Collections.unmodifiableCollection(entreprisesResultantes.values());
	}

	public Set<Long> getIdsEntreprisesResultantes() {
		return Collections.unmodifiableSet(entreprisesResultantes.keySet());
	}

	public void addEntrepriseResultante(EntrepriseResultante entreprise) {
		entreprisesResultantes.put(entreprise.getId(), entreprise);
	}

	public void removeEntrepriseResultante(long idEntrepriseAbsorbee) {
		entreprisesResultantes.remove(idEntrepriseAbsorbee);
	}

	public long getIdEntrepriseScindee() {
		return idEntrepriseScindee;
	}

	public RegDate getDateContratScission() {
		return dateContratScission;
	}

	public boolean isToutesDatesInscriptionEnPhaseAvecDateContratScission() {
		for (EntrepriseResultante er : entreprisesResultantes.values()) {
			if (er.dateInscription != dateContratScission) {
				return false;
			}
		}
		return true;
	}

	public static final class EntrepriseResultante implements Serializable {

		private static final long serialVersionUID = 1807071015594584721L;

		private final long id;
		private final String numeroIDE;
		private final String raisonSociale;
		private final RegDate dateInscription;
		private final String nomSiege;
		private final FormeLegale formeJuridique;
		private final TypeEtatEntreprise etatActuel;

		public EntrepriseResultante(TiersIndexedDataView data) {
			this.id = data.getNumero();
			this.numeroIDE = data.getNumeroIDE();
			this.raisonSociale = data.getNom1();
			this.dateInscription = data.getRegDateNaissanceInscriptionRC();
			this.nomSiege = data.getDomicileEtablissementPrincipal();
			this.formeJuridique = data.getFormeJuridique();
			this.etatActuel = data.getEtatEntreprise();
		}

		public long getId() {
			return id;
		}

		public String getNumeroIDE() {
			return numeroIDE;
		}

		public String getRaisonSociale() {
			return raisonSociale;
		}

		public RegDate getDateInscription() {
			return dateInscription;
		}

		public String getNomSiege() {
			return nomSiege;
		}

		public FormeLegale getFormeJuridique() {
			return formeJuridique;
		}

		public TypeEtatEntreprise getEtatActuel() {
			return etatActuel;
		}
	}
}
