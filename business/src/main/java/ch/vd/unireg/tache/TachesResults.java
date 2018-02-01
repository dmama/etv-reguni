package ch.vd.unireg.tache;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.TiersService;

public abstract class TachesResults<E, R extends JobResults<E, R>> extends JobResults<E, R> {

	// Données de processing
	public int nbOIDTotal;

	public final RegDate dateTraitement;

	public boolean interrompu;

	public TachesResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

}
