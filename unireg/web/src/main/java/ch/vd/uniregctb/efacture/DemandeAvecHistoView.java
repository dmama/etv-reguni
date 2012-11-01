package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.TypeDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
public class DemandeAvecHistoView {

	private final String idDemande;
	private final RegDate dateDemande;
	private final String descriptionTypeDemande;
	private final List<EtatDemandeView> etats;

	public DemandeAvecHistoView(String idDemande, RegDate dateDemande, TypeDemande typeDemande, List<EtatDemandeView> etats) {
		this.idDemande = idDemande;
		this.dateDemande = dateDemande;
		this.descriptionTypeDemande = typeDemande != null ? typeDemande.getDescription() : StringUtils.EMPTY;

		if (etats == null || etats.isEmpty()) {
			throw new IllegalArgumentException("etats ne peut être ni null ni vide");
		}
		this.etats = new ArrayList<EtatDemandeView>(etats);
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getIdDemande() {
		return idDemande;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<EtatDemandeView> getEtats() {
		return etats;
	}

	@SuppressWarnings("UnusedDeclaration")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getDescriptionTypeDemande() {
		return descriptionTypeDemande;
	}

	public EtatDemandeView getEtatCourant() {
		return etats.get(0);
	}

	@SuppressWarnings("UnusedDeclaration")
	private TypeEtatDemande getTypeEtatCourant() {
		return getEtatCourant().getType();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isValidable() {
		return getEtatCourant().isValidable();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isRefusable() {
		return getEtatCourant().isRefusable();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isMettableEnAttenteContact() {
		return getEtatCourant().isMettableEnAttenteContact();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isMettableEnAttenteSignature() {
		return getEtatCourant().isMettableEnAttenteSignature();
	}
}
