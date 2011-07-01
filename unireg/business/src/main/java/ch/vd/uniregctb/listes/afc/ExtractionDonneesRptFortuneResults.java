package ch.vd.uniregctb.listes.afc;

import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ExtractionDonneesRptFortuneResults extends ExtractionDonneesRptResults {

	private final RegDate finAnnee;

	private static final String ASSUJETTI_SANS_FOR_VD_31_12 = "Assujetti sans for vaudois au 31 décembre";
	private static final String NON_ASSUJETTI_31_12 = "Non-assujetti au rôle ordinaire au 31 décembre";
	private static final String NON_ASSUJETTI_ROLE_ORDINAIRE = "Non-assujetti au rôle ordinaire";

	public ExtractionDonneesRptFortuneResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService);
		this.finAnnee = RegDate.get(periodeFiscale, 12, 31);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.FORTUNE;
	}

	@Override
	protected String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer) {
		// pour la fortune, on ne s'intéresse qu'aux contribuables au rôle ordinaire assujettis en fin d'année fiscale
		final Iterator<Assujettissement> iterSrc = listeAFiltrer.iterator();
		while (iterSrc.hasNext()) {
			final Assujettissement a = iterSrc.next();
			if (a instanceof SourcierPur) {
				iterSrc.remove();
			}
		}

		if (listeAFiltrer.size() == 0) {
			return NON_ASSUJETTI_ROLE_ORDINAIRE;
		}

		// on enlève ensuite tous les assujettissements qui ne couvrent pas la fin de l'année
		final Iterator<Assujettissement> iterFinAnnee = listeAFiltrer.iterator();
		while (iterFinAnnee.hasNext()) {
			final Assujettissement a = iterFinAnnee.next();
			if (!a.isValidAt(finAnnee)) {
				iterFinAnnee.remove();
			}
		}

		if (listeAFiltrer.size() == 0) {
			return NON_ASSUJETTI_31_12;
		}

		// [UNIREG-3248] malgré l'assujettissement, si le contribuable n'a aucun for vaudois au 31 décembre, il
		// faut l'ignorer dans le cadre de l'extraction "fortune"
		final List<ForFiscal> fors = ctb.getForsFiscauxValidAt(finAnnee);
		boolean trouveVaudois = false;
		if (fors != null && fors.size() > 0) {
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
	protected InfoPeriodeImposition buildInfoPeriodeImposition(Contribuable ctb, String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal,
	                                                           Long noCtbConjoint, ModeImposition modeImposition, MotifRattachement motifRattachement, Assujettissement a,
	                                                           Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		final boolean limite = a instanceof HorsCanton || a instanceof HorsSuisse;
		return new InfoPeriodeImpositionFortune(ctb.getNumero(), nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint, modeImposition, a.getDateDebut(), a.getDateFin(),
		                                        motifRattachement, a.getMotifFractDebut(), a.getMotifFractFin(), ofsCommuneForGestion, autoriteFiscaleForPrincipal, limite);
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

		public InfoPeriodeImpositionFortune(long noCtb, String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal, Long noCtbConjoint, ModeImposition modeImposition,
		                                     RegDate debutPeriodeImposition, RegDate finPeriodeImposition, MotifRattachement motifRattachement, MotifFor motifOuverture, MotifFor motifFermeture,
		                                     Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal, boolean limite) {
			super(noCtb, nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint, modeImposition, debutPeriodeImposition, finPeriodeImposition, motifRattachement, motifOuverture,
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
