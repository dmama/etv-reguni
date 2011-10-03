<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

	<tiles:put name="title">
		<fmt:message key="title.rf.liste.immeubles" />
	</tiles:put>

	<tiles:put name="head">
		<style type="text/css">
			.pageheader {
				margin-top: 0px;
			}
		</style>
	</tiles:put>

	<tiles:put name="body">
		<fieldset>
			<legend><span><fmt:message key="label.liste.immeubles" /></span></legend>
			<display:table name="${immeubles}" class="display" pagesize="10">
				<display:column titleKey="label.date.debut" property="dateDebut"/>
				<display:column titleKey="label.date.fin" property="dateFin"/>
				<display:column titleKey="label.numero.immeuble" property="numero"/>
				<display:column titleKey="label.nature" property="nature"/>
				<display:column titleKey="label.estimation.fiscale" property="estimationFiscale"/>
				<display:column titleKey="label.date.estimation.fiscale" property="dateEstimationFiscale"/>
				<display:column titleKey="label.ancienne.estimation.fiscale" property="ancienneEstimationFiscale"/>
				<display:column titleKey="label.genre.propriete" property="genrePropriete"/>
				<display:column titleKey="label.part.propriete" property="partPropriete"/>
			</display:table>
		</fieldset>
	</tiles:put>

</tiles:insert>