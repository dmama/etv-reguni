<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.etats"/></span></legend>

	<c:if test="${!command.depuisTache && command.allowedQuittancement && command.typeDocument.ordinaire}">
		<table id="quittancerBouton" border="0">
			<tr>
				<td>
					<unireg:linkTo name="&nbsp;Quittancer" title="Quittancer la déclaration" action="/di/etat/ajouter.do" params="{id:${command.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>

	<c:if test="${not empty command.etats}">
		<display:table name="command.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.obtention">
				<unireg:regdate regdate="${etat.dateObtention}"/>
				<c:if test="${!etat.annule && etat.etat == 'SOMMEE'}">
					&nbsp;
					(<fmt:message key="label.date.envoi.courrier">
					<fmt:param><unireg:date date="${etat.dateEnvoiCourrier}"/></fmt:param>
				</fmt:message>)
				</c:if>
			</display:column>
			<display:column titleKey="label.etat">
				<fmt:message key="option.etat.avancement.${etat.etat}"/>
			</display:column>
			<display:column titleKey="label.source">
				<c:if test="${etat.etat == 'RETOURNEE'}">
					<c:if test="${etat.source == null}">
						<fmt:message key="option.source.quittancement.UNKNOWN"/>
					</c:if>
					<c:if test="${etat.source != null}">
						<fmt:message key="option.source.quittancement.${etat.source}"/>
					</c:if>
				</c:if>
			</display:column>
			<display:column style="action">
				<c:if test="${command.depuisTache || !command.allowedQuittancement}">
					<unireg:consulterLog entityNature="EtatDeclaration" entityId="${etat.id}"/>
				</c:if>
				<c:if test="${!command.depuisTache && command.allowedQuittancement}">
					<c:if test="${!etat.annule && etat.etat == 'RETOURNEE'}">
						<unireg:linkTo name="&nbsp;" title="Annuler le quittancement" confirm="Voulez-vous vraiment annuler ce quittancement ?"
						               action="/di/etat/annuler.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
					</c:if>
				</c:if>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
	</c:if>

</fieldset>
