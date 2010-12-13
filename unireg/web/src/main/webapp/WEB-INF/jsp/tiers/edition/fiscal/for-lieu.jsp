<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tr class="<unireg:nextRowClass/>" >
	<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>

	<c:if test="${param['limited'] != null}">
		<%-- modification du type d'autorité fiscale interdit --%>
		<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/></td>
	</c:if>
	<c:if test="${param['limited'] == null}">
		<%-- modification du type d'autorité fiscale autorisé --%>
		<td id="select_type_for" >
			<form:select path="typeAutoriteFiscale" items="${typesForFiscal}" id="optionTypeAutoriteFiscale"
						 onchange="selectForFiscal(this.options[this.selectedIndex].value);" />
		</td>
		<td id="type_for_fraction" style="display:none;"><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD" /></td>
		<td id="type_for_hs" style="display:none;"><fmt:message key="option.type.for.fiscal.PAYS_HS" /></td>
	</c:if>

	<td id="for_fraction_commune_label"><fmt:message key="label.commune.fraction"/>&nbsp;:</td>
	<td id="for_fraction_commune">
		<form:input path="libFractionCommune" id="libFractionCommune" size="25" />
		<form:errors path="libFractionCommune" cssClass="error" />
		<form:hidden path="numeroForFiscalCommune" />		
		<script>
			$(function() {
				autocomplete_infra('communeVD', '#libFractionCommune', function(item) {
					if (item) {
						$('#numeroForFiscalCommune').val(item.id1);
					}
					else {
						$('#libFractionCommune').val(null);
						$('#numeroForFiscalCommune').val(null);
					}
					<c:if test="${param['onChange'] != null}">
					${param['onChange']}();
					</c:if>
				});
			});
		</script>
	</td>
	<td id="for_commune_label" style="display:none;"><fmt:message key="label.commune"/>&nbsp;:</td>
	<td id="for_commune" style="display:none;">
		<form:input path="libCommuneHorsCanton" id="libCommuneHorsCanton" size="25" />
		<form:errors path="libCommuneHorsCanton" cssClass="error" />
		<form:hidden path="numeroForFiscalCommuneHorsCanton" />			
		<script>
			$(function() {
				autocomplete_infra('communeVD', '#libCommuneHorsCanton', function(item) {
					if (item) {
						$('#numeroForFiscalCommuneHorsCanton').val(item.id1);
					}
					else {
						$('#libCommuneHorsCanton').val(null);
						$('#numeroForFiscalCommuneHorsCanton').val(null);
					}
					<c:if test="${param['onChange'] != null}">
					${param['onChange']}();
					</c:if>
				});
			});
		</script>
	</td>
	<td id="for_pays_label" style="display:none;"><fmt:message key="label.pays"/>&nbsp;:</td>
	<td id="for_pays" style="display:none;">
		<form:input path="libPays" id="libPays" size="25" />
		<form:errors path="libPays" cssClass="error" />
		<form:hidden path="numeroForFiscalPays" />
		<script>
			$(function() {
				autocomplete_infra('pays', '#libPays', function(item) {
					$('#numeroForFiscalPays').val(item ? item.id1 : null);
					<c:if test="${param['onChange'] != null}">
					${param['onChange']}();
					</c:if>
				});
			});
		</script>
	</td>
	<script type="text/javascript">
		selectForFiscal('${command.typeAutoriteFiscale}');
	</script>
</tr>