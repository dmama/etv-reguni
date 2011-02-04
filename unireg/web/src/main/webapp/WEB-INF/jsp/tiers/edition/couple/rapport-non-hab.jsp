<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset class="coupleMenageCommun">
	<legend><span><fmt:message key="label.rapport.menage.commun" /></span></legend>
	<table>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td colspan="2">
				<!-- nouveau contribuable -->
				<form:radiobutton path="nouveauCtb" id="nouveauCtb" value="true" onclick="refresh_panels();"/>
				<label for="nouveauCtb"><fmt:message key="label.couple.nouveau.contribuable" /></label>
				<div id="nouveauCtbPanel" style="display:none;">
					<table>
						<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
							<td width="10%">&nbsp;</td>
							<td width="150px"><fmt:message key="label.date.debut" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateDebut" />
									<jsp:param name="id" value="dateDebut" />
								</jsp:include>
								<font color="#FF0000">*</font>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td colspan="2">
				<!-- contribuable existant -->
				<form:radiobutton path="nouveauCtb" id="ctbExistant" value="false" onclick="refresh_panels();" />
				<label for="ctbExistant"><fmt:message key="label.couple.contribuable.existant" /></label>

				<div id="ctbExistantPanel" style="display:none;">

					<table>
						<tr>
							<td width="10%">&nbsp;</td>
							<td width="150px"><fmt:message key="label.date.debut"/>&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateCoupleExistant" />
									<jsp:param name="id" value="dateCoupleExistant" />
								</jsp:include>
								<font color="#FF0000">*</font>
							</td>
							<td width="45%" rowspan="2" valign="top"><div id="vignetteTroisiemeTiers"/></td>
						</tr>
						<tr>
							<td>&nbsp;</td>
							<td valign="top"><fmt:message key="label.numero.contribuable"/>&nbsp;:</td>
							<td valign="top">
								<form:input path="numeroTroisiemeTiers" id="numeroTroisiemeTiers"/>
								<button id="button_numeroTroisiemeTiers" onclick="return search_troisieme(this);">...</button>
								<form:errors path="numeroTroisiemeTiers" cssClass="error"/>
							</td>
							<td/>
						</tr>
					</table>

				</div>
			</td>
		</tr>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
			<td width="75%">
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</td>
		</tr>
	</table>

	<script>

		$(function() {
			refresh_panels();

			var troisiemeTiers = $('#numeroTroisiemeTiers');

			// function pour mettre-à-jour la vignette lors de tout changement du numéro du tiers remplaçant
			troisiemeTiers.change(function() {
				var id = $(this).val();
				id = id.replace(/[^0-9]*/g, ''); // remove non-numeric chars
				refresh_troisieme(id);
			});

			// on force l'affichage de la vignette si le numéro du troisième tiers est renseigné lors du chargement de la page
			if (troisiemeTiers.val()) {
				troisiemeTiers.change();
			}
		});

		function refresh_panels() {
			if ($('#nouveauCtb').attr('checked')) {
				$('#nouveauCtbPanel').show();
				$('#ctbExistantPanel').hide();
			}
			else {
				$('#nouveauCtbPanel').hide();
				$('#ctbExistantPanel').show();
			}
		}

		function search_troisieme(button) {
			return open_tiers_picker_with_filter(button, 'coupleRecapPickerFilterFactory', null, function(id) {
				$('#numeroTroisiemeTiers').val(id);
				refresh_troisieme(id);
			});
		}

		function refresh_troisieme(id) {
			if (id) {
				$('#vignetteTroisiemeTiers').load(getContextPath() + '/tiers/vignette.do?numero=' + id + '&titre=Contribuable%20existant&showAvatar=true');
			}
			else {
				$('#vignetteTroisiemeTiers').attr('innerHTML', '');
			}
			return false;
		}
	</script>

</fieldset>
<!-- Fin Rapport Menage Commun -->