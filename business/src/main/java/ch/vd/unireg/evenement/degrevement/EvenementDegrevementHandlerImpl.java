package ch.vd.unireg.evenement.degrevement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.dperm.xml.common.v1.TypImmeuble;
import ch.vd.dperm.xml.common.v1.TypeImposition;
import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.documentfiscal.DemandeDegrevementICIHelper;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.DonneesLoiLogement;
import ch.vd.unireg.foncier.DonneesUtilisation;
import ch.vd.unireg.interfaces.infra.data.EntiteOFS;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.degrevement.quittance.v1.Commune;
import ch.vd.unireg.xml.degrevement.quittance.v1.QuittanceIntegrationMetierImmDetails;
import ch.vd.unireg.xml.event.degrevement.v1.DonneesMetier;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.event.degrevement.v1.TypDateAttr;
import ch.vd.unireg.xml.event.degrevement.v1.TypEntMax12Attr;
import ch.vd.unireg.xml.event.degrevement.v1.TypPctPosDecMax32Attr;

/**
 * Handler métier des retours des données de dégrèvement
 */
public class EvenementDegrevementHandlerImpl implements EvenementDegrevementHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDegrevementHandlerImpl.class);

	private static final BigInteger BI_MAXINT = BigInteger.valueOf(Integer.MAX_VALUE);
	private static final BigInteger BI_MAXLONG = BigInteger.valueOf(Long.MAX_VALUE);
	private static final BigDecimal BD_HUNDRED = BigDecimal.valueOf(100L);

	private static final int MAX_NATURE_LENGTH = 256;
	private static final Map<Class<? extends ImmeubleRF>, TypImmeuble> TYPES_IMMEUBLE = buildTypesImmeuble();

	private static Map<Class<? extends ImmeubleRF>, TypImmeuble> buildTypesImmeuble() {
		final Map<Class<? extends ImmeubleRF>, TypImmeuble> map = new HashMap<>();
		map.put(ProprieteParEtageRF.class, TypImmeuble.PPE);
		map.put(DroitDistinctEtPermanentRF.class, TypImmeuble.DDP);
		map.put(MineRF.class, TypImmeuble.MINE);
		map.put(BienFondsRF.class, TypImmeuble.B_F);
		map.put(PartCoproprieteRF.class, TypImmeuble.COP);
		return map;
	}

	private TiersService tiersService;
	private RegistreFoncierService registreFoncierService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@Override
	public QuittanceIntegrationMetierImmDetails onRetourDegrevement(Message retour, Map<String, String> headers) throws EsbBusinessException {

		// que faut-il faire ?
		// 1. identifier la demande de dégrèvement correspondante
		// 2. quittancer le formulaire (= quittancement implicite)
		// 3. récupérer les données de dégrèvement et générer une nouvelle entrée en base

		final DemandeDegrevementICI formulaire = findFormulaireDegrevement(retour);

		final Entreprise entreprise = formulaire.getEntreprise();
		final ImmeubleRF immeuble = formulaire.getImmeuble();

		// TODO faut-il faire un contrôle spécifique si aucune valeur déclarée ne change ?

		final DegrevementICI degrevement = new DegrevementICI();
		degrevement.setDateDebut(RegDate.get(formulaire.getPeriodeFiscale(), 1, 1));
		degrevement.setImmeuble(immeuble);
		degrevement.setContribuable(entreprise);

		// Attention, les trois appels extract... plus bas peuvent encore lancer des EsbBusinessException...
		// il ne faut donc pas commencer la manipulation d'entité persistantes avant que les trois
		// appels soient revenus sans heurt

		final DonneesMetier donneesMetier = retour.getDonneesMetier();
		DonneesUtilisation location;
		DonneesUtilisation propreUsage;
		DonneesLoiLogement loiLogement;
		try {
			location = extractDonneesLocation(donneesMetier);
			propreUsage = extractDonneesPropreUsage(donneesMetier);
			loiLogement = extractDonneesLoiLogement(donneesMetier);

			// ok, tout va bien, on dirait
			degrevement.setNonIntegrable(Boolean.FALSE);
		}
		catch (DonneeNonIntegrableException e) {
			LOGGER.error("Donnée non-intégrable présente dans le message entrant", e);
			location = null;
			propreUsage = null;
			loiLogement = new DonneesLoiLogement(Boolean.FALSE, null, null, null);

			// on le marque au fer rouge !
			degrevement.setNonIntegrable(Boolean.TRUE);
		}

		degrevement.setLocation(location);
		degrevement.setPropreUsage(propreUsage);
		degrevement.setLoiLogement(loiLogement);

		// on construit la réponse avant de faire une quelconque modification (cet appel peut également lancer une exception de départ dans TAO-Admin, i.e. avec commit de la transaction)
		final QuittanceIntegrationMetierImmDetails quittance = buildQuittance(formulaire);

		//
		// maintenant, on peut commencer à modifier les entités persistantes
		//

		// quitance implicite du formulaire
		quittancerFormulaire(formulaire, XmlUtils.xmlcal2regdate(retour.getSupervision().getHorodatageReception()));

		// si on a reçu une donnée pour une PF qui est aussi la PF de début d'une donnée existante, alors il faut annuler la donnée précédemment présente
		// mais dans tous les autres cas, on ne considère que la nouvelle donnée qu'entre la PF de la demande et la PF de la donnée existante suivante
		final List<DegrevementICI> existants = entreprise.getAllegementsFonciersNonAnnulesTries(DegrevementICI.class).stream()
				.filter(deg -> deg.getImmeuble() == immeuble)
				.collect(Collectors.toList());

		// annulation des existants (normalement, un seul) dont la PF de début est la même que celle de la demande
		existants.stream()
				.filter(deg -> deg.getDateDebut().year() == formulaire.getPeriodeFiscale())
				.forEach(deg -> deg.setAnnule(true));

		// trouvons maintenant la date de fin de cette donnée = le dernier jour de la PF qui précède la première donnée ultérieure
		existants.stream()
				.filter(AnnulableHelper::nonAnnule)             // et oui, il peut maintenant y avoir des instances annulées...
				.map(DegrevementICI::getDateDebut)
				.filter(debut -> debut.isAfter(degrevement.getDateDebut()))
				.min(Comparator.naturalOrder())
				.map(RegDate::getOneDayBefore)
				.ifPresent(degrevement::setDateFin);

		// il faut éventuellement raccourcir une donnée existante pour la faire se terminer à la veille de la date de début de la nouvelle instance
		final RegDate nouvelleDateFin = degrevement.getDateDebut().getOneDayBefore();
		existants.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(deg -> Pair.of(deg, DateRangeHelper.intersection(degrevement, deg)))
				.filter(pair -> pair.getRight() != null)
				.forEach(pair -> {
					final DegrevementICI aRaccourcir = pair.getLeft();
					if (aRaccourcir.getDateFin() == null) {
						// on ferme juste
						aRaccourcir.setDateFin(nouvelleDateFin);
					}
					else {
						final DegrevementICI copy = aRaccourcir.duplicate();
						aRaccourcir.setAnnule(true);
						copy.setDateFin(nouvelleDateFin);
						entreprise.addAllegementFoncier(copy);
					}
				});

		entreprise.addAllegementFoncier(degrevement);

		return quittance;
	}

	/**
	 * Exception lancée en interne au moment de la détection d'une donnée non-intégrable
	 * (en général des valeurs numériques hors de leur plage de validité)
	 */
	private static class DonneeNonIntegrableException extends Exception {
		public DonneeNonIntegrableException(String message) {
			super(message);
		}
	}

	@Nullable
	private static DonneesUtilisation extractDonneesLocation(DonneesMetier data) throws DonneeNonIntegrableException {
		final Long volume = extractLong("volume locatif", data.getVolumeLocatif());
		final Long surface = extractLong("surface locative", data.getSurfaceLocatif());
		final Long revenu = extractLong("revenu locatif perçu", data.getRevenuLocatifEncaisse());
		final BigDecimal percent = extractPourcentage("pourcentage locatif", data.getPourcentageLocatif());
		if (volume != null || surface != null || revenu != null || percent != null) {
			return new DonneesUtilisation(revenu, volume, surface, percent, null);
		}
		return null;
	}

	@Nullable
	private static DonneesUtilisation extractDonneesPropreUsage(DonneesMetier data) throws DonneeNonIntegrableException {
		final Long volume = extractLong("volume propre usage", data.getVolumePropreUsage());
		final Long surface = extractLong("surface propre usage", data.getSurfacePropreUsage());
		final Long revenu = extractLong("revenu estimé", data.getRevenuLocatifEstime());
		final BigDecimal percent = extractPourcentage("pourcentage propre usage", data.getPourcentagePropreUsage());
		if (volume != null || surface != null || revenu != null || percent != null) {
			return new DonneesUtilisation(revenu, volume, surface, percent, null);
		}
		return null;
	}

	@NotNull
	private static DonneesLoiLogement extractDonneesLoiLogement(DonneesMetier data) throws DonneeNonIntegrableException {
		if (data.isControleOfficeLogement()) {
			final RegDate dateOctroi = extractDate("date d'octroi", data.getDateOctroi());
			final RegDate dateEcheanceOctroi = extractDate("date d'échéance d'octroi", data.getDateEcheanceOctroi());
			return new DonneesLoiLogement(data.isControleOfficeLogement(), dateOctroi, dateEcheanceOctroi, null);
		}
		else {
			return new DonneesLoiLogement(Boolean.FALSE, null, null, null);
		}
	}

	@Nullable
	private static Long extractLong(String description, TypEntMax12Attr value) throws DonneeNonIntegrableException {
		if (value == null) {
			return null;
		}
		if (!value.isValide()) {
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est indiqué comme non-valide dans les données transmises");
		}
		final BigInteger numericalValue = value.getValue();
		if (numericalValue.compareTo(BigInteger.ZERO) < 0 || numericalValue.compareTo(BI_MAXLONG) > 0) {
			// valeur clairement hors domaine de validité...
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est hors de son domaine de validité [0 - " + Long.MAX_VALUE + "] : " + numericalValue);
		}
		return numericalValue.longValue();
	}

	@Nullable
	private static BigDecimal extractPourcentage(String description, TypPctPosDecMax32Attr value) throws DonneeNonIntegrableException {
		if (value == null){
			return null;
		}
		if (!value.isValide()) {
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est indiqué comme non-valide dans les données transmises");
		}
		final BigDecimal percent = value.getValue();
		if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BD_HUNDRED) > 0) {
			// valeur hors du domaine de validité...
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est hors de son domaine de validité [0.00 - 100.00] : " + percent);
		}
		return percent;
	}

	@Nullable
	private static RegDate extractDate(String description, TypDateAttr value) throws DonneeNonIntegrableException {
		if (value == null) {
			return null;
		}
		if (!value.isValide()) {
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est indiqué comme non-valide dans les données transmises.");
		}
		final RegDate date = XmlUtils.xmlcal2regdate(value.getValue());
		if (date == null) {
			throw new DonneeNonIntegrableException("L'attribut '" + description + "' est hors de son domaine de validité " + DateRangeHelper.toDisplayString(DateConstants.DEFAULT_VALIDITY_RANGE) + " : " + value.getValue());
		}
		return date;
	}

	/**
	 * Classe interne qui contient les informations nécessaire pour retrouver un formulaire de demande de dégrèvement ICI
	 */
	private static final class IdFormulaire {
		final int noContribuable;
		final int numeroSequence;
		final int pf;

		public IdFormulaire(int noContribuable, int numeroSequence, int pf) {
			this.noContribuable = noContribuable;
			this.numeroSequence = numeroSequence;
			this.pf = pf;
		}
	}

	/**
	 * Retrouve le formulaire de dégrèvement ICI correspondant aux données qui reviennent
	 * @param retour message entrant
	 * @return le formulaire de demande
	 * @throws EsbBusinessException si le formulaire est introuvable
	 */
	@NotNull
	private DemandeDegrevementICI findFormulaireDegrevement(Message retour) throws EsbBusinessException {

		// récupération des données de récupération de la demande de dégrèvement ICI
		final IdFormulaire id = extractIdFormulaire(retour.getDonneesMetier());

		final Tiers tiers = tiersService.getTiers(id.noContribuable);
		if (!(tiers instanceof Entreprise)) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Entreprise inconnue avec ce numéro de contribuable.", null);
		}

		final Entreprise entreprise = (Entreprise) tiers;
		final Optional<DemandeDegrevementICI> formulaire = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
				.filter(dd -> dd.getPeriodeFiscale() == id.pf)
				.filter(dd -> dd.getNumeroSequence() == id.numeroSequence)
				.findFirst();
		if (formulaire.isPresent()) {
			return formulaire.get();
		}

		throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Formulaire de demande de dégrèvement introuvable pour la PF " + id.pf + " et le numéro de séquence " + id.numeroSequence, null);
	}

	/**
	 * Quittance le formulaire de demande de dégrèvement s'il n'est pas déjà quittancé
	 * @param formulaire formulaire
	 * @param dateQuittance nouvelle date de quittance
	 */
	private void quittancerFormulaire(DemandeDegrevementICI formulaire, RegDate dateQuittance) {
		if (formulaire.getDateRetour() == null) {
			formulaire.setDateRetour(dateQuittance);
		}
	}

	@NotNull
	private IdFormulaire extractIdFormulaire(DonneesMetier data) throws EsbBusinessException {
		if (data == null) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Données métier absentes.", null);
		}

		final BigInteger noSeqBrut = data.getNumeroSequenceDemande();
		if (noSeqBrut == null) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Numéro de séquence du formulaire originel absent.", null);
		}
		else if (noSeqBrut.compareTo(BigInteger.ZERO) <= 0 || noSeqBrut.compareTo(BI_MAXINT) > 0) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Numéro de séquence invalide (" + noSeqBrut + ").", null);
		}

		return new IdFormulaire(data.getNumeroContribuable(), noSeqBrut.intValue(), data.getPeriodeFiscale());
	}

	private QuittanceIntegrationMetierImmDetails buildQuittance(DemandeDegrevementICI formulaire) throws EsbBusinessException {
		final QuittanceIntegrationMetierImmDetails quittance = new QuittanceIntegrationMetierImmDetails();
		quittance.setCommune(extractCommune(formulaire));
		quittance.setEstimationFiscale(extractEstimationFiscale(formulaire));
		quittance.setHorodatage(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()));
		quittance.setNatureImmeuble(extractNatureImmeuble(formulaire));
		quittance.setNumeroContribuable(formulaire.getEntreprise().getNumero().intValue());
		quittance.setNumeroParcelle(DemandeDegrevementICIHelper.getNumeroParcelleComplet(formulaire, registreFoncierService));
		quittance.setPeriodeFiscale(BigInteger.valueOf(formulaire.getPeriodeFiscale()));
		quittance.setTraitementMetier(true);
		quittance.setTypeImmeuble(getTypeImmeuble(formulaire.getImmeuble()));
		quittance.setTypeImpot(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE);
		return quittance;
	}

	@Nullable
	static TypImmeuble getTypeImmeuble(ImmeubleRF immeuble) {
		return TYPES_IMMEUBLE.get(immeuble.getClass());
	}

	@Nullable
	private String extractNatureImmeuble(DemandeDegrevementICI formulaire) {
		return StringUtils.trimToNull(DemandeDegrevementICIHelper.getNatureImmeuble(formulaire, MAX_NATURE_LENGTH));
	}

	private Commune extractCommune(DemandeDegrevementICI formulaire) throws EsbBusinessException {
		final EntiteOFS commune = DemandeDegrevementICIHelper.getCommune(formulaire, registreFoncierService);
		if (commune != null) {
			return new Commune(BigInteger.valueOf(commune.getNoOFS()), commune.getNomOfficiel());
		}
		throw new EsbBusinessException(EsbBusinessCode.REPONSE_IMPOSSIBLE, "Commune de l'immeuble introuvable...", null);
	}

	@Nullable
	private BigDecimal extractEstimationFiscale(DemandeDegrevementICI formulaire) {
		return Optional.of(formulaire)
				.map(f -> DemandeDegrevementICIHelper.getEstimationFiscale(f, registreFoncierService))
				.map(EstimationRF::getMontant)
				.map(BigDecimal::valueOf)
				.orElse(null);
	}
}
