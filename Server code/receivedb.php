<?php

/**
This script received a database file exported by MaxieKeyboard on Android, and stores the 
contents of the file into set of MySQL tables on the remote web server. The received file
is also saved in an appropriate directory as a backup.

For the appropriate database structure, please see the MaxieDB.sql file
**/
  	error_reporting(E_ALL);
	
	
	$hostname_test = "localhost";
	$database_test = "oats";
	$username_test = "root";
	$password_test = "?????";
	
	$test = mysql_pconnect($hostname_test, $username_test, $password_test) or trigger_error(mysql_error(),E_USER_ERROR); 
	mysql_select_db($database_test, $test);
	
    $file_path = "databases/filedumps/";
      
    $file_path = $file_path.basename($_FILES['file']['name']);
    if(move_uploaded_file($_FILES['file']['tmp_name'], $file_path)) 
	{
        echo "success in moving DB file to $file_path \n";
		$db = new SQLite3($file_path);
		
		$tables = array();
		$tables['typingevents']='ps_typingevents';
		$tables['sessions']='ps_sessions';
		$tables['suspects']='ps_suspects';
		$tables['user']='ps_users';
		
		foreach($tables as $sqlitetable=>$mysqltable)
		{
			$valueString="";
			$cols="";
			$results = $db->query("Select count(*) as bob from $sqlitetable");
			$roww = $results->fetchArray(SQLITE3_ASSOC);
			if ($roww['bob']>0)
			{
				echo "About to insert $sqlitetable to $mysqltable \n";
				$results2 = $db->query("SELECT * FROM $sqlitetable");
				$row = $results2->fetchArray(SQLITE3_ASSOC);
				//first show the table columns
				$cols;
				foreach ($row as $key=>$val)
				{
					$cols.=$key.",";
				}
				$cols=rtrim($cols, ',');
				$cols = "(".$cols.")";
				
				//now start forming the values
				$values;
				$valueString;
				do
				{
					$values="";
					foreach ($row as $key=>$val)
					{
						if($val == "")
						{
							$val="NULL";
							$values.= $val.",";
						}
						else
						{												
							if($key=="keychar" || $key="user" || $key="code")
								$values.= "\"".$val."\",";
							else
								$values.= $val.",";
						}
					}
					$values=rtrim($values, ',');
					
					$valueString.= "(".$values."),";
					
				}while($row = $results2->fetchArray(SQLITE3_ASSOC));
				
				$valueString = rtrim($valueString,',');
				
				if($mysqltable=='ps_users')
					$insertSQL = "Insert ignore into $mysqltable $cols VALUES $valueString";
				else
					$insertSQL = "Insert into $mysqltable $cols VALUES $valueString";
				$Recordset1 = mysql_query($insertSQL, $test) or die(mysql_error()." - ".$insertSQL);
				
			}
			else
				echo "Nothing to insert $sqlitetable to $mysqltable \n";
		
		}
		echo "Done";
    } else
	{
        echo "fail moving DB file $file_path ";
		print_r($_FILES);
    }
 ?>