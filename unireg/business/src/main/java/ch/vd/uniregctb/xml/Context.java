package ch.vd.uniregctb.xml;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Encapsule quelques services d'unireg
 */
public class Context {

	public AdresseService adresseService;

	public SituationFamilleService situationService;

	public TiersDAO tiersDAO;

	public TiersService tiersService;

	public ServiceInfrastructureService infraService;

	public IbanValidator ibanValidator;

	public ParametreAppService parametreService;

	public ServiceCivilService serviceCivilService;

	public ServiceOrganisationService serviceOrganisationService;

	public HibernateTemplate hibernateTemplate;

	public PlatformTransactionManager transactionManager;
	
	public ListeRecapService lrService;

	public DeclarationImpotService diService;

	public BamMessageSender bamSender;

	public AssujettissementService assujettissementService;

	public PeriodeImpositionService periodeImpositionService;

	public PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService;

	public SecurityProviderInterface securityProvider;

	public EFactureService eFactureService;

	public ExerciceCommercialHelper exerciceCommercialHelper;

	public EvenementFiscalService evenementFiscalService;

	public RegistreFoncierService registreFoncierService;
}
