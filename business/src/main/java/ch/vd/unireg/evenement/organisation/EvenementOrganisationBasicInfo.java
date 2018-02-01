package ch.vd.unireg.evenement.organisation;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;


/**
 * Informations de base sur un événement organisation.
 * <p/><b>Note: </b> cette classe n'est pas <i>thread-safe</i>.
 */
public final class EvenementOrganisationBasicInfo implements Serializable {

	private static final long serialVersionUID = 8037880188053201128L;

	private final long id;
	private final long noEvenement;
	private final long noOrganisation;
	private final EtatEvenementOrganisation etat;
	private final TypeEvenementOrganisation type;
	private final RegDate date;

	public EvenementOrganisationBasicInfo(EvenementOrganisation evt, long noOrganisation) {
		if (noOrganisation != evt.getNoOrganisation()) {
			// ce serait un gros bug... mais on n'est jamais trop sûr...
			throw new IllegalArgumentException("Numéros d'organisation différents : l'événement avait " + evt.getNoOrganisation() + " mais on veut le traiter avec " + noOrganisation);
		}
		this.id = evt.getId();
		this.noEvenement = evt.getNoEvenement();
		this.noOrganisation = evt.getNoOrganisation();
		this.etat = evt.getEtat();
		this.type = evt.getType();
		this.date = evt.getDateEvenement();
	}

	public EvenementOrganisationBasicInfo(long id, long noEvenement, long noOrganisation, EtatEvenementOrganisation etat,
	                                      TypeEvenementOrganisation type, RegDate date) {
		this.id = id;
		this.noEvenement = noEvenement;
		this.noOrganisation = noOrganisation;
		this.etat = etat;
		this.type = type;
		this.date = date;
	}

	public RegDate getDate() {
		return date;
	}

	public EtatEvenementOrganisation getEtat() {
		return etat;
	}

	public long getId() {
		return id;
	}

	public long getNoEvenement() {
		return noEvenement;
	}

	public long getNoOrganisation() {
		return noOrganisation;
	}

	public TypeEvenementOrganisation getType() {
		return type;
	}
}
