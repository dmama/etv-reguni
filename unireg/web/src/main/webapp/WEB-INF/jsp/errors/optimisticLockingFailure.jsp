<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Modification concurrente détectée</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>
		
		<p>Un autre utilisateur a modifié le tiers dans la base de données
		en même temps que vous.</p>
		<p>Cette erreur survient lorsque un tiers est modifié par deux
		utilisateurs en même temps. Le premier utilisateur qui termine la
		modification peut sauver les données en base. Mais le second
		utilisateur devra recommencer son opération pour tenir compte des
		modifications apportées par le premier. Cette règle est nécessaire
		pour garantir la cohérence des données.</p>
		<h3>Désolé, veuillez recommencer l'opération depuis le début.</h3>

		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si l'erreur persiste, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>

	</tiles:put>
</tiles:insert>