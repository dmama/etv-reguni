package ch.vd.uniregctb.tache;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.TiersService;

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
