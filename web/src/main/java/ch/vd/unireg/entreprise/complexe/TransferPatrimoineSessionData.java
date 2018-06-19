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
 * Structure de données posée en session sous le nom {@link TransfertPatrimoineController#TRANSFERT_NAME}
 */
public class TransferPatrimoineSessionData implements Serializable {

	private static final long serialVersionUID = 6065437163419550925L;

	private final long idEntrepriseEmettrice;
	private final RegDate dateTransfert;
	private final Map<Long, EntrepriseReceptrice> entreprisesReceptrices = new LinkedHashMap<>();

	public TransferPatrimoineSessionData(long idEntrepriseEmettrice, RegDate dateTransfert) {
		this.idEntrepriseEmettrice = idEntrepriseEmettrice;
		this.dateTransfert = dateTransfert;
	}

	public Iterable<EntrepriseReceptrice> getEntreprisesReceptrices() {
		return Collections.unmodifiableCollection(entreprisesReceptrices.values());
	}

	public Set<Long> getIdsEntreprisesReceptrices() {
		return Collections.unmodifiableSet(entreprisesReceptrices.keySet());
	}

	public void addEntrepriseReceptrice(EntrepriseReceptrice entreprise) {
		entreprisesReceptrices.put(entreprise.getId(), entreprise);
	}

	public void removeEntrepriseReceptrice(long idEntrepriseReceptrice) {
		entreprisesReceptrices.remove(idEntrepriseReceptrice);
	}

	public long getIdEntrepriseEmettrice() {
		return idEntrepriseEmettrice;
	}

	public RegDate getDateTransfert() {
		return dateTransfert;
	}

	public static final class EntrepriseReceptrice implements Serializable {

		private static final long serialVersionUID = -3486594155618625029L;

		private final long id;
		private final String numeroIDE;
		private final String raisonSociale;
		private final RegDate dateInscription;
		private final String nomSiege;
		private final FormeLegale formeJuridique;
		private final TypeEtatEntreprise etatActuel;

		public EntrepriseReceptrice(TiersIndexedDataView data) {
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
