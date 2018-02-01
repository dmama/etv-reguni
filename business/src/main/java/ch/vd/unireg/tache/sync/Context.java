package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.QuestionnaireSNCDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class Context {

	public final Contribuable contribuable;
	public final CollectiviteAdministrative collectivite;
	public final TacheDAO tacheDAO;
	public final TiersService tiersService;
	public final DeclarationImpotService diService;
	public final CollectiviteAdministrative caApresDeces;
	public final DeclarationImpotOrdinaireDAO diDAO;
	public final QuestionnaireSNCDAO qsncDAO;
	public final PeriodeFiscaleDAO periodeFiscaleDAO;
	public final ParametreAppService parametreAppService;

	public Context(Contribuable contribuable, CollectiviteAdministrative collectivite, TacheDAO tacheDAO, DeclarationImpotService diService, CollectiviteAdministrative caApresDeces,
	               TiersService tiersService, DeclarationImpotOrdinaireDAO diDAO, QuestionnaireSNCDAO qsncDAO, PeriodeFiscaleDAO periodeFiscaleDAO, ParametreAppService parametreAppService) {
		this.contribuable = contribuable;
		this.collectivite = collectivite;
		this.tacheDAO = tacheDAO;
		this.diService = diService;
		this.caApresDeces = caApresDeces;
		this.tiersService = tiersService;
		this.diDAO = diDAO;
		this.qsncDAO = qsncDAO;
		this.periodeFiscaleDAO = periodeFiscaleDAO;
		this.parametreAppService = parametreAppService;
	}
}
