<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.fors.EditForPrincipalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.for.principal">
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
				background: url(../css/x/fors/principal_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<table border="0"><tr valign="top">
		<td>
			<form:form commandName="command" action="editPrincipal.do">
				<fieldset>
					<legend><span><fmt:message key="label.for.fiscal" /></span></legend>

					<form:hidden path="id"/>
					<form:hidden path="tiersId"/>

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
							<td><unireg:regdate regdate="${command.dateDebut}"/></td>
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
							<td><fmt:message key="option.motif.ouverture.${command.motifDebut}" /></td>
							<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
							<td>
								<form:select path="motifFin" cssStyle="width:30ex" onchange="updateSyncActions();" onkeyup="updateSyncActions();"/>
								<form:errors path="motifFin" cssClass="error" />
							</td>
						</tr>

						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
							<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}" /></td>
							<td>
								<label for="autoriteFiscale">
								<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
									<fmt:message key="label.commune.fraction"/>
								</c:if>
								<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">
									<fmt:message key="label.commune"/>
								</c:if>
								<c:if test="${command.typeAutoriteFiscale == 'PAYS_HS'}">
									<fmt:message key="label.pays"/>
								</c:if>
								&nbsp;:
								</label>
							</td>
							<td>
								<input id="autoriteFiscale" size="25">
								<form:errors path="noAutoriteFiscale" cssClass="error" />
								<form:hidden path="noAutoriteFiscale" />
							</td>
						</tr>
					</table>
				</fieldset>

				<form:errors cssClass="error" />
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

				var motifsDebut = '${command.motifDebut}';
				var dateDebut = '<unireg:regdate regdate="${command.dateDebut}" />';
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

			// on initialise les motifs au chargement de la page
			Fors.updateMotifsFermeture($('#motifFin'), '${command.tiersId}', 'REVENU_FORTUNE', '${command.motifRattachement}', '${command.motifFin}');

			<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
			$('#autoriteFiscale').val('<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomMinuscule"/>');
			Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale', updateSyncActions);
			</c:if>
			<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">
			$('#autoriteFiscale').val('<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomMinuscule"/>');
			Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale', updateSyncActions);
			</c:if>
			<c:if test="${command.typeAutoriteFiscale == 'PAYS_HS'}">
			$('#autoriteFiscale').val('<unireg:infra entityType="pays" entityId="${command.noAutoriteFiscale}" entityPropertyName="nomMinuscule"/>');
			Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale', updateSyncActions);
			</c:if>
		</script>

	</tiles:put>
</tiles:insert>
