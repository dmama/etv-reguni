package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;

public class Context {

	public final Contribuable contribuable;
	public final CollectiviteAdministrative collectivite;
	public final TacheDAO tacheDAO;
	public final DeclarationImpotService diService;
	public final CollectiviteAdministrative officeSuccessions;
	public final DeclarationImpotOrdinaireDAO diDAO;
	public final PeriodeFiscaleDAO periodeFiscaleDAO;

	public Context(Contribuable contribuable, CollectiviteAdministrative collectivite, TacheDAO tacheDAO, DeclarationImpotService diService, CollectiviteAdministrative officeSuccessions,
	               DeclarationImpotOrdinaireDAO diDAO, PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.contribuable = contribuable;
		this.collectivite = collectivite;
		this.tacheDAO = tacheDAO;
		this.diService = diService;
		this.officeSuccessions = officeSuccessions;
		this.diDAO = diDAO;
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}
}
