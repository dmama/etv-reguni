package ch.vd.unireg.entreprise.complexe;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * Structure de données posée en session sous le nom {@link FusionEntreprisesController#FUSION_NAME}
 */
public class FusionEntreprisesSessionData implements Serializable {

	private static final long serialVersionUID = 2252075084359401280L;

	private final long idEntrepriseAbsorbante;
	private final RegDate dateContratFusion;
	private final RegDate dateBilanFusion;
	private final Map<Long, EntrepriseAbsorbee> entreprisesAbsorbees = new LinkedHashMap<>();

	public FusionEntreprisesSessionData(long idEntrepriseAbsorbante, RegDate dateContratFusion, RegDate dateBilanFusion) {
		this.idEntrepriseAbsorbante = idEntrepriseAbsorbante;
		this.dateBilanFusion = dateBilanFusion;
		this.dateContratFusion = dateContratFusion;
	}

	public Iterable<EntrepriseAbsorbee> getEntreprisesAbsorbees() {
		return Collections.unmodifiableCollection(entreprisesAbsorbees.values());
	}

	public Set<Long> getIdsEntreprisesAbsorbees() {
		return Collections.unmodifiableSet(entreprisesAbsorbees.keySet());
	}

	public void addEntrepriseAbsorbee(EntrepriseAbsorbee entreprise) {
		entreprisesAbsorbees.put(entreprise.getId(), entreprise);
	}

	public void removeEntrepriseAbsorbee(long idEntrepriseAbsorbee) {
		entreprisesAbsorbees.remove(idEntrepriseAbsorbee);
	}

	public long getIdEntrepriseAbsorbante() {
		return idEntrepriseAbsorbante;
	}

	public RegDate getDateBilanFusion() {
		return dateBilanFusion;
	}

	public RegDate getDateContratFusion() {
		return dateContratFusion;
	}

	public static final class EntrepriseAbsorbee implements Serializable {

		private static final long serialVersionUID = -7386206293139050447L;

		private final long id;
		private final String numeroIDE;
		private final String raisonSociale;
		private final RegDate dateInscription;
		private final String nomSiege;
		private final FormeLegale formeJuridique;
		private final TypeEtatEntreprise etatActuel;

		public EntrepriseAbsorbee(TiersIndexedDataView data) {
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
