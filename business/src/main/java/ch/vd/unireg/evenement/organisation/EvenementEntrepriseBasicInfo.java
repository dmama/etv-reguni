package ch.vd.unireg.evenement.organisation;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;


/**
 * Informations de base sur un événement entreprise.
 * <p/><b>Note: </b> cette classe n'est pas <i>thread-safe</i>.
 */
public final class EvenementEntrepriseBasicInfo implements Serializable {

	private static final long serialVersionUID = 8037880188053201128L;

	private final long id;
	private final long noEvenement;
	private final long noEntrepriseCivile;
	private final EtatEvenementEntreprise etat;
	private final TypeEvenementEntreprise type;
	private final RegDate date;

	public EvenementEntrepriseBasicInfo(EvenementEntreprise evt, long noEntrepriseCivile) {
		if (noEntrepriseCivile != evt.getNoEntrepriseCivile()) {
			// ce serait un gros bug... mais on n'est jamais trop sûr...
			throw new IllegalArgumentException("Numéros d'entreprise différents : l'événement avait " + evt.getNoEntrepriseCivile() + " mais on veut le traiter avec " + noEntrepriseCivile);
		}
		this.id = evt.getId();
		this.noEvenement = evt.getNoEvenement();
		this.noEntrepriseCivile = evt.getNoEntrepriseCivile();
		this.etat = evt.getEtat();
		this.type = evt.getType();
		this.date = evt.getDateEvenement();
	}

	public EvenementEntrepriseBasicInfo(long id, long noEvenement, long noEntrepriseCivile, EtatEvenementEntreprise etat,
	                                    TypeEvenementEntreprise type, RegDate date) {
		this.id = id;
		this.noEvenement = noEvenement;
		this.noEntrepriseCivile = noEntrepriseCivile;
		this.etat = etat;
		this.type = type;
		this.date = date;
	}

	public RegDate getDate() {
		return date;
	}

	public EtatEvenementEntreprise getEtat() {
		return etat;
	}

	public long getId() {
		return id;
	}

	public long getNoEvenement() {
		return noEvenement;
	}

	public long getNoEntrepriseCivile() {
		return noEntrepriseCivile;
	}

	public TypeEvenementEntreprise getType() {
		return type;
	}
}
