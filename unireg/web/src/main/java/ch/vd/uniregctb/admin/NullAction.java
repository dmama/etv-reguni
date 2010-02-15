/**
 *
 */
package ch.vd.uniregctb.admin;

import org.springmodules.xt.ajax.AjaxAction;

/**
 * Action javascript qui ne fait rien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
final class NullAction implements AjaxAction {

	private static final long serialVersionUID = 6948565481274427280L;

	public String render() {
		return "";
	}
}