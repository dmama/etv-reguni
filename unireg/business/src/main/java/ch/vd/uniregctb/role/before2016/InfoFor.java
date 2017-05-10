package ch.vd.uniregctb.role.before2016;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class InfoFor implements DateRange {

	//
	// information glânée de potentiellement plusieurs fors
	//

	public final InfoContribuable.TypeContribuable typeCtb;
	public final RegDate dateDebut;
	public final RegDate dateFin;
	public final MotifAssujettissement motifDebut;
	public final MotifAssujettissement motifFin;
	public final InfoContribuable.TypeAssujettissement typeAssujettissement;
	public final InfoContribuable.TypeContribuable ancienTypeCtb;

	//
	// information récupérée d'un for spécifique
	//

	public final boolean forPrincipal;
	public final MotifRattachement motifRattachement;
	public final int ofsCommune;
	public final RegDate dateOuvertureFor;
	public final RegDate dateFermetureFor;

	/**
	 * Constructeur pour les assujettissements sur la période
	 */
	public InfoFor(InfoContribuable.TypeContribuable typeCtb, RegDate dateDebut, MotifAssujettissement motifDebut, RegDate dateFin, MotifAssujettissement motifFin,
	               InfoContribuable.TypeAssujettissement typeAssujettissement, ForFiscalRevenuFortune forFiscal) {
		this(typeCtb, dateDebut, motifDebut, dateFin, motifFin, typeAssujettissement, null, forFiscal.isPrincipal(), forFiscal.getMotifRattachement(), forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getDateDebut(), forFiscal.getDateFin());
		Assert.isTrue(typeAssujettissement == InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF || typeAssujettissement == InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF);
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscal.getTypeAutoriteFiscale());
	}

	/**
	 * Constructeur pour les fors fermés dans la période fiscale, et qui ont donné lieu à une fin d'assujettissement
	 * à la fin de la période fiscale précédente (attention, les données "début" et "fin" ne sont pas nécessairement issues du même for fiscal !)
	 * @param dateDebut date d'ouverture du for déterminant pour l'assujettissement maintenant terminé
	 * @param motifDebut motif d'ouverture de ce même for
	 * @param dateFin date de fermeture du for déterminant (ne devrait pas être nulle)
	 * @param motifFin motif de fermeture de ce même for (ne devrait pas être nul)
	 * @param ancienTypeCtb type du contribuable dans la période fiscale précédente (il est maintenant non-assujetti)
	 * @param ofsCommune numéro OFS de la commune du for (vaudois!)
	 */
	public InfoFor(RegDate dateDebut, MotifAssujettissement motifDebut, RegDate dateFin, MotifAssujettissement motifFin, InfoContribuable.TypeContribuable ancienTypeCtb, boolean forPrincipal,
	               MotifRattachement motifRattachement, int ofsCommune, RegDate dateOuvertureFor, RegDate dateFermetureFor) {
		this(InfoContribuable.TypeContribuable.NON_ASSUJETTI, dateDebut, motifDebut, dateFin, motifFin, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, ancienTypeCtb, forPrincipal, motifRattachement, ofsCommune, dateOuvertureFor, dateFermetureFor);
	}

	/**
	 * Constructeur interne factorisé
	 */
	private InfoFor(InfoContribuable.TypeContribuable typeCtb, RegDate dateDebut, MotifAssujettissement motifDebut, RegDate dateFin, MotifAssujettissement motifFin,
	                InfoContribuable.TypeAssujettissement typeAssujettissement, InfoContribuable.TypeContribuable ancienTypeCtb, boolean forPrincipal,
	                MotifRattachement motifRattachement, int ofsCommune, RegDate dateOuvertureFor, RegDate dateFermetureFor) {

		Assert.notNull(dateDebut);
		Assert.notNull(dateOuvertureFor);
		Assert.notNull(motifDebut);

		this.typeCtb = typeCtb;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.motifDebut = motifDebut;
		this.motifFin = motifFin;
		this.typeAssujettissement = typeAssujettissement;
		this.ancienTypeCtb = ancienTypeCtb;

		this.forPrincipal = forPrincipal;
		this.motifRattachement = motifRattachement;
		this.ofsCommune = ofsCommune;
		this.dateOuvertureFor = dateOuvertureFor;
		this.dateFermetureFor = dateFermetureFor;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
