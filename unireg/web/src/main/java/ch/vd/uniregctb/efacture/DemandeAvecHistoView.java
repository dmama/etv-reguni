package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */

@SuppressWarnings("UnusedDeclaration")
public class DemandeAvecHistoView {

	private String idDemande;
	private RegDate dateDemande;
	private List<EtatDemandeView> etats;

	public String getIdDemande() {
		return idDemande;
	}

	public void setIdDemande(String idDemande) {
		this.idDemande = idDemande;
	}

	public List<EtatDemandeView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDemandeView> etats) {
		if (etats == null || etats.isEmpty()) {
			throw new IllegalArgumentException("etats ne peut être ni null ni vide");
		}
		this.etats = new ArrayList<EtatDemandeView>(etats);
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public EtatDemandeView getEtatCourant() {
		return etats.get(0);
	}

	private TypeEtatDemande getTypeEtatCourant() {
		return getEtatCourant().getType();
	}

	public boolean isValidable() {
		return getEtatCourant().isValidable();
	}

	public boolean isRefusable() {
		return getEtatCourant().isRefusable();
	}

	public boolean isMettableEnAttenteContact() {
		return getEtatCourant().isMettableEnAttenteContact();
	}

	public boolean isMettableEnAttenteSignature() {
		return getEtatCourant().isMettableEnAttenteSignature();
	}
}
