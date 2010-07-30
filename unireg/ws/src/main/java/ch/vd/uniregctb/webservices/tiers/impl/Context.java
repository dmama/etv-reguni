package ch.vd.uniregctb.webservices.tiers.impl;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;

/**
 * Encapsule quelques services d'unireg
 */
public class Context {

	public AdresseService adresseService;

	public SituationFamilleService situationService;

	public TiersService tiersService;

	public ServiceInfrastructureService infraService;

	public IbanValidator ibanValidator;

	public ParametreAppService parametreService;

	public NoOfsTranslator noOfsTranslator;
}
