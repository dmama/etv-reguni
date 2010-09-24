package ch.vd.uniregctb.indexer.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
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
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
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
				if (pp.isHabitantVD()) {
					final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null, AttributeIndividu.ADRESSES);
					if (ind == null) {
						throw new IndividuNotFoundException(pp);
					}
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

	/**
	 * Concatène toutes les chaînes de la liste en une seule chaîne, en utilisant le séparateur donné entre chacune d'entre elles
	 */
	private static String concat(List<String> elts, String separator) {
		final StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String elt : elts) {
			if (!first) {
				b.append(separator);
			}
			b.append(elt);
			first = false;
		}
		return b.toString();
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;

		data.setNumeros(IndexerFormatHelper.objectToString(tiers.getNumero()));

		final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
		data.setNomRaison(concat(raisonSociale, " "));
		data.setCategorieDebiteurIs(IndexerFormatHelper.objectToString(dpi.getCategorieImpotSource()));

		if (ctbIndexable == null) {

			// dans ce cas, la raison sociale vue plus haut est nom1 et nom2

			data.addNomRaison(dpi.getComplementNom());
			data.setAutresNom(raisonSociale.size() > 1 ? raisonSociale.get(1) : null);
			data.setNom1(raisonSociale.size() > 0 ? raisonSociale.get(0) : null);
			data.setNom2(raisonSociale.size() > 1 ? raisonSociale.get(1) : null);
		}
		else {
			final TiersIndexableData ctbData = (TiersIndexableData) ctbIndexable.getIndexableData();
			data.addNomRaison(ctbData.getNomRaison());
			data.setAutresNom(ctbData.getAutresNom());
			data.addLocaliteEtPays(ctbData.getLocaliteEtPays());
			data.setNatureJuridique(ctbData.getNatureJuridique());
			data.setNom1(ctbData.getNom1());
			data.setNom2(ctbData.getNom2());
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
