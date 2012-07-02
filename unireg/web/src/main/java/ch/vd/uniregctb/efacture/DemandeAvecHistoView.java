package ch.vd.uniregctb.efacture;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
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
		this.etats = etats;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	@Nullable
	public EtatDemandeView getEtatCourant() {
		return etats != null && etats.size() > 0 ? etats.get(0) : null;
	}

	@Nullable
	private TypeEtatDemande getTypeEtatCourant() {
		final EtatDemandeView etatCourant = getEtatCourant();
		return etatCourant != null ? etatCourant.getType() : null;
	}

	public boolean isValidable() {
		final EtatDemandeView etatCourant = getEtatCourant();
		return etatCourant != null && etatCourant.isValidable();
	}

	public boolean isRefusable() {
		final EtatDemandeView etatCourant = getEtatCourant();
		return etatCourant != null && etatCourant.isRefusable();
	}

	public boolean isMettableEnAttenteContact() {
		final EtatDemandeView etatCourant = getEtatCourant();
		return etatCourant != null && etatCourant.isMettableEnAttenteContact();
	}

	public boolean isMettableEnAttenteSignature() {
		final EtatDemandeView etatCourant = getEtatCourant();
		return etatCourant != null && etatCourant.isMettableEnAttenteSignature();
	}
}
