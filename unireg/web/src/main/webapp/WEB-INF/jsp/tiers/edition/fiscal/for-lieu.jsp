<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tr class="<unireg:nextRowClass/>" >

	<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
	<%-- modification du type d'autorité fiscale interdit --%>
	<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/></td>

	<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">

		<td id="for_fraction_commune_label"><fmt:message key="label.commune.fraction"/>&nbsp;:</td>
		<td id="for_fraction_commune">
			<form:input path="libFractionCommune" id="libFractionCommune" size="25" />
			<form:errors path="libFractionCommune" cssClass="error" />
			<form:hidden path="numeroForFiscalCommune" />
			<script>
				$(function() {
					Autocomplete.infra('communeVD', '#libFractionCommune', true, function(item) {
						$('#numeroForFiscalCommune').val(item ? item.id1 : null);
						<c:if test="${param['onChange'] != null}">
						${param['onChange']}();
						</c:if>
					});
				});
			</script>
		</td>

	</c:if>

	<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">

		<td id="for_commune_label" style="display:none;"><fmt:message key="label.commune"/>&nbsp;:</td>
		<td id="for_commune" style="display:none;">
			<form:input path="libCommuneHorsCanton" id="libCommuneHorsCanton" size="25" />
			<form:errors path="libCommuneHorsCanton" cssClass="error" />
			<form:hidden path="numeroForFiscalCommuneHorsCanton" />
			<script>
				$(function() {
					Autocomplete.infra('communeHC', '#libCommuneHorsCanton', true, function(item) {
						$('#numeroForFiscalCommuneHorsCanton').val(item ? item.id1 : null);
						<c:if test="${param['onChange'] != null}">
						${param['onChange']}();
						</c:if>
					});
				});
			</script>
		</td>

	</c:if>

	<c:if test="${command.typeAutoriteFiscale == 'PAYS_HS'}">

		<td id="for_pays_label" style="display:none;"><fmt:message key="label.pays"/>&nbsp;:</td>
		<td id="for_pays" style="display:none;">
			<form:input path="libPays" id="libPays" size="25" />
			<form:errors path="libPays" cssClass="error" />
			<form:hidden path="numeroForFiscalPays" />
			<script>
				$(function() {
					Autocomplete.infra('etat', '#libPays', true, function(item) {
						$('#numeroForFiscalPays').val(item ? item.id1 : null);
						<c:if test="${param['onChange'] != null}">
						${param['onChange']}();
						</c:if>
					});
				});
			</script>
		</td>

	</c:if>
</tr>