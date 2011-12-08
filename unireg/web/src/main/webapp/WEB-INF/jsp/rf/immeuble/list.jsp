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
			<display:table name="${immeubles}" id="immeuble" class="display" pagesize="10">
				<display:column titleKey="label.commune">
					<c:out value="${immeuble.noCommune}"/> <c:out value="${immeuble.nomCommune}"/>
				</display:column>
				<display:column titleKey="label.numero.immeuble" property="numero"/>
				<display:column titleKey="label.type.immeuble">
					<fmt:message key="option.rf.type.immeuble.${immeuble.typeImmeuble}" />
				</display:column>
				<display:column titleKey="label.nature" property="nature"/>
				<display:column titleKey="label.genre.propriete">
					<fmt:message key="option.rf.genre.propriete.${immeuble.genrePropriete}" />
				</display:column>
				<display:column titleKey="label.part.propriete" property="partPropriete"/>
				<display:column titleKey="label.estimation.fiscale" property="estimationFiscale" class="number" decorator="ch.vd.uniregctb.utils.SwissCurrencyColumnDecorator"/>
				<display:column titleKey="label.ref.estimation.fiscale" property="referenceEstimationFiscale"/>
				<display:column titleKey="label.date.derniere.mutation" property="dateDernierMutation"/>
				<display:column titleKey="label.type.derniere.mutation">
					<fmt:message key="option.rf.type.mutation.${immeuble.derniereMutation}" />
				</display:column>
				<display:column titleKey="label.date.debut" property="dateDebut"/>
				<display:column titleKey="label.date.fin" property="dateFin"/>
				<display:column class="action" style="width:38px">
					<a href="<c:out value="${immeuble.lienRF}"/>" class="extlink" title="Lien vers le registre foncier" style="margin-right: 0.5em;" target="_blank">&nbsp;</a>
					<unireg:consulterLog entityNature="Immeuble" entityId="${immeuble.id}"/>
				</display:column>
			</display:table>
		</fieldset>
	</tiles:put>

</tiles:insert>