package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * <b>Dans la version 3 du web-service :</b> <i>debtorType</i> (xml) / <i>Debtor</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DebiteurHisto", propOrder = {
		"raisonSociale", "categorie", "periodiciteDecompte", "periodeDecompte", "modeCommunication", "sansRappel",
		"sansListRecapitulative", "contribuableAssocie", "periodicites","logicielId"
})
public class DebiteurHisto extends TiersHisto {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>name</i>
	 */
	@XmlElement(required = true)
	public String raisonSociale;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>category</i>
	 */
	@XmlElement(required = true)
	public CategorieDebiteur categorie;

	/**
	 * <b>Dans la version 3 du web-service :</b> supprimé. Se déduit de la collection <i>periodicities</i> en prenant le dernier élément.
	 */
	@XmlElement(required = true)
	public PeriodiciteDecompte periodiciteDecompte;

	/**
	 * <b>Dans la version 3 du web-service :</b> supprimé. Se déduit de la collection <i>periodicities</i> en prenant le dernier élément.
	 */
	@XmlElement(required = false)
	public PeriodeDecompte periodeDecompte;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>communicationMode</i>
	 */
	@XmlElement(required = true)
	public ModeCommunication modeCommunication;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>withoutReminder</i>
	 */
	@XmlElement(required = true)
	public boolean sansRappel;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>withoutWithholdingTaxDeclaration</i>
	 */
	@XmlElement(required = true)
	public boolean sansListRecapitulative;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>softwareId</i>
	 */
	@XmlElement(required = false)
	public Long logicielId;

	/**
	 * Le numéro du contribuable associé à ce débiteur; ou <b>null</b> si le débiteur n'est pas lié à un contribuable.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>associatedTaxpayerNumber</i>
	 */
	@XmlElement(required = false)
	public Long contribuableAssocie;

	/**
	 * Historique des periodicités du débiteur
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>periodicities</i>
	 */
	@XmlElement(required = false)
	public List<Periodicite> periodicites;

	public DebiteurHisto() {
	}

	public DebiteurHisto(ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur, Set<TiersPart> parts, Context context) throws BusinessException {
		super(debiteur, parts, context);
		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			initPeriodicites(debiteur);
		}
		setBase(context, debiteur);
	}

	private void initPeriodicites(DebiteurPrestationImposable debiteur) {
		this.periodicites = new ArrayList<>();
		for (ch.vd.uniregctb.declaration.Periodicite periodicite : debiteur.getPeriodicitesNonAnnules(true)) {
			this.periodicites.add(new Periodicite(periodicite));
		}
	}

	public DebiteurHisto(ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur, int periode, Set<TiersPart> parts, Context context) throws BusinessException {
		super(debiteur, periode, parts, context);
		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));
			initPeriodicites(debiteur, range);
		}
		setBase(context, debiteur);
	}

	private void initPeriodicites(DebiteurPrestationImposable debiteur, DateRangeHelper.Range range) {
		this.periodicites = new ArrayList<>();
		// Ajoute les périodicités
		for (ch.vd.uniregctb.declaration.Periodicite periodicite : debiteur.getPeriodicitesNonAnnules(true)) {
			if (range != null && !DateRangeHelper.intersect(periodicite, range)) {
				continue;
			}

			this.periodicites.add(new Periodicite(periodicite));
		}
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
		this.periodicites = debiteur.periodicites;
		this.logicielId = debiteur.logicielId;
	}

	private void setBase(Context context, ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur) {
		this.raisonSociale = BusinessHelper.getRaisonSociale(debiteur, null, context.adresseService);
		this.categorie = EnumHelper.coreToWeb(debiteur.getCategorieImpotSource());

		final ch.vd.uniregctb.declaration.Periodicite periodicite = debiteur.getPeriodiciteAt(RegDate.get());
		if (periodicite != null) {
			this.periodeDecompte = EnumHelper.coreToWeb(periodicite.getPeriodeDecompte());
			this.periodiciteDecompte = EnumHelper.coreToWeb(periodicite.getPeriodiciteDecompte());
		}
		else {
			this.periodeDecompte = null;
			this.periodiciteDecompte = null;
		}

		this.modeCommunication = EnumHelper.coreToWeb(debiteur.getModeCommunication());
		this.sansRappel = DataHelper.coreToWeb(debiteur.getSansRappel());
		this.sansListRecapitulative = DataHelper.coreToWeb(debiteur.getSansListeRecapitulative());
		this.contribuableAssocie = debiteur.getContribuableId();
		if (this.modeCommunication == ModeCommunication.ELECTRONIQUE) {
			this.logicielId = debiteur.getLogicielId();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TiersHisto clone(Set<TiersPart> parts) {
		return new DebiteurHisto(this, parts);
	}
}
