package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class DebiteurPrestationImposableIndexable extends TiersIndexable {

	public static final String SUB_TYPE = "debiteurprestationimposable";

	private ContribuableIndexable ctbIndexable;

	public DebiteurPrestationImposableIndexable(AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra,
	                                            DebiteurPrestationImposable dpi) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, dpi);

		final Contribuable ctb = tiersService.getContribuable(dpi);
		if (ctb != null) {
			if (ctb instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) ctb;
				if (pp.isHabitant()) {
					Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), DateHelper.getCurrentYear());
					ctbIndexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, pp, ind);
				}
				else {
					ctbIndexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, pp);
				}
			}
			else if (ctb instanceof Entreprise) {
				ctbIndexable = new EntrepriseIndexable(adresseService, tiersService, serviceInfra, (Entreprise) ctb);
			}
			else if (ctb instanceof AutreCommunaute) {
				ctbIndexable = new AutreCommunauteIndexable(adresseService, tiersService, serviceInfra, (AutreCommunaute) ctb);
			}
			else if (ctb instanceof CollectiviteAdministrative) {
				ctbIndexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, serviceInfra, (CollectiviteAdministrative) ctb);
			}
			else if (ctb instanceof MenageCommun) {
				ctbIndexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivil, serviceInfra, ((MenageCommun) ctb));
			}
			else {
				Assert.fail("Type de contribuable inconnu = " + ctb.getNatureTiers());
			}
		}
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;

		data.setNumeros(IndexerFormatHelper.objectToString(tiers.getNumero()));
		data.setNomRaison(dpi.getNom1());
		data.setCategorieDebiteurIs(IndexerFormatHelper.objectToString(dpi.getCategorieImpotSource()));

		if (ctbIndexable == null) {
			data.addNomRaison(dpi.getComplementNom());
			data.setAutresNom(dpi.getNom2());
			data.setNom1(dpi.getNom1());
			data.setNom2(dpi.getNom2());
		}
		else {
			final TiersIndexableData ctbData = (TiersIndexableData) ctbIndexable.getIndexableData();
			data.addNomRaison(ctbData.getNomRaison());
			data.setAutresNom(ctbData.getAutresNom());
			data.addLocaliteEtPays(ctbData.getLocaliteEtPays());
			data.addNatureJuridique(ctbData.getNatureJuridique());

			if (StringUtils.isBlank(dpi.getNom1())) {
				// [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
				data.setNom1(ctbData.getNom1());
				data.setNom2(ctbData.getNom2());
			}
			else {
				data.setNom1(dpi.getNom1());
				data.setNom2(dpi.getNom2());
			}
		}

		final ForDebiteurPrestationImposable fdpi = dpi.getDernierForDebiteur();
		final boolean isActif = (fdpi != null && fdpi.isValidAt(null));
		data.setTiersActif(IndexerFormatHelper.objectToString(isActif));
	}

	@Override
	protected void fillForsData(TiersIndexableData data) {

		// For principal actif
		String typeAutFfpActif = null;
		String noOfsFfpActif = null;

		final ForDebiteurPrestationImposable principalActif = tiers.getForDebiteurPrestationImposableAt(null);
		if (principalActif != null) {
			typeAutFfpActif = principalActif.getTypeAutoriteFiscale().toString();
			noOfsFfpActif = principalActif.getNumeroOfsAutoriteFiscale().toString();
		}

		// For principal
		String communeDernierFfp = null;
		RegDate dateOuvertureFor = null;
		RegDate dateFermetureFor = null;
		final ForDebiteurPrestationImposable dernierPrincipal = tiers.getDernierForDebiteur();
		if (dernierPrincipal != null) {
			communeDernierFfp =  getForCommuneAsString(dernierPrincipal);
			dateOuvertureFor = dernierPrincipal.getDateDebut();
			dateFermetureFor = dernierPrincipal.getDateFin();
		}

		// Autre fors
		StringBuilder noOfsAutresFors = new StringBuilder();
		final Set<ForFiscal> fors = tiers.getForsFiscaux();
		if (fors != null) {
			for (ForFiscal forF : fors) {
				addValue(noOfsAutresFors, forF.getNumeroOfsAutoriteFiscale().toString());
			}
		}

		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.objectToString(dateOuvertureFor));
		data.setDateFermtureFor(IndexerFormatHelper.objectToString(dateFermetureFor));
	}
}
