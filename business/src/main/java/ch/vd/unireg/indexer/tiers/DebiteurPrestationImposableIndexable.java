package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DebiteurPrestationImposableIndexable extends TiersIndexable<DebiteurPrestationImposable> {

	public static final String SUB_TYPE = "debiteurprestationimposable";

	private final ContribuableIndexable ctbIndexable;

	public DebiteurPrestationImposableIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceCivilService serviceCivil, ServiceOrganisationService serviceOrganisation,
	                                            ServiceInfrastructureService serviceInfra, AvatarService avatarService, DebiteurPrestationImposable dpi) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, dpi);

		final Contribuable ctb = tiersService.getContribuable(dpi);
		if (ctb != null) {
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				if (pp.isHabitantVD()) {
					final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null, AttributeIndividu.ADRESSES);
					if (ind == null) {
						throw new IndividuNotFoundException(pp);
					}
					ctbIndexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp, ind);
				}
				else {
					ctbIndexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
				}
			}
			else if (ctb instanceof Entreprise) {
				ctbIndexable = new EntrepriseIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceOrganisation, avatarService, (Entreprise) ctb);
			}
			else if (ctb instanceof Etablissement) {
				ctbIndexable = new EtablissementIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceOrganisation, avatarService, (Etablissement) ctb);
			}
			else if (ctb instanceof AutreCommunaute) {
				ctbIndexable = new AutreCommunauteIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, (AutreCommunaute) ctb);
			}
			else if (ctb instanceof CollectiviteAdministrative) {
				ctbIndexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, (CollectiviteAdministrative) ctb);
			}
			else if (ctb instanceof MenageCommun) {
				ctbIndexable = new MenageCommunIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, ((MenageCommun) ctb));
			}
			else {
				throw new IllegalArgumentException("Type de contribuable inconnu = " + ctb.getNatureTiers());
			}
		}
		else {
			ctbIndexable = null;
		}
	}

	@Override
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

		final List<String> raisonSociale = tiersService.getRaisonSociale(tiers);
		data.setNomRaison(concat(raisonSociale, " "));
		data.setCategorieDebiteurIs(IndexerFormatHelper.enumToString(tiers.getCategorieImpotSource()));
		data.setModeCommunication(tiers.getModeCommunication());
		data.addNomRaison(tiers.getComplementNom());

		if (ctbIndexable == null) {

			// dans ce cas, la raison sociale vue plus haut est nom1 et nom2

			data.setAutresNom(raisonSociale.size() > 1 ? raisonSociale.get(1) : null);
			data.setNom1(!raisonSociale.isEmpty() ? raisonSociale.get(0) : null);
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

		final ForDebiteurPrestationImposable fdpi = tiers.getDernierForDebiteur();
		final boolean isActif = (fdpi != null && fdpi.isValidAt(null));
		data.setTiersActif(isActif);
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

		// Dernier for principal
		String communeDernierFfp = null;
		RegDate dateOuvertureFor = null;
		RegDate dateFermetureFor = null;
		MotifFor motifFermetureDernierFor = null;
		final ForDebiteurPrestationImposable dernierFor = tiers.getDernierForDebiteur();
		if (dernierFor != null) {
			communeDernierFfp =  getLocalisationAsString(dernierFor, tiers);
			dateOuvertureFor = dernierFor.getDateDebut();
			dateFermetureFor = dernierFor.getDateFin();
			motifFermetureDernierFor = dernierFor.getMotifFermeture();
		}

		// Autre fors
		StringBuilder noOfsAutresFors = new StringBuilder();
		final Set<ForFiscal> fors = tiers.getForsFiscaux();
		if (fors != null) {
			for (ForFiscal forF : fors) {
				addValue(noOfsAutresFors, forF.getNumeroOfsAutoriteFiscale().toString());
			}
		}

		// Fors vaudois
		RegDate dateOuvertureForVd = null;
		RegDate dateFermetureForVd = null;

		// [SIFISC-19217] ce qui nous intéresse pour les DPI, c'est la dernière présence vaudoise continue
		final List<ForFiscal> tousForsNonAnnules = tiers.getForsFiscauxNonAnnules(true);
		final List<ForFiscal> forsVaudois = new ArrayList<>(tousForsNonAnnules.size());
		for (ForFiscal ff : tousForsNonAnnules) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				forsVaudois.add(ff);
			}
		}
		final List<DateRange> rangesVaudois = DateRangeHelper.merge(forsVaudois);
		if (rangesVaudois != null && !rangesVaudois.isEmpty()) {
			final DateRange lastRange = CollectionsUtils.getLastElement(rangesVaudois);
			dateOuvertureForVd = lastRange.getDateDebut();
			dateFermetureForVd = lastRange.getDateFin();
		}

		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.dateToString(dateOuvertureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateFermetureFor(IndexerFormatHelper.dateToString(dateFermetureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateOuvertureForVd(IndexerFormatHelper.dateToString(dateOuvertureForVd, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateFermetureForVd(IndexerFormatHelper.dateToString(dateFermetureForVd, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setMotifFermetureDernierForPrincipal(motifFermetureDernierFor);
	}
}
