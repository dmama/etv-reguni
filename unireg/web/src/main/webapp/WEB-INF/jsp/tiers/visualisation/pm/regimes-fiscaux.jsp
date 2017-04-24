<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.VD"/></span></legend>

	<unireg:setAuth var="auth" tiersId="${command.tiers.numero}"/>
	<c:if test="${!command.tiers.annule && auth.regimesFiscaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../regimefiscal/edit-list.do?pmId=${command.tiers.numero}&portee=VD" tooltip="Modifier les régimes fiscaux cantonaux" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.regimesFiscauxVD}">

		<input class="noprint" name="rfvd_histo" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('regimesVD','rfvd_histo', 'histo-only');" id="rfvd_histo" />
		<label class="noprint" for="rfvd_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.regimesFiscauxVD}" id="regimesVD" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
				<unireg:regdate regdate="${regimesVD.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 20%;">
				<unireg:regdate regdate="${regimesVD.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<c:out value="${regimesVD.type.libelleAvecCode}"/>
				<c:if test="${regimesVD.type.indetermine && !regimesVD.annule}">
					<div style="display: inline-block;" class="warning_icon" title="<fmt:message key='label.regime.a.determiner.VD'/>">&nbsp;</div>
				</c:if>
			</display:column>
			<display:column titleKey="label.exoneration.IBC" style="width: 10%; text-align: center;">
				<input type="checkbox" disabled="disabled" <c:if test="${regimesVD.exonerantIBC}">checked="checked"</c:if>/>
			</display:column>
			<display:column titleKey="label.exoneration.ICI" style="width: 10%; text-align: center;">
				<input type="checkbox" disabled="disabled" <c:if test="${regimesVD.exonerantICI}">checked="checked"</c:if>/>
			</display:column>
			<display:column titleKey="label.exoneration.IFONC" style="width: 10%; text-align: center;">
				<input type="checkbox" disabled="disabled" <c:if test="${regimesVD.exonerantIFONC}">checked="checked"</c:if>/>
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesVD.id}" />
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.CH"/></span></legend>

	<c:if test="${!command.tiers.annule && auth.regimesFiscaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../regimefiscal/edit-list.do?pmId=${command.tiers.numero}&portee=CH" tooltip="Modifier les régimes fiscaux fédéraux" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.regimesFiscauxCH}">

		<input class="noprint" name="rfch_histo" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('regimesCH','rfch_histo', 'histo-only');" id="rfch_histo" />
		<label class="noprint" for="rfch_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.regimesFiscauxCH}" id="regimesCH" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
				<unireg:regdate regdate="${regimesCH.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 20%;">
				<unireg:regdate regdate="${regimesCH.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<c:out value="${regimesCH.type.libelleAvecCode}"/>
				<c:if test="${regimesCH.type.indetermine && !regimesCH.annule}">
					<div style="display: inline-block;" class="warning_icon" title="<fmt:message key='label.regime.a.determiner.CH'/>">&nbsp;</div>
				</c:if>
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesCH.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	$(function() {
		Histo.toggleRowsIsHistoFromClass('regimesVD','rfvd_histo', 'histo-only');
		Histo.toggleRowsIsHistoFromClass('regimesCH','rfch_histo', 'histo-only');
	});

</script>
