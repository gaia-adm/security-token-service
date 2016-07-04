<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <link href="${pageContext.request.contextPath}/css/main.css" rel="stylesheet" >
    <title>Welcome to Gaia</title>
</head>
<body>
<% System.out.println("Server for further redirection: " + request.getServerName()); %>
<div id=title><p><h1>Welcome to Gaia</h1></p></div>
<p class="emptyParagraph"/>
<div id=subtitle><p><h2>What would you like to do next?</h2></p></div>
<p class="emptyParagraph"/>
<p class="emptyParagraph"/>
<p class="emptyParagraph"/>
<table id=content>
<tr>
<td>
<a href="https://gaia-local.skydns.local">
  <img src="${pageContext.request.contextPath}/images/iconUI.png" alt="Open UI" class="imageSelector"></a>
</td>
<td class="emptyCell"/>
<td>
<a href="http://gaia-local.skydns.local:4000/gau">
  <img src="${pageContext.request.contextPath}/images/iconConfig.png" alt="Open Admin UI" class="imageSelector"></a>
</a>
<td>
</tr>
<tr>
<td><div class=explainingText>Open Gaia UI</div></td>
<td class="emptyCell"/>
<td><div class=explainingText>Open Gaia Admin UI</div></td>
</tr>
</table>
</p>

<!--
<p>
<a href="https://gaia-local.skydns.local">
  <img src="${pageContext.request.contextPath}/images/iconUI.png" alt="Open UI" style="width:128px;height:128px;border:0;"> Open Gaia UI
</a>
</p>
<p>
<a href="http://gaia-local.skydns.local:4000/gau">
  <img src="${pageContext.request.contextPath}/images/iconConfig.png" alt="Open Admin UI" style="width:128px;height:128px;border:0;"> Open Gaia Admin UI
</a>
</p>
-->


<!--
You are in!

<a href="${pageContext.request.contextPath}/verify">Verify yourself, if you want</a>
<h3>What would you like to do next?</h3>
<ul>
    <li><a href="http://google.com">google.com</a></li>
    <li><a href="http://gaia-local.skydns.local">See your data in Kibana</a></li>
    <li>Do something administrative - not implemented yet</li>
    <li><a href="${pageContext.request.contextPath}/logout">Logout</a></li>
</ul>
-->

</body>
</html>
