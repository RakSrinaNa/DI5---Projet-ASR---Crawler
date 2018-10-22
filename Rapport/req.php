<html>
<head>
</head>
<body>
<div>
    Headers:
    <ul>
		<?php
			foreach($_SERVER as $h => $v)
			{
				if(preg_match('/^HTTP_(.+)/', $h, $hp))
				{
					echo "<li>$h = $v</li>";
				}
			}
		?>
    </ul>
</div>

<hr/>

<div>
    Get params:
    <ul>
		<?php
			foreach($_GET as $h => $v)
			{
				echo "<li>$h = $v</li>";
			}
		?>
    </ul>
</div>

<hr/>

<div>
    Post params:
    <ul>
		<?php
			foreach($_POST as $h => $v)
			{
				echo "<li>$h = $v</li>";
			}
		?>
    </ul>
</div>

<hr/>

<div>
    Input params:
	<?php
		echo file_get_contents("php://input");
	?>
</div>

<hr/>

<div>
    Request params:
    <ul>
		<?php
			foreach($_REQUEST as $h => $v)
			{
				echo "<li>$h = $v</li>";
			}
		?>
    </ul>
</div>
</body>
</html>
