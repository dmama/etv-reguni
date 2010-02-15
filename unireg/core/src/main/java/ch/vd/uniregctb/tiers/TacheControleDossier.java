package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_B2aOsG7EEd2HlNPAVeri9w"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_B2aOsG7EEd2HlNPAVeri9w"
 */
@Entity
@DiscriminatorValue("CTRL_DOSSIER")
public class TacheControleDossier extends Tache {

	private static final long serialVersionUID = -4462641747618584587L;

	// Ce constructeur est requis par Hibernate
	protected TacheControleDossier() {
	}

	public TacheControleDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable) {
		super(etat, dateEcheance, contribuable);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheControleDossier;
	}
}
