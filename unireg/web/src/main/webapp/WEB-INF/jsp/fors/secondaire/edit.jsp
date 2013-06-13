<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.fors.EditForSecondaireView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.for.secondaire">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="head">
		<style type="text/css">
			h1 {
				margin-left: 10px;
				padding-left: 40px;
				padding-top: 4px;
				height: 32px;
				background: url(../css/x/fors/secondaire_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<table border="0"><tr valign="top">
		<td>
			<form:form commandName="command" action="edit.do">
				<fieldset>
					<legend><span><fmt:message key="label.for.fiscal" /></span></legend>

					<form:hidden path="id"/>

					<!-- Debut For -->
					<table border="0">
						<unireg:nextRowClass reset="0"/>
						<tr class="<unireg:nextRowClass/>" >
							<td width="20%"><fmt:message key="label.genre.impot"/>&nbsp;:</td>
							<td><fmt:message key="option.genre.impot.REVENU_FORTUNE"/></td>
							<td width="20%"><fmt:message key="label.rattachement"/>&nbsp;:</td>
							<td><fmt:message key="option.rattachement.${command.motifRattachement}" /></td>
						</tr>
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateDebut" />
									<jsp:param name="id" value="dateDebut" />
									<jsp:param name="onChange" value="updateSyncActions" />
									<jsp:param name="onkeyup" value="updateSyncActions" />
								</jsp:include>
							</td>
							<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateFin" />
									<jsp:param name="id" value="dateFin" />
									<jsp:param name="onChange" value="updateSyncActions" />
									<jsp:param name="onkeyup" value="updateSyncActions" />
								</jsp:include>
							</td>
						</tr>
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
							<td>
								<form:select path="motifDebut" cssStyle="width:30ex" onchange="updateSyncActions();" onkeyup="updateSyncActions();"/>
								<form:errors path="motifDebut" cssClass="error" />
							</td>
							<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
							<td>
								<form:select path="motifFin" cssStyle="width:30ex" onchange="updateSyncActions();" onkeyup="updateSyncActions();"/>
								<form:errors path="motifFin" cssClass="error" />
							</td>
						</tr>

						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
							<td><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD" /></td>
							<td><label for="autoriteFiscale"><fmt:message key="label.commune.fraction"/>&nbsp;:</label></td>
							<td>
								<input id="autoriteFiscale" size="25" />
								<form:errors path="noAutoriteFiscale" cssClass="error" />
								<form:hidden path="noAutoriteFiscale" />
							</td>
						</tr>
					</table>
				</fieldset>

				<table border="0">
					<tr>
						<td width="25%">&nbsp;</td>
						<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
						<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>
			</form:form>

		</td>
		<td id="actions_column" style="display:none">
			<div id="actions_list"/>
		</td>
		</tr></table>

		<script type="text/javascript">
			function updateSyncActions() {

				var motifsDebut = $('#motifDebut').val();
				var dateDebut = $('#dateDebut').val();
				var motifsFin = $('#motifFin').val();
				var dateFin = $('#dateFin').val();

				var noOfsAut = $('#noAutoriteFiscale').val();
				noOfsAut = noOfsAut.replace(/[^\d]/g, "");

				if (StringUtils.isBlank(noOfsAut)) {
					// [SIFISC-5265] pas de commune valable, on laisse tomber l'affichage des actions
					$('#actions_column').hide();
					return;
				}

				var idFor = ${command.id};
				var queryString = 'idFor=' + idFor + '&startDate=' + dateDebut + '&startReason=' + motifsDebut +
						'&endDate=' + dateFin + '&endReason=' + motifsFin + '&noOfs=' + noOfsAut + '&' + new Date().getTime();

				$.get('<c:url value="/simulate/forFiscalUpdate.do"/>?' + queryString, function(results) {
					if (!results || results.empty) {
						$('#actions_column').hide();
					}
					else {
						$('#actions_list').html(Fors.buildActionTableHtml(results));
						$('#actions_column').show();
					}
				}, 'json').error(Ajax.notifyErrorHandler("simulation des changements"));
			}


			// on initialise l'auto-completion de l'autorité fiscale
			$('#autoriteFiscale').val('<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" escapeMode="javascript"/>');
			Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale', updateSyncActions);

			// on initialise les motifs au chargement de la page
			Fors.updateMotifsOuverture($('#motifDebut'), '${command.tiersId}', 'REVENU_FORTUNE', '${command.motifRattachement}', '${command.motifDebut}');
			Fors.updateMotifsFermeture($('#motifFin'), '${command.tiersId}', 'REVENU_FORTUNE', '${command.motifRattachement}', '${command.motifFin}');
		</script>

	</tiles:put>
</tiles:insert>
