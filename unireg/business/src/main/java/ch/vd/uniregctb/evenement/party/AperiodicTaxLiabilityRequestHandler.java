package ch.vd.uniregctb.evenement.party;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityResponse;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.ResponseType;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.Scope;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.TaxResidenceBuilder;

public class AperiodicTaxLiabilityRequestHandler implements RequestHandler<AperiodicTaxLiabilityRequest> {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private SecurityProviderInterface securityProvider;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult handle(AperiodicTaxLiabilityRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		final int number = request.getPartyNumber();
		final RegDate date = DataHelper.xmlToCore(request.getDate());
		final Scope scope = request.getScope();

		final Tiers tiers = tiersDAO.get(number, true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + number + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}
		if (!(tiers instanceof PersonnePhysique)) {
			return new RequestHandlerResult(new AperiodicTaxLiabilityResponse(number, null, ResponseType.TAXPAYER_WITHOUT_TAX_LIABILITY, null));
		}

		final List<State> states = new ArrayList<>();

		// 1) La personne physique identifiée par le "numéro de tiers" doit avoir son for principal dans le canton de Vaud ou être imposée à la source à la "date déterminante".
		final PersonnePhysique pp = (PersonnePhysique) tiers;
		final State ppState = new State(tiers, date, scope);
		states.add(ppState);
		if (ppState.isAssujettiPCAP()) {
			return new RequestHandlerResult(new AperiodicTaxLiabilityResponse(number, TaxResidenceBuilder.newMainTaxResidence(ppState.ffp, false), ResponseType.OK_TAXPAYER_FOUND, null));
		}

		// 2) Si la recherche 1) est infructueuse, il faut rechercher une éventuelle appartenance à un contribuable "ménage commun" à la "date déterminante" et,
		// si ce contribuable existe, il faut qu'il ait son for principal dans le canton de Vaud ou qu'il soit imposé à la source à la "date déterminante".
		final MenageCommun menage = getMenage(pp, date);
		if (menage != null) {
			final State menageState = new State(menage, date, scope);
			states.add(menageState);
			if (menageState.isAssujettiPCAP()) {
				return new RequestHandlerResult(
						new AperiodicTaxLiabilityResponse(menageState.numero, TaxResidenceBuilder.newMainTaxResidence(menageState.ffp, false), ResponseType.OK_TAXPAYER_FOUND, null));
			}
		}

		// 3) Si la recherche 2) est infructueuse et si la personne physique identifiée est mineure à la "date déterminante", il faut vérifier une éventuelle filiation à
		// deux autres personnes physiques "parentes" à la "date déterminante". Si la filiation existe, il faut que les deux personnes physiques "parentes" forment
		// un contribuable "ménage commun" correspondant au critère de la recherche 2)
		if (tiersService.isMineur(pp, date)) {
			final MenageCommun menageParents = getMenageDesParents(pp, date);
			if (menageParents != null) {
				final State menageParentsState = new State(menageParents, date, scope);
				states.add(menageParentsState);
				if (menageParentsState.isAssujettiPCAP()) {
					return new RequestHandlerResult(
							new AperiodicTaxLiabilityResponse(menageParentsState.numero, TaxResidenceBuilder.newMainTaxResidence(menageParentsState.ffp, false), ResponseType.OK_TAXPAYER_FOUND, null));
				}
			}
		}

		// b) Dans le cas où, à la fin de la recherche 3), aucun contribuable n'a été trouvé, mais que la recherche 1), 2) ou 3) a trouvé un contribuable qui répondait aux critères
		// de la recherche avant la "date déterminante" : la détermination est négative et le service retourne la date de l'événement le plus récent, le type d'événement et le numéro
		// de contribuable correspondant à cet événement.
		final List<State> anciensAssujettissements = new ArrayList<>();
		for (State state : states) {
			if (state.wasAssujettiPCAP()) {
				anciensAssujettissements.add(state);
			}
		}
		if (!anciensAssujettissements.isEmpty()) {
			// détermine l'assujettissement le plus récent
			Collections.sort(anciensAssujettissements, new Comparator<State>() {
				@Override
				public int compare(State o1, State o2) {
					return o2.getPreviousAssujettissementPCAP().getDateFin().compareTo(o1.getPreviousAssujettissementPCAP().getDateFin());
				}
			});
			final State recent = anciensAssujettissements.get(0);
			return new RequestHandlerResult(
					new AperiodicTaxLiabilityResponse(recent.numero, TaxResidenceBuilder.newMainTaxResidence(recent.getPreviousAssujettissementPCAP(), false), ResponseType.FORMER_TAXPAYER, null));
		}

		// c) Dans le cas où, à la fin de la recherche 3), aucun contribuable n'a été trouvé, mais que la recherche 1), 2) ou 3) a trouvé un contribuable qui n'avait pas de for principal,
		// mais un for secondaire vaudois à la "date déterminante" : la détermination est négative et le service retourne le numéro de contribuable, le for principal, et la nature du
		// for principal (hors Suisse, hors Canton).
		for (State state : states) {
			if (state.hasForSecondaire()) {
				return new RequestHandlerResult(
						new AperiodicTaxLiabilityResponse(state.numero, TaxResidenceBuilder.newMainTaxResidence(state.ffp, false), ResponseType.OUT_OF_SCOPE_TAXPAYER, null));
			}
		}

		// d) Dans le cas où, à la fin de la recherche 3), la PP ne correspond à aucun contribuable assujetti :
		// la détermination est négative et le service retourne les données d'absence d'assujettissement.
		return new RequestHandlerResult(new AperiodicTaxLiabilityResponse(number, null, ResponseType.TAXPAYER_WITHOUT_TAX_LIABILITY, null));
	}

	private static class State {
		public final int numero;
		public final RegDate date;
		public final Scope scope;
		public final ForFiscalPrincipal ffp;
		public final List<ForFiscalPrincipal> ffpPrecedents;
		public final List<ForFiscalSecondaire> ffs;

		private boolean isAssujettiPCAP;
		private ForFiscalPrincipal previousAssujettissementPCAP;

		private State(@NotNull Tiers tiers, RegDate date, Scope scope) {
			this.date = date;
			this.scope = scope;
			this.numero = tiers.getNumero().intValue();

			final List<ForFiscal> ffps = tiers.getForsFiscauxSorted();
			if (ffps == null) {
				this.ffp = null;
				this.ffpPrecedents = null;
			}
			else {
				ForFiscalPrincipal current = null;
				final List<ForFiscalPrincipal> previous = new ArrayList<>();
				for (ForFiscal f : ffps) {
					if (f.isPrincipal() && !f.isAnnule()) {
						if (f.isValidAt(date)) {
							current = (ForFiscalPrincipal) f;
							break;
						}
						previous.add((ForFiscalPrincipal) f);
					}
				}
				this.ffp = current;
				this.ffpPrecedents = previous.isEmpty() ? null : previous;
			}

			this.ffs = new ArrayList<>();
			for (ForFiscal f : tiers.getForsFiscauxValidAt(date)) {
				if (f instanceof ForFiscalSecondaire) {
					ffs.add((ForFiscalSecondaire) f);
				}
			}

			this.isAssujettiPCAP = isAssujettiPCAP(ffp, scope);
			this.previousAssujettissementPCAP = findPreviousAssujettissementPCAP(ffpPrecedents, scope);
		}

		public boolean isAssujettiPCAP() {
			return isAssujettiPCAP;
		}

		public boolean wasAssujettiPCAP() {
			return previousAssujettissementPCAP != null;
		}

		public ForFiscalPrincipal getPreviousAssujettissementPCAP() {
			return previousAssujettissementPCAP;
		}

		public boolean hasForSecondaire() {
			return ffs != null && !ffs.isEmpty();
		}

		@Nullable
		private static ForFiscalPrincipal findPreviousAssujettissementPCAP(@Nullable List<ForFiscalPrincipal> ffpPrecedents, Scope scope) {
			ForFiscalPrincipal previous = null;
			if (ffpPrecedents != null) {
				for (ForFiscalPrincipal f : ffpPrecedents) {
					if (isAssujettiPCAP(f, scope)) {
						previous = f;
					}
				}
			}
			return previous;
		}

		private static boolean isAssujettiPCAP(ForFiscalPrincipal ffp, Scope scope) {
			if (ffp == null) {
				return false;
			}

			switch (scope) {
			case VD_RESIDENT_AND_WITHHOLDING:
				return (estVaudoisNonDiplomateSuisse(ffp) || ffp.getModeImposition() == ModeImposition.SOURCE);
			case ALL_EXCEPT_OTHER_CANTON:
				return (!estHorsCanton(ffp) || ffp.getModeImposition() == ModeImposition.SOURCE);
			default:
				throw new IllegalArgumentException("Le scope [" + scope + "] est inconnu");
			}
		}
	}

	/**
	 * Détermine et retourne le ménage commun des parents d'une personne physique; ou <b>null</b> si la personne physique ne possède pas de parents ou s'ils ne sont pas en ménage.
	 *
	 * @param pp   une personne physique
	 * @param date une date de référence
	 * @return le ménage commun des parents de la personne physique; ou <b>null</b> si la personne physique ne possède pas de parents ou s'ils ne sont pas en ménage.
	 */
	@Nullable
	private MenageCommun getMenageDesParents(@NotNull PersonnePhysique pp, RegDate date) {
		MenageCommun menageParents = null;
		final List<PersonnePhysique> parents = tiersService.getParents(pp, date);
		if (parents.size() == 2) { // TODO (msi) vérifier ça avec Philippe Campiche
			final MenageCommun menage0 = getMenage(parents.get(0), date);
			final MenageCommun menage1 = getMenage(parents.get(1), date);
			if (menage0 != null && menage0 == menage1) {
				menageParents = menage0;
			}
		}
		return menageParents;
	}

	@Nullable
	private MenageCommun getMenage(@Nullable PersonnePhysique pp, RegDate date) {
		if (pp == null) {
			return null;
		}
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(pp, date);
		return ensemble == null ? null : ensemble.getMenage();
	}

	private static boolean estVaudoisNonDiplomateSuisse(ForFiscalPrincipal ffp) {
		return ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE;
	}

	private static boolean estHorsCanton(ForFiscalPrincipal ffp) {
		return ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/aperiodic-taxliab-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/aperiodic-taxliab-response-1.xsd"));
	}
}
