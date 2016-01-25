<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.VD"/></span></legend>

	<c:if test="${empty command.regimesFiscauxVD}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.regimesFiscauxVD}">
	
		<display:table name="${command.regimesFiscauxVD}" id="regimesVD" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${regimesVD.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${regimesVD.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				${regimesVD.type}
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesVD.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.CH"/></span></legend>

	<c:if test="${empty command.regimesFiscauxCH}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.regimesFiscauxCH}">
	
		<display:table name="${command.regimesFiscauxCH}" id="regimesCH" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${regimesCH.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${regimesCH.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				${regimesCH.type}
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="RegimeFiscal" entityId="${regimesCH.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
