package ch.vd.uniregctb.validation.tiers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Validateur qui se préoccupe de la partie Contribuable d'un tiers contribuable
 */
public abstract class ContribuableValidator<T extends Contribuable> extends TiersValidator<T> {

	private PeriodeImpositionService periodeImpositionService;
	private ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public ValidationResults validate(T ctb) {
		final ValidationResults vr = super.validate(ctb);
		if (!ctb.isAnnule()) {
			vr.merge(validateDecisions(ctb));
			vr.merge(validateAdressesMandataires(ctb));
			vr.merge(validateChevauchementsMandats(ctb));
		}
		return vr;
	}

	@NotNull
	protected static <T> List<T> neverNull(List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults vr = super.validateFors(ctb);

		// les plages de validité des fors principaux ne doivent pas se chevaucher
		final List<? extends ForFiscalPrincipal> forsPrincipaux = neverNull(ctb.getForsFiscauxPrincipauxActifsSorted());
		final MovingWindow<ForFiscalPrincipal> movingWindow = new MovingWindow<>(forsPrincipaux);
		while (movingWindow.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snapshot = movingWindow.next();
			final ForFiscalPrincipal current = snapshot.getCurrent();
			final ForFiscalPrincipal next = snapshot.getNext();
			if (current != null && next != null && DateRangeHelper.intersect(current, next)) {
				vr.addError(String.format("Le for principal qui commence le %s chevauche le for précédent", RegDateHelper.dateToDisplayString(next.getDateDebut())));
			}
		}

		final ForsParType fors = ctb.getForsParType(true);

		// Pour chaque for secondaire il doit exister un for principal valide
		for (ForFiscalSecondaire fs : fors.secondaires) {
			if (!Contribuable.existForPrincipal(forsPrincipaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				vr.addError(msg);
			}
		}

		// Pour chaque for autre élément imposable il doit exister un for principal valide
		for (ForFiscalAutreElementImposable fs : fors.autreElementImpot) {
			if (!Contribuable.existForPrincipal(forsPrincipaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for autre élément imposable qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				vr.addError(msg);
			}
		}

		// Les for DPI ne sont pas autorisés
		for (ForDebiteurPrestationImposable fpdi : fors.dpis) {
			vr.addError("Le for " + fpdi + " n'est pas un type de for autorisé sur un contribuable.");
		}

		return vr;

	}

	private ValidationResults validateDecisions(Contribuable ctb) {

		final ValidationResults results = new ValidationResults();
		if (ctb.getDecisionsAci() == null) {
			return results;
		}

		final Set<DecisionAci> decisionAcis = ctb.getDecisionsAci();
		// On valide toutes les décisions
		final ValidationService validationService = getValidationService();
		for (DecisionAci d : decisionAcis) {
			results.merge(validationService.validate(d));
		}

		return results;
	}

	private ValidationResults validateAdressesMandataires(Contribuable ctb) {

		final ValidationResults results = new ValidationResults();
		final Set<AdresseMandataire> adressesMandataires = ctb.getAdressesMandataires();
		if (adressesMandataires == null || adressesMandataires.isEmpty()) {
			return results;
		}

		final ValidationService service = getValidationService();
		for (AdresseMandataire am : adressesMandataires) {
			results.merge(service.validate(am));
		}
		return results;
	}

	@Override
	protected ValidationResults validateDeclarations(T ctb) {
		final ValidationResults results = super.validateDeclarations(ctb);

		final List<DeclarationImpotOrdinaire> decls = ctb.getDeclarationsTriees(DeclarationImpotOrdinaire.class, false);
		if (decls != null && !decls.isEmpty()) {

			// [SIFISC-3127] on valide les déclarations d'impôts ordinaires par rapport aux périodes d'imposition théoriques
			try {
				final List<PeriodeImposition> periodes = periodeImpositionService.determine(ctb);
				for (DeclarationImpotOrdinaire di : decls) {
					if (isPeriodeImpositionExpected(di)) {
						validateDeclarationVsPeriodeImposition(di, periodes, results);
					}
				}
			}
			catch (Exception e) {
				results.addWarning("Impossible de calculer les périodes d'imposition: ", e);
			}
		}

		return results;
	}

	/**
	 * @param di une déclaration d'impôt ordinaire
	 * @return <code>true</code> s'il devrait y avoir une période d'imposition correspondant à la déclaration, <code>false</code> sinon (parce que la DI est trop vieille, par exemple...)
	 */
	protected boolean isPeriodeImpositionExpected(DeclarationImpotOrdinaire di) {
		return true;
	}

	/**
	 * Blindage contre les déclarations qui n'ont pas de modèle de document associé (elles ne passent normalement pas
	 * la validation, mais si la données est ainsi en base, c'est dommage d'exploser avec une NPE
	 * @param di la déclaration
	 * @return une description de son type (en général fonction du modèle de document associé...)
	 */
	private static String getDescriptionDI(DeclarationImpotOrdinaire di) {
		if (di.getModeleDocument() != null) {
			final TypeDocument typeDocument = di.getModeleDocument().getTypeDocument();
			return typeDocument.getDescription();
		}
		else {
			return "déclaration d'impôt";
		}
	}

	private static void validateDeclarationVsPeriodeImposition(DeclarationImpotOrdinaire di, List<PeriodeImposition> periodes, ValidationResults results) {
		boolean intersect = false;
		final String descriptionDI = getDescriptionDI(di);
		if (periodes != null) {
			for (PeriodeImposition p : periodes) {
				if (DateRangeHelper.equals(di, p)) {
					intersect = true;
					break;
				}
				else if (DateRangeHelper.intersect(di, p)) {
					intersect = true;
					final String message = String.format("La %s qui va du %s au %s ne correspond pas à la période d'imposition théorique qui va du %s au %s",
					                                     descriptionDI, RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()),
					                                     RegDateHelper.dateToDisplayString(p.getDateDebut()), RegDateHelper.dateToDisplayString(p.getDateFin()));
					results.addWarning(message);
				}
			}
		}
		if (!intersect) {
			final String message = String.format("La %s qui va du %s au %s ne correspond à aucune période d'imposition théorique",
			                                     descriptionDI, RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()));
			results.addWarning(message);
		}
	}

	/**
	 * Clé extractible depuis un mandat-lien ou une adresse mandataire pour déterminer les différentes typologies
	 * de mandat dans chacune desquelles il ne doit pas y avoir de chevauchement
	 */
	private static final class MandatTypeKey {

		private final TypeMandat typeMandat;
		private final String codeGenreImpot;

		public MandatTypeKey(Mandat mandat) {
			this.typeMandat = mandat.getTypeMandat();
			this.codeGenreImpot = this.typeMandat == TypeMandat.SPECIAL ? mandat.getCodeGenreImpot() : null;
		}

		public MandatTypeKey(AdresseMandataire adresse) {
			this.typeMandat = adresse.getTypeMandat();
			this.codeGenreImpot = this.typeMandat == TypeMandat.SPECIAL ? adresse.getCodeGenreImpot() : null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final MandatTypeKey mandatKey = (MandatTypeKey) o;
			return typeMandat == mandatKey.typeMandat && Objects.equals(codeGenreImpot, mandatKey.codeGenreImpot);
		}

		@Override
		public int hashCode() {
			int result = typeMandat != null ? typeMandat.hashCode() : 0;
			result = 31 * result + (codeGenreImpot != null ? codeGenreImpot.hashCode() : 0);
			return result;
		}
	}

	private String toDisplayString(MandatTypeKey key) {
		if (key.codeGenreImpot == null) {
			return key.typeMandat.name();
		}
		else {
			final List<GenreImpotMandataire> gims = infraService.getGenresImpotMandataires();
			String libelle = key.codeGenreImpot;
			for (GenreImpotMandataire gim : gims) {
				if (Objects.equals(key.codeGenreImpot, gim.getCode())) {
					libelle = gim.getLibelle();
					break;
				}
			}
			return String.format("%s (%s)", key.typeMandat.name(), libelle);
		}
	}

	/**
	 * Validation que, par type de mandat, il n'y a pas de chevauchement entre les différents mandats non-annulés, quelle qu'en soit la forme
	 * (mandat-lien ou adresse de mandataire)
	 * @param mandant mandant, point de départ
	 * @return des éventuelles erreurs de validation concernant les chevauchements trouvés
	 */
	private ValidationResults validateChevauchementsMandats(Contribuable mandant) {

		// différents univers de non-chevauchement
		final Map<MandatTypeKey, List<DateRange>> universes = new HashMap<>();

		// on parcourt d'abord les mandats-liens
		final Set<RapportEntreTiers> rets = mandant.getRapportsSujet();
		if (rets != null && !rets.isEmpty()) {
			for (RapportEntreTiers ret : rets) {
				if (!ret.isAnnule() && ret instanceof Mandat) {
					final Mandat mandat = (Mandat) ret;
					final MandatTypeKey key = new MandatTypeKey(mandat);
					consolidateInMap(universes, key, mandat);
				}
			}
		}

		// ... puis les adresses mandataire
		final Set<AdresseMandataire> adresses = mandant.getAdressesMandataires();
		if (adresses != null && !adresses.isEmpty()) {
			for (AdresseMandataire adresse : adresses) {
				if (!adresse.isAnnule()) {
					final MandatTypeKey key = new MandatTypeKey(adresse);
					consolidateInMap(universes, key, adresse);
				}
			}
		}

		// préparons l'objet à fournir en sortie
		final ValidationResults vr = new ValidationResults();

		// ensuite, on prend chaque univers un par un
		final DateRangeComparator<DateRange> rangeComparator = new DateRangeComparator<>();
		for (Map.Entry<MandatTypeKey, List<DateRange>> entry : universes.entrySet()) {
			final List<DateRange> ranges = entry.getValue();
			if (ranges.size() > 1) {
				Collections.sort(ranges, rangeComparator);
				final List<DateRange> overlaps = DateRangeHelper.overlaps(ranges);
				if (overlaps != null && !overlaps.isEmpty()) {
					for (DateRange overlap : overlaps) {
						vr.addError(String.format("La période %s est couverte par plusieurs mandats de type %s",
						                          DateRangeHelper.toDisplayString(overlap),
						                          toDisplayString(entry.getKey())));
					}
				}
			}
		}

		return vr;
	}

	private static <K, V> void consolidateInMap(Map<K, List<V>> map, K key, V value) {
		final List<V> list = map.computeIfAbsent(key, k -> new LinkedList<>());
		list.add(value);
	}
}
