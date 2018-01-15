<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="depuisTache" value="${param.depuisTache}" />
<c:if test="${not empty command.delais}">

	<display:table 	name="command.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.demande">
			<unireg:regdate regdate="${delai.dateDemande}" />
		</display:column>
		<display:column titleKey="label.date.traitement">
			<unireg:regdate regdate="${delai.dateTraitement}" />
		</display:column>
		<display:column titleKey="label.decision">
			<fmt:message key="option.etat.delai.${delai.etat}"/>
		</display:column>
		<display:column titleKey="label.date.delai.accorde">
			<unireg:regdate regdate="${delai.delaiAccordeAu}" />
			<c:if test="${delai.sursis && delai.delaiAccordeAu != null}">
				(<fmt:message key="label.sursis"/>)
			</c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<unireg:consulterLog entityNature="DelaiDeclaration" entityId="${delai.id}"/>
			</c:if>
			<c:if test="${depuisTache == null && command.allowedDelai && page == 'edit'}">
				<c:if test="${delai.etat == 'DEMANDE' && !delai.annule}">
					<unireg:linkTo name="" title="Accorder/refuser le délai" action="/di/delai/editer-pm.do" params="{id:${delai.id}}" link_class="edit"/>
				</c:if>
				<c:if test="${(!delai.annule) && (!delai.first)}">
					<unireg:linkTo name="" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
					               action="/declaration/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
				</c:if>
			</c:if>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</c:if>