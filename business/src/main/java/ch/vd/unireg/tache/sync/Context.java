package ch.vd.unireg.tache.sync;

import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNCDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TiersService;

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
