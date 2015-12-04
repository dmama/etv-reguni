package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

@SuppressWarnings("UnusedDeclaration")
public class DeclarationListView {

	private final long ctbId;
	private final List<DeclarationView> dis;
	private boolean ctbPP;
	private boolean ctbPM;

	public DeclarationListView(Contribuable ctb, MessageSource messageSource) {
		this.ctbId = ctb.getId();
		this.dis = initDeclarations(ctb.getDeclarations(), messageSource);
		this.ctbPP = ctb instanceof ContribuableImpositionPersonnesPhysiques;
		this.ctbPM = ctb instanceof ContribuableImpositionPersonnesMorales;
	}

	public static List<DeclarationView> initDeclarations(Collection<Declaration> declarations, MessageSource messageSource) {
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DeclarationView> views = new ArrayList<>(declarations.size());
		for (Declaration declaration : declarations) {
			views.add(new DeclarationView(declaration, messageSource));
		}
		Collections.sort(views, new Comparator<DeclarationView>() {
			@Override
			public int compare(DeclarationView o1, DeclarationView o2) {
				if (o1.isAnnule() && !o2.isAnnule()) {
					return 1;
				}
				else if (!o1.isAnnule() && o2.isAnnule()) {
					return -1;
				}
				else {
					return o2.getDateDebut().compareTo(o1.getDateDebut());
				}
			}
		});
		return views;
	}

	public long getCtbId() {
		return ctbId;
	}

	public List<DeclarationView> getDis() {
		return dis;
	}

	public boolean isCtbPP() {
		return ctbPP;
	}

	public boolean isCtbPM() {
		return ctbPM;
	}
}
