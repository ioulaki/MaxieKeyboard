-- phpMyAdmin SQL Dump
-- version 4.0.2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Mar 01, 2016 at 05:54 PM
-- Server version: 5.6.28-0ubuntu0.14.04.1
-- PHP Version: 5.5.9-1ubuntu4.14

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `oats`
--
CREATE DATABASE IF NOT EXISTS `oats` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `oats`;

-- --------------------------------------------------------

--
-- Table structure for table `ps_sessions`
--

CREATE TABLE IF NOT EXISTS `ps_sessions` (
  `id` int(11) NOT NULL,
  `user` varchar(100) DEFAULT NULL,
  `session_width` int(11) DEFAULT NULL,
  `session_height` int(11) DEFAULT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `app` varchar(100) DEFAULT NULL,
  `low_errors` int(11) DEFAULT NULL,
  `high_errors` int(11) DEFAULT NULL,
  `suggestions_picked` int(11) DEFAULT NULL,
  `injections` int(11) DEFAULT NULL,
  `first_word` varchar(100) DEFAULT NULL,
  `autocorrect` int(11) DEFAULT NULL,
  `sound` int(11) DEFAULT NULL,
  `haptic` int(11) DEFAULT NULL,
  `visual` int(11) DEFAULT NULL,
  `sugg_highlight` int(11) DEFAULT NULL,
  `dots` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `ps_suspects`
--

CREATE TABLE IF NOT EXISTS `ps_suspects` (
  `id` int(11) NOT NULL,
  `user` varchar(100) DEFAULT NULL,
  `sessionid` int(11) DEFAULT NULL,
  `suspect` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `ps_typingevents`
--

CREATE TABLE IF NOT EXISTS `ps_typingevents` (
  `id` int(11) NOT NULL,
  `sessionid` int(11) DEFAULT NULL,
  `user` varchar(100) DEFAULT NULL,
  `timesincelast` int(11) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `rawxdiff` double DEFAULT NULL,
  `rawydiff` double DEFAULT NULL,
  `xdiff` double DEFAULT NULL,
  `ydiff` double DEFAULT NULL,
  `keycode` int(11) DEFAULT NULL,
  `keychar` text,
  `followspace` int(11) DEFAULT NULL,
  `precedespace` int(11) DEFAULT NULL,
  `followshift` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `ps_users`
--

CREATE TABLE IF NOT EXISTS `ps_users` (
  `user` varchar(50) NOT NULL,
  `age` int(11) NOT NULL,
  `sex` int(11) NOT NULL,
  `country` int(11) NOT NULL,
  `code` varchar(50) NOT NULL,
  PRIMARY KEY (`user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
