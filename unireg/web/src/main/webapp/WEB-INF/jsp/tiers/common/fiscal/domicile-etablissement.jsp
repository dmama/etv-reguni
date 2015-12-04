<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<display:table
		name="command.domicilesEtablissement" id="domicile"
		requestURI="visu.do"
		class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

	<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
		<unireg:regdate regdate="${domicile.dateDebut}"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
		<unireg:regdate regdate="${domicile.dateFin}"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.etablissement.domicile">
		<c:choose>
			<c:when test="${domicile.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
				<unireg:commune ofs="${domicile.numeroOfsAutoriteFiscale}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${domicile.dateDebut}"/>
			</c:when>
			<c:when test="${domicile.typeAutoriteFiscale == 'COMMUNE_HC' }">
				<unireg:commune ofs="${domicile.numeroOfsAutoriteFiscale}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${domicile.dateDebut}"/>
			</c:when>
			<c:when test="${domicile.typeAutoriteFiscale == 'PAYS_HS' }">
				<unireg:pays ofs="${domicile.numeroOfsAutoriteFiscale}" displayProperty="nomCourt" titleProperty="noOFS" date="${domicile.dateDebut}"/>
			</c:when>
		</c:choose>
	</display:column>

	<display:column style="action">
		<unireg:consulterLog entityNature="DomicileEtablissement" entityId="${domicile.id}"/>
	</display:column>

	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>

</display:table>
