package ch.vd.uniregctb.listes.afc;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ExtractionDonneesRptFortuneResults extends ExtractionDonneesRptPeriodeImpositionResults {

	private final RegDate finAnnee;

	private static final String ASSUJETTI_SANS_FOR_VD_31_12 = "Assujetti sans for vaudois au 31 décembre";
	private static final String NON_ASSUJETTI_31_12 = "Non-assujetti au rôle ordinaire au 31 décembre";

	public ExtractionDonneesRptFortuneResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                          AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService, assujettissementService, periodeImpositionService, adresseService);
		this.finAnnee = RegDate.get(periodeFiscale, 12, 31);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.FORTUNE;
	}

	@Override
	protected String filterPeriodes(Contribuable ctb, List<PeriodeImposition> listeAFiltrer) {
		// pour la fortune, on ne s'intéresse qu'aux contribuables au rôle ordinaire assujettis en fin d'année fiscale

		// on enlève donc toutes les périodes qui ne couvrent pas la fin de l'année
		listeAFiltrer.removeIf(p -> !p.isValidAt(finAnnee));

		if (listeAFiltrer.isEmpty()) {
			return NON_ASSUJETTI_31_12;
		}

		// [UNIREG-3248] malgré l'assujettissement, si le contribuable n'a aucun for vaudois au 31 décembre, il
		// faut l'ignorer dans le cadre de l'extraction "fortune"
		final List<ForFiscal> fors = ctb.getForsFiscauxValidAt(finAnnee);
		boolean trouveVaudois = false;
		if (!fors.isEmpty()) {
			for (ForFiscal ff : fors) {
				if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					trouveVaudois = true;
					break;
				}
			}
		}
		if (!trouveVaudois) {
			listeAFiltrer.clear();
			return ASSUJETTI_SANS_FOR_VD_31_12;
		}

		return null;
	}

	@Override
	protected InfoPeriodeImposition buildInfoPeriodeImpositionFromPeriodeImposition(Contribuable ctb, InfoIdentificationCtb identification, ModeImposition modeImposition, MotifRattachement motifRattachement,
	                                                                                PeriodeImposition periode, MotifAssujettissement motifDebut, MotifAssujettissement motifFin,
	                                                                                Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		final boolean limite = (autoriteFiscaleForPrincipal == TypeAutoriteFiscale.PAYS_HS || autoriteFiscaleForPrincipal == TypeAutoriteFiscale.COMMUNE_HC);
		return new InfoPeriodeImpositionFortune(ctb.getNumero(), identification, modeImposition, periode.getDateDebut(), periode.getDateFin(),
		                                        motifRattachement, motifDebut, motifFin, ofsCommuneForGestion, autoriteFiscaleForPrincipal, limite);
	}

	/**
	 * Le cas "fortune" possède une colonne de plus pour indiquer s'il s'agit d'un assujettissement limité ou illimité
	 */
	public static class InfoPeriodeImpositionFortune extends InfoPeriodeImposition {

		public final boolean limite;

		private static final String LIMITE_DISPLAY = "L";
		private static final String ILLIMITE_DISPLAY = "I";

		private static final String[] NOMS_COLONNES_FORTUNE;

		static {
			NOMS_COLONNES_FORTUNE = new String[NOMS_COLONNES.length + 1];
			System.arraycopy(NOMS_COLONNES, 0, NOMS_COLONNES_FORTUNE, 0, NOMS_COLONNES.length);
			NOMS_COLONNES_FORTUNE[NOMS_COLONNES.length] = "LIMITE/ILLIMITE";
		}

		public InfoPeriodeImpositionFortune(long noCtb, InfoIdentificationCtb identification, ModeImposition modeImposition,
		                                     RegDate debutPeriodeImposition, RegDate finPeriodeImposition, MotifRattachement motifRattachement, MotifAssujettissement motifOuverture, MotifAssujettissement motifFermeture,
		                                     Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal, boolean limite) {
			super(noCtb, identification, modeImposition, debutPeriodeImposition, finPeriodeImposition, motifRattachement, motifOuverture,
			      motifFermeture, ofsCommuneForGestion, autoriteFiscaleForPrincipal);
			this.limite = limite;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES_FORTUNE;
		}

		@Override
		public Object[] getValeursColonnes() {
			final Object[] valeursBase = super.getValeursColonnes();
			final Object[] valeurs = new Object[valeursBase.length + 1];
			System.arraycopy(valeursBase, 0, valeurs, 0, valeursBase.length);
			valeurs[valeursBase.length] = limite ? LIMITE_DISPLAY : ILLIMITE_DISPLAY;
			return valeurs;
		}
	}
}
