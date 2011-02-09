<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
	<tiles:put name="body">

		<div id="tiers-picker">

			<div id="tiers-picker-filter-description"></div>

			<div id="tiers-picker-quicktab">
				<%-- voir la méthode open_tiers_picker() dans dialog.js --%>
				<table><tr>
				<td style="vertical-align:middle"><input type="text" id="tiers-picker-query" class="text ui-widget-content ui-corner-all" placeholder="Entrez vos critères de recherche ici" autofocus/></td>
				<td width="12px"></td>
				<td style="vertical-align:middle" width="2%"><a href="#" onclick="return toogle_search();" style="font-size:11px">recherche avancée</a></td>
				</tr></table>
			</div>

			<div id="tiers-picker-fulltab" style="display:none;">
				<table>
				<tr>
					<td style="width:11em"><fmt:message key="label.numero.tiers"/>&nbsp;:</td><td><input type="text" id="tiers-picker-id"/></td>
					<td></td><td align="right"><a href="#" onclick="return toogle_search();" style="font-size:11px">recherche simple</a></td>
				</tr>
				<tr>
					<td><fmt:message key="label.nom.raison"/>&nbsp;:</td><td><input type="text" id="tiers-picker-nomraison" style="width:100%"/></td>
					<td style="width:9em; padding-left:1em"><fmt:message key="label.localite.postale.suisse"/>&nbsp;:</td><td><input type="text" id="tiers-picker-localite" style="width:97%"/></td>
				</tr>
				<tr>
					<td><fmt:message key="label.date.naissance"/>&nbsp;:</td><td><input type="text" id="tiers-picker-datenaissance" size="10" maxlength="10" class="date"/></td>
					<td style="padding-left:1em"><fmt:message key="label.numero.avs"/>&nbsp;:</td><td><input type="text" id="tiers-picker-noavs"/></td>
				</tr>
				<tr>
					<td colspan="4" align="center">
						<input type="button" id="fullSearch" value="Chercher" style="width:10em; margin-right:3em;"/>
						<input type="button" id="clearAll" value="Effacer" style="width:10em"/>
					</td>
				</tr>
				</table>
				<script>
					function toogle_search() {
						$('#tiers-picker-quicktab').toggle();
						$('#tiers-picker-fulltab').toggle();
						$('#tiers-picker-results').attr('innerHTML', '');
						return false;
					}
					$(function() {
						autocomplete_infra('localiteOuPays', '#tiers-picker-localite', function(item) {
							if (!item) {
								$('#tiers-picker-localite').val(null);
							}
						});
						$('#tiers-picker-datenaissance').datepicker({
							showOn: "button",
							showAnim: '',
							yearRange: '1900:+10',
							buttonImage: "<c:url value='/css/x/calendar_off.gif'/>",
							buttonImageOnly: true,
							changeMonth: true,
							changeYear: true
						});
					});
				</script>
			</div>

			<div id="tiers-picker-results"><%-- cette DIV est mise-à-jour par Ajax --%></div>

			<script>
		    $(function() {
				// fallback autofocus pour les browsers qui ne le supportent pas
				if (!("autofocus" in document.createElement("input"))) {
					$('#tiers-picker-query').focus();
				}
			});
			</script>
		</div>

	</tiles:put>
</tiles:insert>
