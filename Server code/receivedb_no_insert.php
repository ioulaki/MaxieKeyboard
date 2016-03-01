<?php

/**
This script received a database file exported by MaxieKeyboard on Android, and stores the 
received file in an appropriate directory.
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
		echo "Done";
    } 
	else
	{
        echo "fail moving DB file $file_path ";
		print_r($_FILES);
    }
 ?>