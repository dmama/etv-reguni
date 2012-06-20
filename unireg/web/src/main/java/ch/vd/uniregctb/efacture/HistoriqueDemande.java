package ch.vd.uniregctb.efacture;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
public class HistoriqueDemande {

	private String idDemande;
	private RegDate dateDemande;
	private List<EtatDemande> etats;

	public String getIdDemande() {
		return idDemande;
	}

	public void setIdDemande(String idDemande) {
		this.idDemande = idDemande;
	}

	public List<EtatDemande> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDemande> etats) {
		this.etats = etats;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	@Nullable
	public EtatDemande getEtatCourant() {
		return etats != null && etats.size() > 0 ? etats.get(0) : null;
	}

	@Nullable
	private TypeEtatDemande getTypeEtatCourant() {
		final EtatDemande etatCourant = getEtatCourant();
		return etatCourant != null ? etatCourant.getType() : null;
	}

	public boolean isValidable() {
		final TypeEtatDemande type = getTypeEtatCourant();
		return type != null && type.isValidable();
	}

	public boolean isRefusable() {
		final TypeEtatDemande type = getTypeEtatCourant();
		return type != null && type.isRefusable();
	}

	public boolean isMettableEnAttenteContact() {
		final TypeEtatDemande type = getTypeEtatCourant();
		return type != null && type.isMettableEnAttenteContact();
	}

	public boolean isMettableEnAttenteSignature() {
		final TypeEtatDemande type = getTypeEtatCourant();
		return type != null && type.isMettableEnAttenteSignature();
	}
}
