<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Welcome to Gaia</title>
</head>
<body>
You are in!

<a href="${pageContext.request.contextPath}/verify">Verify yourself, if you want</a>
<h3>What would you like to do next?</h3>
<ul>
    <li><a href="http://google.com">google.com</a></li>
    <li><a href="http://gaia.skydns.local">See your data in Kibana</a></li>
    <li>Do something administrative - not implemented yet</li>
    <li><a href="${pageContext.request.contextPath}/logout">Logout</a></li>
</ul>

</body>
</html>
