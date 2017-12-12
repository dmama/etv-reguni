<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.surcharge.commune.faitieres" />
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

		<%--@elvariable id="pageSize" type="java.lang.Long"--%>
		<%--@elvariable id="count" type="java.lang.Long"--%>
		<display:table name="situations" id="situation" class="display_table" pagesize="${pageSize}" size="${count}" sort="external" partialList="true"
		               requestURI="/registrefoncier/situation/surcharge/list.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucune.situation.trouve"/></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.une.situation.trouve"/></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.situations.trouvees"/></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.situations.trouvees"/></span></display:setProperty>

			<display:column titleKey="label.id.situation.rf" sortable="true" sortName="id">
				<unireg:linkTo name="${situation.id}" action="${situation.action}"/>
				<input type="hidden" name="ids" value="${situation.id}"/>
			</display:column>
			<display:column titleKey="label.date.debut" sortable="true" sortName="dateDebut">
				<unireg:regdate regdate="${situation.dateDebut}"/>
			</display:column>
			<display:column titleKey="label.date.fin" sortable="true" sortName="dateFin">
				<unireg:regdate regdate="${situation.dateFin}"/>
			</display:column>
			<display:column titleKey="label.parcelle" sortable="true" sortName="noParcelle" property="noParcelle"/>
			<display:column titleKey="label.indexes" property="indexes"/>
			<display:column titleKey="label.commune.faitiere.rf" sortable="true" sortName="commune.nomRf">
				<c:out value="${situation.communeRF.nom}"/>
			</display:column>
			<display:column titleKey="label.egrid" property="immeuble.egrid"/>
			<display:column titleKey="label.id.rf" property="immeuble.idRF"/>
		</display:table>

	</tiles:put>
</tiles:insert>
