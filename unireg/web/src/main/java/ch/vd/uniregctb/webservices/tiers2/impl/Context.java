package ch.vd.uniregctb.webservices.tiers2.impl;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

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

	public HibernateTemplate hibernateTemplate;

	public PlatformTransactionManager transactionManager;
	
	public NoOfsTranslator noOfsTranslator;

	public ListeRecapService lrService;

	public DeclarationImpotService diService;
}
