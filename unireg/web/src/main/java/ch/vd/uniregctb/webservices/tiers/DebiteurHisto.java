package ch.vd.uniregctb.webservices.tiers;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.webservices.tiers.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.tiers.impl.Context;
import ch.vd.uniregctb.webservices.tiers.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DebiteurHisto", propOrder = {
		"raisonSociale", "categorie", "periodiciteDecompte", "periodeDecompte", "modeCommunication", "sansRappel",
		"sansListRecapitulative", "contribuableAssocie"
})
public class DebiteurHisto extends TiersHisto {

	@XmlElement(required = true)
	public String raisonSociale;

	@XmlElement(required = true)
	public CategorieDebiteur categorie;

	@XmlElement(required = true)
	public PeriodiciteDecompte periodiciteDecompte;

	@XmlElement(required = false)
	public PeriodeDecompte periodeDecompte;

	@XmlElement(required = true)
	public ModeCommunication modeCommunication;

	@XmlElement(required = true)
	public boolean sansRappel;

	@XmlElement(required = true)
	public boolean sansListRecapitulative;

	/** Le numéro du contribuable associé à ce débiteur; ou <b>null</b> si le débiteur n'est pas lié à un contribuable. */
	@XmlElement(required = false)
	public Long contribuableAssocie;

	public DebiteurHisto() {
	}

	public DebiteurHisto(ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur, Set<TiersPart> parts, Context context) {
		super(debiteur, parts, context);
		setBase(context, debiteur);
	}

	public DebiteurHisto(ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur, int periode, Set<TiersPart> parts, Context context) {
		super(debiteur, periode, parts, context);
		setBase(context, debiteur);
	}

	public DebiteurHisto(DebiteurHisto debiteur, Set<TiersPart> parts) {
		super(debiteur, parts);
		this.raisonSociale = debiteur.raisonSociale;
		this.categorie = debiteur.categorie;
		this.periodiciteDecompte = debiteur.periodiciteDecompte;
		this.periodeDecompte = debiteur.periodeDecompte;
		this.modeCommunication = debiteur.modeCommunication;
		this.sansRappel = debiteur.sansRappel;
		this.sansListRecapitulative = debiteur.sansListRecapitulative;
		this.contribuableAssocie = debiteur.contribuableAssocie;
	}

	private void setBase(Context context, ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur) {
		this.raisonSociale = BusinessHelper.getRaisonSociale(debiteur, null, context.adresseService);
		this.categorie = EnumHelper.coreToWeb(debiteur.getCategorieImpotSource());
		this.periodiciteDecompte = EnumHelper.coreToWeb(debiteur.getPeriodiciteDecompte());
		this.periodeDecompte = EnumHelper.coreToWeb(debiteur.getPeriodeDecompte());
		this.modeCommunication = EnumHelper.coreToWeb(debiteur.getModeCommunication());
		this.sansRappel = DataHelper.coreToWeb(debiteur.getSansRappel());
		this.sansListRecapitulative = DataHelper.coreToWeb(debiteur.getSansListeRecapitulative());
		final Contribuable contribuable = debiteur.getContribuable();
		this.contribuableAssocie = (contribuable == null ? null : contribuable.getNumero());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TiersHisto clone(Set<TiersPart> parts) {
		return new DebiteurHisto(this, parts);
	}
}
