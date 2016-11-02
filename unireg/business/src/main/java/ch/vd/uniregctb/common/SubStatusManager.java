package ch.vd.uniregctb.common;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.batchtemplate.StatusManager;

/**
 * Sous status manager qui permet de mapper son pourcentage de progression sur une plage restreinte du status manager parent.
 * <p/>
 * Exemples:
 * <ul>
 *     <li>
 *         <b>SubStatusManager(minRange=0, maxRange=50)</b>
 *         <table border="1">
 *             <tr>
 *                 <th>SubStatusManager</td>
 *                 <th>Parent</td>
 *             </tr>
 *             <tr>
 *                 <td>0%</td>
 *                 <td>0%</td>
 *             </tr>
 *             <tr>
 *                 <td>10%</td>
 *                 <td>5%</td>
 *             </tr>
 *             <tr>
 *                 <td>50%</td>
 *                 <td>25%</td>
 *             </tr>
 *             <tr>
 *                 <td>100%</td>
 *                 <td>50%</td>
 *             </tr>
 *         </table>
 *     </li>
 *     <li>
 *         <b>SubStatusManager(minRange=50, maxRange=100)</b>
 *         <table border="1">
 *             <tr>
 *                 <th>SubStatusManager</td>
 *                 <th>Parent</td>
 *             </tr>
 *             <tr>
 *                 <td>0%</td>
 *                 <td>50%</td>
 *             </tr>
 *             <tr>
 *                 <td>10%</td>
 *                 <td>55%</td>
 *             </tr>
 *             <tr>
 *                 <td>50%</td>
 *                 <td>75%</td>
 *             </tr>
 *             <tr>
 *                 <td>100%</td>
 *                 <td>100%</td>
 *             </tr>
 *         </table>
 *     </li>
 * </ul>
 */
public class SubStatusManager implements StatusManager {

	private final int minRange;
	private final int maxRange;

	@NotNull
	private final StatusManager parent;

	public SubStatusManager(int minRange, int maxRange, @NotNull StatusManager parent) {
		this.minRange = minRange;
		this.maxRange = maxRange;
		this.parent = parent;
	}

	@Override
	public boolean interrupted() {
		return parent.interrupted();
	}

	@Override
	public void setMessage(String msg) {
		parent.setMessage(msg);
	}

	@Override
	public void setMessage(String msg, int percentProgression) {
		final int parentPercent = minRange + ((maxRange - minRange) * percentProgression) / 100;
		parent.setMessage(msg, parentPercent);
	}
}
