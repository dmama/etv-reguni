package ch.vd.uniregctb.web.xt.handler.remarque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.web.xt.component.SimpleText;
import ch.vd.uniregctb.web.xt.handler.BreakLineComponent;

/**
 * [UNIREG-1059] Handler qui permet d'afficher les remarques d'un tiers, et d'en ajouter.
 */
public class RemarqueHandler extends AbstractAjaxHandler {

	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private RemarqueDAO remarqueDAO;

	@SuppressWarnings({"UnusedDeclaration", "unchecked"})
	public AjaxResponse refreshRemarques(AjaxActionEvent event) {

		final Map<String, String> parameters = event.getParameters();
		final Long tiersId = Long.valueOf(parameters.get("tiersId"));

		final MutableInt count = new MutableInt(0);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Component> components = (List<Component>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Component> list = new ArrayList<Component>();

				final RemarquesTable table = buildRemarquesTable(tiersId);
				if (table == null) {
					if (canAddRemark()) {
						list.add(new AddRemarqueLink(tiersId));
						list.add(new BreakLineComponent());
						list.add(new BreakLineComponent());
					}
					list.add(new SimpleText("(aucune remarque n'a été saisie pour l'instant)"));
				}
				else {
					count.setValue(table.getCount());
					if (canAddRemark()) {
						list.add(new AddRemarqueLink(tiersId));
					}
					list.add(table);
				}
				return list;
			}
		});

		// Met-à-jour le table des remarques
		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction("remarques", components));

		// Met-à-jour le nombre de remarques affiché dans la tabulation
		response.addAction(new ReplaceContentAction("remarqueTabAnchor", new RemarqueTabText(count.intValue())));

		return response;
	}

	/**
	 * @return <b>vrai</b> si l'utilisateur courant peut ajouter une remarque sur le tiers courant; <b>faux</b> autrement.
	 */
	private boolean canAddRemark() {
		return SecurityProvider.isGranted(Role.COOR_FIN) ||
				SecurityProvider.isGranted(Role.MODIF_AC) ||
				SecurityProvider.isGranted(Role.MODIF_VD_ORD) ||
				SecurityProvider.isGranted(Role.MODIF_VD_SOURC) ||
				SecurityProvider.isGranted(Role.MODIF_HC_HS) ||
				SecurityProvider.isGranted(Role.MODIF_HAB_DEBPUR) ||
				SecurityProvider.isGranted(Role.MODIF_NONHAB_DEBPUR) ||
				SecurityProvider.isGranted(Role.MODIF_PM) ||
				SecurityProvider.isGranted(Role.MODIF_CA) ||
				SecurityProvider.isGranted(Role.MODIF_NONHAB_INACTIF);
	}

	private RemarquesTable buildRemarquesTable(Long tiersId) {

		final List<Remarque> remarques = remarqueDAO.getRemarques(tiersId);
		if (remarques == null || remarques.isEmpty()) {
			return null;
		}

		// On affiche les remarques les plus récentes en premier
		Collections.sort(remarques, new Comparator<Remarque>() {
			public int compare(Remarque o1, Remarque o2) {
				return o2.getLogCreationDate().compareTo(o1.getLogCreationDate());
			}
		});

		final RemarquesTable table = new RemarquesTable();
		for (Remarque r : remarques) {
			table.addRemarque(r);
		}

		return table;
	}

	@SuppressWarnings({"UnusedDeclaration", "unchecked"})
	public AjaxResponse addRemarque(AjaxActionEvent event) {

		final Map<String, String> parameters = event.getParameters();
		final Long tiersId = Long.valueOf(parameters.get("tiersId"));

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Component> components = (List<Component>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Component> list = new ArrayList<Component>();

				final NewRemarqueForm form = new NewRemarqueForm(tiersId);
				list.add(form);
				final RemarquesTable table = buildRemarquesTable(tiersId);
				if (table != null) {
					list.add(table);
				}
				return list;
			}
		});

		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction("remarques", components));
		return response;
	}

	@SuppressWarnings({"UnusedDeclaration", "unchecked"})
	public AjaxResponse saveRemarque(AjaxSubmitEvent event) {

		final Map<String, String> parameters = event.getParameters();
		final Long tiersId = Long.valueOf(parameters.get("tiersId"));
		final String texte = parameters.get("texte");

		final MutableInt count = new MutableInt(0);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Component> components = (List<Component>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				if (StringUtils.isNotBlank(texte)) {
					final Tiers tiers = tiersDAO.get(tiersId);
					Assert.notNull(tiers);
					final Remarque remarque = new Remarque();
					final String t = (texte.length() > LengthConstants.TIERS_REMARQUE ? texte.substring(0, LengthConstants.TIERS_REMARQUE - 1) : texte);
					remarque.setTexte(t);
					remarque.setTiers(tiers);
					remarqueDAO.save(remarque);
					remarqueDAO.getHibernateTemplate().flush();
				}

				final List<Component> list = new ArrayList<Component>();
				if (canAddRemark()) {
					list.add(new AddRemarqueLink(tiersId));
				}
				
				final RemarquesTable table = buildRemarquesTable(tiersId);
				if (table != null) {
					count.setValue(table.getCount());
					list.add(table);
				}
				return list;
			}
		});

		// Met-à-jour le table des remarques
		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction("remarques", components));

		// Met-à-jour le nombre de remarques affiché dans la tabulation
		response.addAction(new ReplaceContentAction("remarqueTabAnchor", new RemarqueTabText(count.intValue())));

		return response;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}
}
