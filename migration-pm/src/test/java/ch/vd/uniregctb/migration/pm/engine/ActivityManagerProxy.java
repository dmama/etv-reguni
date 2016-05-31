package ch.vd.uniregctb.migration.pm.engine;

import org.junit.Assert;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;

public class ActivityManagerProxy implements ActivityManager {

	private ActivityManager target;

	@Override
	public boolean isActive(RegpmEntreprise entreprise) {
		Assert.assertNotNull(target);
		return target.isActive(entreprise);
	}

	public void setup(ActivityManager target) {
		Assert.assertNull(this.target);
		Assert.assertNotNull(target);
		this.target = target;
	}
}
