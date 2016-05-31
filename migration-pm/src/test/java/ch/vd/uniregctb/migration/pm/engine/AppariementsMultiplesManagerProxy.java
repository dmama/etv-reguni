package ch.vd.uniregctb.migration.pm.engine;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class AppariementsMultiplesManagerProxy implements AppariementsMultiplesManager {

	private AppariementsMultiplesManager target;

	public void setup(AppariementsMultiplesManager target) {
		Assert.assertNull(this.target);
		Assert.assertNotNull(target);
		this.target = target;
	}

	@NotNull
	@Override
	public Set<Long> getIdentifiantsEntreprisesAvecMemeAppariement(long noCantonal) {
		Assert.assertNotNull(target);
		return target.getIdentifiantsEntreprisesAvecMemeAppariement(noCantonal);
	}
}
