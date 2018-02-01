package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Entity
@DiscriminatorValue("CTRL_DOSSIER")
public class TacheControleDossier extends Tache {

	// Ce constructeur est requis par Hibernate
	protected TacheControleDossier() {
	}

	public TacheControleDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheControleDossier;
	}
}
