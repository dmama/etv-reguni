package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;

public class Context {
	public final Contribuable contribuable;
	public final CollectiviteAdministrative collectivite;
	public final RegDate dateEcheance;
	public final TacheDAO tacheDAO;
	public final DeclarationImpotService diService;

	public Context(Contribuable contribuable, CollectiviteAdministrative collectivite, RegDate dateEcheance, TacheDAO tacheDAO, DeclarationImpotService diService) {
		this.contribuable = contribuable;
		this.collectivite = collectivite;
		this.dateEcheance = dateEcheance;
		this.tacheDAO = tacheDAO;
		this.diService = diService;
	}
}
