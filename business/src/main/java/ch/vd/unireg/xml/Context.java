package ch.vd.unireg.xml;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.efacture.EFactureService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.BouclementService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.validation.ValidationService;

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

	public ServiceEntreprise serviceEntreprise;

	public HibernateTemplate hibernateTemplate;

	public PlatformTransactionManager transactionManager;
	
	public ListeRecapService lrService;

	public DeclarationImpotService diService;

	public BamMessageSender bamSender;

	public AssujettissementService assujettissementService;

	public PeriodeExploitationService periodeExploitationService;

	public PeriodeImpositionService periodeImpositionService;

	public PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService;

	public SecurityProviderInterface securityProvider;

	public EFactureService eFactureService;

	public ExerciceCommercialHelper exerciceCommercialHelper;

	public EvenementFiscalService evenementFiscalService;

	public RegistreFoncierService registreFoncierService;

	public RegimeFiscalService regimeFiscalService;

	public ValidationService validationService;

	public BouclementService bouclementService;

	public PeriodeFiscaleDAO periodeDAO;
}
