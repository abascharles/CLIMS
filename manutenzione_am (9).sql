-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Creato il: Mag 19, 2025 alle 21:54
-- Versione del server: 10.4.32-MariaDB
-- Versione PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `manutenzione am`
--
CREATE DATABASE IF NOT EXISTS `manutenzione_am` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `manutenzione_am`;

-- --------------------------------------------------------

--
-- Struttura della tabella `anagrafica_carichi`
--

CREATE TABLE `anagrafica_carichi` (
  `PartNumber` varchar(50) NOT NULL,
  `Nomenclatura` varchar(100) DEFAULT NULL,
  `CodiceDitta` varchar(50) DEFAULT NULL,
  `Massa` decimal(10,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `anagrafica_carichi`
--

INSERT INTO `anagrafica_carichi` (`PartNumber`, `Nomenclatura`, `CodiceDitta`, `Massa`) VALUES
('0000', 'andrrea', 'LAD', 100.00),
('123', 'missile prova', 'LAD', 150.00),
('ADA', 'WP', 'LAD', 1500.00);

-- --------------------------------------------------------

--
-- Struttura della tabella `anagrafica_lanciatore`
--

CREATE TABLE `anagrafica_lanciatore` (
  `PartNumber` varchar(50) NOT NULL,
  `Nomenclatura` varchar(100) DEFAULT NULL,
  `CodiceDitta` varchar(50) DEFAULT NULL,
  `OreVitaOperativa` decimal(10,2) DEFAULT NULL,
  `ID_materiale` varchar(20) DEFAULT NULL,
  `ID_Geometria` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `anagrafica_lanciatore`
--

INSERT INTO `anagrafica_lanciatore` (`PartNumber`, `Nomenclatura`, `CodiceDitta`, `OreVitaOperativa`, `ID_materiale`, `ID_Geometria`) VALUES
('0000', 'lanciatore prova', 'LAD', 1500.00, NULL, NULL),
('1111', 'andrea', 'lad', 1500.00, NULL, NULL),
('DAD', 'LRDAD', 'LAD', 1500.00, NULL, NULL);

-- --------------------------------------------------------

--
-- Struttura della tabella `bgeometria_lanciatore`
--

CREATE TABLE `bgeometria_lanciatore` (
  `ID_Geometria` varchar(20) NOT NULL,
  `Descrizione` varchar(100) DEFAULT NULL,
  `Lunghezza_mm` float DEFAULT NULL COMMENT 'lunghezza totale del pylon',
  `Altezza_mm` float DEFAULT NULL COMMENT 'altezza sezione trasversale',
  `Larghezza_mm` float DEFAULT NULL COMMENT 'larghezza sezione',
  `Spessore_mm` float DEFAULT NULL COMMENT 'spessore delle pareti. struttura scatolata',
  `y` float DEFAULT NULL COMMENT 'metà dell altezza della sezione distanza asse neutro',
  `I` float DEFAULT NULL COMMENT 'momento inerzia',
  `d` float DEFAULT NULL COMMENT 'punto applicazione carico missile'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struttura della tabella `bmateriale_lanciatore`
--

CREATE TABLE `bmateriale_lanciatore` (
  `ID_Materiale` varchar(20) NOT NULL,
  `Nome` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struttura della tabella `bproprieta_materiale`
--

CREATE TABLE `bproprieta_materiale` (
  `ID_Materiale` varchar(20) NOT NULL,
  `Sigma_snervamento` float DEFAULT NULL COMMENT 'tensione snervamento Mpa',
  `Sigma_rottura` float DEFAULT NULL COMMENT 'tensione a rottura Mpa',
  `Sigma_fatica_limite` float DEFAULT NULL COMMENT 'limite fatica r-1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struttura della tabella `dichiarazione_missile_gui`
--

CREATE TABLE `dichiarazione_missile_gui` (
  `ID` int(11) NOT NULL,
  `ID_Missione` int(11) NOT NULL,
  `PosizioneVelivolo` varchar(20) NOT NULL,
  `Missile_Sparato` enum('SI','NO') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `dichiarazione_missile_gui`
--

INSERT INTO `dichiarazione_missile_gui` (`ID`, `ID_Missione`, `PosizioneVelivolo`, `Missile_Sparato`) VALUES
(5, 107, 'P1', 'SI'),
(6, 104, 'P2', 'SI'),
(7, 104, 'P3', 'SI');

-- --------------------------------------------------------

--
-- Struttura della tabella `matricola_velivolo`
--

CREATE TABLE `matricola_velivolo` (
  `MatricolaVelivolo` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `matricola_velivolo`
--

INSERT INTO `matricola_velivolo` (`MatricolaVelivolo`) VALUES
('100'),
('M91');

-- --------------------------------------------------------

--
-- Struttura della tabella `missione`
--

CREATE TABLE `missione` (
  `ID` int(11) NOT NULL,
  `MatricolaVelivolo` varchar(50) DEFAULT NULL,
  `DataMissione` date DEFAULT NULL,
  `NumeroVolo` int(11) DEFAULT NULL,
  `OraPartenza` time DEFAULT NULL,
  `OraArrivo` time DEFAULT NULL,
  `PartNumberLanciatoreP1` varchar(50) DEFAULT NULL,
  `PartNumberMissileP1` varchar(50) DEFAULT NULL,
  `PartNumberLanciatoreP13` varchar(50) DEFAULT NULL,
  `PartNumberMissileP13` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `missione`
--

INSERT INTO `missione` (`ID`, `MatricolaVelivolo`, `DataMissione`, `NumeroVolo`, `OraPartenza`, `OraArrivo`, `PartNumberLanciatoreP1`, `PartNumberMissileP1`, `PartNumberLanciatoreP13`, `PartNumberMissileP13`) VALUES
(104, '100', '2025-05-19', 300, '20:00:00', '21:00:00', '0000', '123', '1111', '0000'),
(105, '100', '2025-05-19', 500, '20:10:00', '21:10:00', '0000', '123', '1111', '0000'),
(107, 'M91', '2025-05-19', 1, '20:10:00', '21:10:00', 'DAD', 'ADA', NULL, NULL);

--
-- Trigger `missione`
--
DELIMITER $$
CREATE TRIGGER `trg_dichiarazione_missili` AFTER INSERT ON `missione` FOR EACH ROW BEGIN
  -- Se P1 ha un missile
  IF NEW.PartNumberMissileP1 IS NOT NULL THEN
    INSERT INTO dichiarazione_missile_gui (
      ID_Missione,
      PosizioneVelivolo,
      Missile_Sparato
    ) VALUES (
      NEW.ID,
      'P1',
      'NO'
    );
  END IF;

  -- Se P13 ha un missile
  IF NEW.PartNumberMissileP13 IS NOT NULL THEN
    INSERT INTO dichiarazione_missile_gui (
      ID_Missione,
      PosizioneVelivolo,
      Missile_Sparato
    ) VALUES (
      NEW.ID,
      'P13',
      'NO'
    );
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_insert_storici_da_missione` AFTER INSERT ON `missione` FOR EACH ROW BEGIN
  -- Missile P1
  IF NEW.PartNumberMissileP1 IS NOT NULL THEN
    INSERT INTO storico_carico (
      MatricolaVelivolo,
      PartNumber,
      DataImbarco,
      PosizioneVelivolo
    ) VALUES (
      NEW.MatricolaVelivolo,
      NEW.PartNumberMissileP1,
      NEW.DataMissione,
      'P1'  -- <- Valore della posizione, non nome colonna
    );
  END IF;

  -- Lanciatore P1
  IF NEW.PartNumberLanciatoreP1 IS NOT NULL THEN
    INSERT INTO storico_lanciatore (
      MatricolaVelivolo,
      PartNumber,
      DataInstallazione,
      PosizioneVelivolo
    ) VALUES (
      NEW.MatricolaVelivolo,
      NEW.PartNumberLanciatoreP1,
      NEW.DataMissione,
      'P1'
    );
  END IF;

  -- Missile P13
  IF NEW.PartNumberMissileP13 IS NOT NULL THEN
    INSERT INTO storico_carico (
      MatricolaVelivolo,
      PartNumber,
      DataImbarco,
      PosizioneVelivolo
    ) VALUES (
      NEW.MatricolaVelivolo,
      NEW.PartNumberMissileP13,
      NEW.DataMissione,
      'P13'
    );
  END IF;

  -- Lanciatore P13
  IF NEW.PartNumberLanciatoreP13 IS NOT NULL THEN
    INSERT INTO storico_lanciatore (
      MatricolaVelivolo,
      PartNumber,
      DataInstallazione,
      PosizioneVelivolo
    ) VALUES (
      NEW.MatricolaVelivolo,
      NEW.PartNumberLanciatoreP13,
      NEW.DataMissione,
      'P13'
    );
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_popola_missione_posizione` AFTER INSERT ON `missione` FOR EACH ROW BEGIN
    DECLARE pos VARCHAR(20);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR
        SELECT PosizioneVelivolo FROM Storico_Lanciatore
        WHERE MatricolaVelivolo = NEW.MatricolaVelivolo
          AND DataInstallazione <= NEW.DataMissione
          AND (DataRimozione IS NULL OR DataRimozione >= NEW.DataMissione);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO pos;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO Missione_Posizione_Automatica (ID_Missione, PosizioneVelivolo)
        VALUES (NEW.ID, pos);
    END LOOP;
    CLOSE cur;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Struttura della tabella `posizione_velivolo`
--

CREATE TABLE `posizione_velivolo` (
  `PosizioneVelivolo` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `posizione_velivolo`
--

INSERT INTO `posizione_velivolo` (`PosizioneVelivolo`) VALUES
('P1'),
('P13'),
('P2'),
('P3'),
('P4');

-- --------------------------------------------------------

--
-- Struttura della tabella `snapshot_missione`
--

CREATE TABLE `snapshot_missione` (
  `ID_Snapshot` int(11) NOT NULL,
  `ID_Missione` int(11) DEFAULT NULL,
  `Timestamp` float DEFAULT NULL,
  `AltitudineMin` int(11) DEFAULT NULL,
  `AltitudineMax` int(11) DEFAULT NULL,
  `Gmin` float DEFAULT NULL,
  `Gmax` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struttura della tabella `storico_carico`
--

CREATE TABLE `storico_carico` (
  `ID` int(11) NOT NULL,
  `PartNumber` varchar(50) DEFAULT NULL,
  `DataImbarco` date DEFAULT NULL,
  `DataSbarco` date DEFAULT NULL,
  `PosizioneVelivolo` varchar(20) DEFAULT NULL,
  `MatricolaVelivolo` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `storico_carico`
--

INSERT INTO `storico_carico` (`ID`, `PartNumber`, `DataImbarco`, `DataSbarco`, `PosizioneVelivolo`, `MatricolaVelivolo`) VALUES
(12, '123', '2025-05-19', NULL, 'P1', '100'),
(13, '0000', '2025-05-19', NULL, 'P13', '100'),
(14, 'ADA', '2025-05-19', NULL, 'P1', 'M91');

--
-- Trigger `storico_carico`
--
DELIMITER $$
CREATE TRIGGER `trg_check_sbarco_superiore_imbarco` BEFORE INSERT ON `storico_carico` FOR EACH ROW BEGIN
    IF NEW.DataSbarco IS NOT NULL AND NEW.DataSbarco < NEW.DataImbarco THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La DataSbarco non può essere precedente alla DataImbarco.';
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Struttura della tabella `storico_lanciatore`
--

CREATE TABLE `storico_lanciatore` (
  `ID` int(11) NOT NULL,
  `MatricolaVelivolo` varchar(50) DEFAULT NULL,
  `PartNumber` varchar(50) DEFAULT NULL,
  `DataInstallazione` date DEFAULT NULL,
  `DataRimozione` date DEFAULT NULL,
  `PosizioneVelivolo` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `storico_lanciatore`
--

INSERT INTO `storico_lanciatore` (`ID`, `MatricolaVelivolo`, `PartNumber`, `DataInstallazione`, `DataRimozione`, `PosizioneVelivolo`) VALUES
(10, '100', '0000', '2025-05-19', NULL, 'P1'),
(11, '100', '1111', '2025-05-19', NULL, 'P13'),
(12, 'M91', 'DAD', '2025-05-19', NULL, 'P1');

-- --------------------------------------------------------

--
-- Struttura della tabella `utenti`
--

CREATE TABLE `utenti` (
  `id` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dump dei dati per la tabella `utenti`
--

INSERT INTO `utenti` (`id`, `username`, `password`) VALUES
(1, 'root', '0000'),
(2, 'andrea', '1'),
(3, 'roberto', '1'),
(4, 'boba', '1'),
(5, 'enza', '1');

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `views_material_handling`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `views_material_handling` (
`PartNumber` varchar(50)
,`Nomenclatura` varchar(100)
,`DataInstallazione` date
,`DataRimozione` date
,`PosizioneVelivolo` varchar(20)
,`MatricolaVelivolo` varchar(50)
,`TipoComponente` varchar(10)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `view_aircraft_list`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `view_aircraft_list` (
`MatricolaVelivolo` varchar(50)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `view_launcher_list`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `view_launcher_list` (
`Nomenclatura` varchar(100)
,`PartNumber` varchar(50)
,`CodiceDitta` varchar(50)
,`OreVitaOperativa` decimal(10,2)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `view_weapon_list`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `view_weapon_list` (
`PartNumber` varchar(50)
,`Nomenclatura` varchar(100)
,`CodiceDitta` varchar(50)
,`Massa` decimal(10,2)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_fatigue_monitoring`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_fatigue_monitoring` (
`ID_Missione` int(11)
,`Timestamp` float
,`Gmin` float
,`Gmax` float
,`Gmedia` double
,`Massa_Kg` decimal(10,2)
,`d` float
,`y` float
,`I` float
,`Forza_N` double
,`Sigma_a` double
,`N_cicli` double
,`Danno_snapshot` double
,`PosizioneVelivolo` varchar(20)
,`Lanciatore_ID` varchar(50)
,`Missile_ID` varchar(50)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_fatigue_snapshot`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_fatigue_snapshot` (
`ID_Missione` int(11)
,`Timestamp` float
,`Gmin` float
,`Gmax` float
,`Gmedia` double
,`Massa_Kg` decimal(10,2)
,`d` float
,`y` float
,`I` float
,`Forza_N` double
,`Sigma_a` double
,`N_cicli` double
,`Danno_snapshot` double
,`Lanciatore_PartNumber` varchar(50)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_gui_missione`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_gui_missione` (
`ID_Missione` int(11)
,`MatricolaVelivolo` varchar(50)
,`DataMissione` date
,`NumeroVolo` int(11)
,`OraPartenza` time
,`OraArrivo` time
,`GloadMin` double(19,2)
,`GloadMax` double(19,2)
,`QuotaMedia` decimal(12,0)
,`PosizioniSparo` mediumtext
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_lanciatore_statistiche`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_lanciatore_statistiche` (
`PartNumber` varchar(50)
,`Nomenclatura` varchar(100)
,`NumeroMissioni` bigint(21)
,`NumeroSpari` bigint(21)
,`OreTotali` decimal(41,2)
,`VitaResiduaPercentuale` double(19,2)
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_stato_missili_missione`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_stato_missili_missione` (
);

-- --------------------------------------------------------

--
-- Struttura stand-in per le viste `vista_stato_vita_lanciatore`
-- (Vedi sotto per la vista effettiva)
--
CREATE TABLE `vista_stato_vita_lanciatore` (
);

-- --------------------------------------------------------

--
-- Struttura per vista `views_material_handling`
--
DROP TABLE IF EXISTS `views_material_handling`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `views_material_handling`  AS SELECT `sc`.`PartNumber` AS `PartNumber`, `ac`.`Nomenclatura` AS `Nomenclatura`, `sc`.`DataImbarco` AS `DataInstallazione`, `sc`.`DataSbarco` AS `DataRimozione`, `sc`.`PosizioneVelivolo` AS `PosizioneVelivolo`, `sc`.`MatricolaVelivolo` AS `MatricolaVelivolo`, 'Carico' AS `TipoComponente` FROM (`storico_carico` `sc` join `anagrafica_carichi` `ac` on(`sc`.`PartNumber` = `ac`.`PartNumber`))union all select `sl`.`PartNumber` AS `PartNumber`,`al`.`Nomenclatura` AS `Nomenclatura`,`sl`.`DataInstallazione` AS `DataInstallazione`,`sl`.`DataRimozione` AS `DataRimozione`,`sl`.`PosizioneVelivolo` AS `PosizioneVelivolo`,`sl`.`MatricolaVelivolo` AS `MatricolaVelivolo`,'Lanciatore' AS `TipoComponente` from (`storico_lanciatore` `sl` join `anagrafica_lanciatore` `al` on(`sl`.`PartNumber` = `al`.`PartNumber`))  ;

-- --------------------------------------------------------

--
-- Struttura per vista `view_aircraft_list`
--
DROP TABLE IF EXISTS `view_aircraft_list`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `view_aircraft_list`  AS SELECT `matricola_velivolo`.`MatricolaVelivolo` AS `MatricolaVelivolo` FROM `matricola_velivolo` ;

-- --------------------------------------------------------

--
-- Struttura per vista `view_launcher_list`
--
DROP TABLE IF EXISTS `view_launcher_list`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `view_launcher_list`  AS SELECT `anagrafica_lanciatore`.`Nomenclatura` AS `Nomenclatura`, `anagrafica_lanciatore`.`PartNumber` AS `PartNumber`, `anagrafica_lanciatore`.`CodiceDitta` AS `CodiceDitta`, `anagrafica_lanciatore`.`OreVitaOperativa` AS `OreVitaOperativa` FROM `anagrafica_lanciatore` ;

-- --------------------------------------------------------

--
-- Struttura per vista `view_weapon_list`
--
DROP TABLE IF EXISTS `view_weapon_list`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `view_weapon_list`  AS SELECT `anagrafica_carichi`.`PartNumber` AS `PartNumber`, `anagrafica_carichi`.`Nomenclatura` AS `Nomenclatura`, `anagrafica_carichi`.`CodiceDitta` AS `CodiceDitta`, `anagrafica_carichi`.`Massa` AS `Massa` FROM `anagrafica_carichi` ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_fatigue_monitoring`
--
DROP TABLE IF EXISTS `vista_fatigue_monitoring`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_fatigue_monitoring`  AS SELECT `sm`.`ID_Missione` AS `ID_Missione`, `sm`.`Timestamp` AS `Timestamp`, `sm`.`Gmin` AS `Gmin`, `sm`.`Gmax` AS `Gmax`, (`sm`.`Gmin` + `sm`.`Gmax`) / 2 AS `Gmedia`, `ac`.`Massa` AS `Massa_Kg`, `bg`.`d` AS `d`, `bg`.`y` AS `y`, `bg`.`I` AS `I`, `ac`.`Massa`* ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 AS `Forza_N`, `ac`.`Massa`* ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I` AS `Sigma_a`, pow(10,19 - 4.3 * log10(`ac`.`Massa` * ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I`)) AS `N_cicli`, 1 / pow(10,19 - 4.3 * log10(`ac`.`Massa` * ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I`)) AS `Danno_snapshot`, `dl`.`PosizioneVelivolo` AS `PosizioneVelivolo`, `al`.`PartNumber` AS `Lanciatore_ID`, `ac`.`PartNumber` AS `Missile_ID` FROM (((((((`snapshot_missione` `sm` join `missione` `m` on(`sm`.`ID_Missione` = `m`.`ID`)) join `dichiarazione_missile_gui` `dl` on(`dl`.`ID_Missione` = `m`.`ID`)) join `storico_lanciatore` `sl` on(`sl`.`MatricolaVelivolo` = `m`.`MatricolaVelivolo` and `sl`.`PosizioneVelivolo` = `dl`.`PosizioneVelivolo` and `m`.`DataMissione` between `sl`.`DataInstallazione` and ifnull(`sl`.`DataRimozione`,curdate()))) join `anagrafica_lanciatore` `al` on(`al`.`PartNumber` = `sl`.`PartNumber`)) join `bgeometria_lanciatore` `bg` on(`bg`.`ID_Geometria` = `al`.`ID_Geometria`)) join `storico_carico` `sc` on(`sc`.`PosizioneVelivolo` = `dl`.`PosizioneVelivolo` and `m`.`DataMissione` between `sc`.`DataImbarco` and ifnull(`sc`.`DataSbarco`,curdate()))) join `anagrafica_carichi` `ac` on(`ac`.`PartNumber` = `sc`.`PartNumber`)) ORDER BY `sm`.`Timestamp` ASC ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_fatigue_snapshot`
--
DROP TABLE IF EXISTS `vista_fatigue_snapshot`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_fatigue_snapshot`  AS SELECT `sm`.`ID_Missione` AS `ID_Missione`, `sm`.`Timestamp` AS `Timestamp`, `sm`.`Gmin` AS `Gmin`, `sm`.`Gmax` AS `Gmax`, (`sm`.`Gmin` + `sm`.`Gmax`) / 2 AS `Gmedia`, `ac`.`Massa` AS `Massa_Kg`, `bg`.`d` AS `d`, `bg`.`y` AS `y`, `bg`.`I` AS `I`, `ac`.`Massa`* ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 AS `Forza_N`, `ac`.`Massa`* ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I` AS `Sigma_a`, pow(10,19 - 4.3 * log10(`ac`.`Massa` * ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I`)) AS `N_cicli`, 1 / pow(10,19 - 4.3 * log10(`ac`.`Massa` * ((`sm`.`Gmin` + `sm`.`Gmax`) / 2) * 9.81 * (`bg`.`d` / 1000) * (`bg`.`y` / 1000) / `bg`.`I`)) AS `Danno_snapshot`, `al`.`PartNumber` AS `Lanciatore_PartNumber` FROM ((((((`snapshot_missione` `sm` join `missione` `m` on(`sm`.`ID_Missione` = `m`.`ID`)) join `storico_lanciatore` `sl` on(`sl`.`MatricolaVelivolo` = `m`.`MatricolaVelivolo` and `m`.`DataMissione` between `sl`.`DataInstallazione` and ifnull(`sl`.`DataRimozione`,curdate()))) join `anagrafica_lanciatore` `al` on(`sl`.`PartNumber` = `al`.`PartNumber`)) join `bgeometria_lanciatore` `bg` on(`al`.`ID_Geometria` = `bg`.`ID_Geometria`)) join `storico_carico` `sc` on(`sc`.`PosizioneVelivolo` = `sl`.`PosizioneVelivolo` and `m`.`DataMissione` between `sc`.`DataImbarco` and ifnull(`sc`.`DataSbarco`,curdate()))) join `anagrafica_carichi` `ac` on(`sc`.`PartNumber` = `ac`.`PartNumber`)) ORDER BY `sm`.`Timestamp` ASC ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_gui_missione`
--
DROP TABLE IF EXISTS `vista_gui_missione`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_gui_missione`  AS SELECT `m`.`ID` AS `ID_Missione`, `m`.`MatricolaVelivolo` AS `MatricolaVelivolo`, `m`.`DataMissione` AS `DataMissione`, `m`.`NumeroVolo` AS `NumeroVolo`, `m`.`OraPartenza` AS `OraPartenza`, `m`.`OraArrivo` AS `OraArrivo`, round(avg(`sm`.`Gmin`),2) AS `GloadMin`, round(avg(`sm`.`Gmax`),2) AS `GloadMax`, round(avg((`sm`.`AltitudineMin` + `sm`.`AltitudineMax`) / 2),0) AS `QuotaMedia`, (select group_concat(`d`.`PosizioneVelivolo` order by `d`.`PosizioneVelivolo` ASC separator ', ') from `dichiarazione_missile_gui` `d` where `d`.`ID_Missione` = `m`.`ID` and `d`.`Missile_Sparato` = 'SI') AS `PosizioniSparo` FROM (`missione` `m` left join `snapshot_missione` `sm` on(`sm`.`ID_Missione` = `m`.`ID`)) GROUP BY `m`.`ID`, `m`.`MatricolaVelivolo`, `m`.`DataMissione`, `m`.`NumeroVolo`, `m`.`OraPartenza`, `m`.`OraArrivo` ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_lanciatore_statistiche`
--
DROP TABLE IF EXISTS `vista_lanciatore_statistiche`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_lanciatore_statistiche`  AS SELECT `al`.`PartNumber` AS `PartNumber`, `al`.`Nomenclatura` AS `Nomenclatura`, count(distinct `m`.`ID`) AS `NumeroMissioni`, count(distinct case when `dmg`.`Missile_Sparato` = 'SI' then concat(`m`.`ID`,'-',`dmg`.`PosizioneVelivolo`) end) AS `NumeroSpari`, round(sum(time_to_sec(timediff(`m`.`OraArrivo`,`m`.`OraPartenza`)) / 3600),2) AS `OreTotali`, round(greatest(0,(1 - coalesce(sum(`vfm`.`Danno_snapshot`),0)) * 100),2) AS `VitaResiduaPercentuale` FROM ((((`anagrafica_lanciatore` `al` left join `storico_lanciatore` `sl` on(`sl`.`PartNumber` = `al`.`PartNumber`)) left join `missione` `m` on(`m`.`MatricolaVelivolo` = `sl`.`MatricolaVelivolo` and `m`.`DataMissione` >= `sl`.`DataInstallazione` and (`sl`.`DataRimozione` is null or `m`.`DataMissione` <= `sl`.`DataRimozione`))) left join `dichiarazione_missile_gui` `dmg` on(`dmg`.`ID_Missione` = `m`.`ID` and `dmg`.`PosizioneVelivolo` = `sl`.`PosizioneVelivolo`)) left join `vista_fatigue_monitoring` `vfm` on(`vfm`.`Lanciatore_ID` = `al`.`PartNumber`)) GROUP BY `al`.`PartNumber`, `al`.`Nomenclatura` ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_stato_missili_missione`
--
DROP TABLE IF EXISTS `vista_stato_missili_missione`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_stato_missili_missione`  AS SELECT `smm`.`ID_Missione` AS `ID_Missione`, `m`.`MatricolaVelivolo` AS `MatricolaVelivolo`, `m`.`NumeroVolo` AS `NumeroVolo`, `m`.`DataMissione` AS `DataMissione`, `smm`.`PosizioneVelivolo` AS `PosizioneVelivolo`, `smm`.`PartNumber` AS `Missile_PartNumber`, `smm`.`Nomenclatura` AS `Missile_Nomenclatura`, `smm`.`Stato` AS `Stato_Missile`, `smm`.`Lanciatore_PartNumber` AS `Lanciatore_PartNumber`, `smm`.`Lanciatore_SerialNumber` AS `Lanciatore_SerialNumber` FROM (`stato_missili_missione` `smm` join `missione` `m` on(`smm`.`ID_Missione` = `m`.`ID`)) ORDER BY `m`.`DataMissione` ASC, `smm`.`PosizioneVelivolo` ASC ;

-- --------------------------------------------------------

--
-- Struttura per vista `vista_stato_vita_lanciatore`
--
DROP TABLE IF EXISTS `vista_stato_vita_lanciatore`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_stato_vita_lanciatore`  AS SELECT `dlm`.`Nome_Lanciatore` AS `Nome_Lanciatore`, `dlm`.`Lanciatore_PartNumber` AS `Lanciatore_PartNumber`, `dlm`.`Lanciatore_SerialNumber` AS `Lanciatore_SerialNumber`, count(distinct `dlm`.`ID_Missione`) AS `Numero_Missioni`, sum(case when `dlm`.`Numero_Missili_Sparati` > 0 then 1 else 0 end) AS `Missioni_con_Sparo`, sum(case when `dlm`.`Numero_Missili_Sparati` = 0 then 1 else 0 end) AS `Missioni_senza_Sparo`, round(sum(`dlm`.`Ore_di_Volo`),2) AS `Ore_di_Volo_Totali`, round(100 * (1 - sum(`dlm`.`Danno_Miner_Missione`)),2) AS `Vita_Residua_Percentuale` FROM `vista_degrado_lanciatore_missione` AS `dlm` GROUP BY `dlm`.`Nome_Lanciatore`, `dlm`.`Lanciatore_PartNumber`, `dlm`.`Lanciatore_SerialNumber` ORDER BY round(100 * (1 - sum(`dlm`.`Danno_Miner_Missione`)),2) ASC ;

--
-- Indici per le tabelle scaricate
--

--
-- Indici per le tabelle `anagrafica_carichi`
--
ALTER TABLE `anagrafica_carichi`
  ADD PRIMARY KEY (`PartNumber`);

--
-- Indici per le tabelle `anagrafica_lanciatore`
--
ALTER TABLE `anagrafica_lanciatore`
  ADD PRIMARY KEY (`PartNumber`),
  ADD KEY `FK_Lanciatore_Materiale` (`ID_materiale`),
  ADD KEY `FK_Lanciatore_Geometria` (`ID_Geometria`);

--
-- Indici per le tabelle `bgeometria_lanciatore`
--
ALTER TABLE `bgeometria_lanciatore`
  ADD PRIMARY KEY (`ID_Geometria`);

--
-- Indici per le tabelle `bmateriale_lanciatore`
--
ALTER TABLE `bmateriale_lanciatore`
  ADD PRIMARY KEY (`ID_Materiale`);

--
-- Indici per le tabelle `bproprieta_materiale`
--
ALTER TABLE `bproprieta_materiale`
  ADD PRIMARY KEY (`ID_Materiale`);

--
-- Indici per le tabelle `dichiarazione_missile_gui`
--
ALTER TABLE `dichiarazione_missile_gui`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `ID_Missione` (`ID_Missione`),
  ADD KEY `PosizioneVelivolo` (`PosizioneVelivolo`);

--
-- Indici per le tabelle `matricola_velivolo`
--
ALTER TABLE `matricola_velivolo`
  ADD PRIMARY KEY (`MatricolaVelivolo`);

--
-- Indici per le tabelle `missione`
--
ALTER TABLE `missione`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_Missione_MatricolaVelivolo` (`MatricolaVelivolo`),
  ADD KEY `fk_missile_p1` (`PartNumberMissileP1`),
  ADD KEY `fk_missile_p13` (`PartNumberMissileP13`),
  ADD KEY `fk_lanciatore_p1` (`PartNumberLanciatoreP1`),
  ADD KEY `fk_lanciatore_p13` (`PartNumberLanciatoreP13`);

--
-- Indici per le tabelle `posizione_velivolo`
--
ALTER TABLE `posizione_velivolo`
  ADD PRIMARY KEY (`PosizioneVelivolo`);

--
-- Indici per le tabelle `snapshot_missione`
--
ALTER TABLE `snapshot_missione`
  ADD PRIMARY KEY (`ID_Snapshot`),
  ADD KEY `FK_Snapshot_Missione` (`ID_Missione`);

--
-- Indici per le tabelle `storico_carico`
--
ALTER TABLE `storico_carico`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `PosizioneVelivolo` (`PosizioneVelivolo`),
  ADD KEY `FK_StoricoCarico_Anagrafica` (`PartNumber`);

--
-- Indici per le tabelle `storico_lanciatore`
--
ALTER TABLE `storico_lanciatore`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `PosizioneVelivolo` (`PosizioneVelivolo`),
  ADD KEY `FK_StoricoLanciatore_Anagrafica` (`PartNumber`);

--
-- Indici per le tabelle `utenti`
--
ALTER TABLE `utenti`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT per le tabelle scaricate
--

--
-- AUTO_INCREMENT per la tabella `dichiarazione_missile_gui`
--
ALTER TABLE `dichiarazione_missile_gui`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT per la tabella `missione`
--
ALTER TABLE `missione`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=108;

--
-- AUTO_INCREMENT per la tabella `snapshot_missione`
--
ALTER TABLE `snapshot_missione`
  MODIFY `ID_Snapshot` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT per la tabella `storico_carico`
--
ALTER TABLE `storico_carico`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT per la tabella `storico_lanciatore`
--
ALTER TABLE `storico_lanciatore`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT per la tabella `utenti`
--
ALTER TABLE `utenti`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- Limiti per le tabelle scaricate
--

--
-- Limiti per la tabella `anagrafica_lanciatore`
--
ALTER TABLE `anagrafica_lanciatore`
  ADD CONSTRAINT `FK_Lanciatore_Geometria` FOREIGN KEY (`ID_Geometria`) REFERENCES `bgeometria_lanciatore` (`ID_Geometria`),
  ADD CONSTRAINT `FK_Lanciatore_Materiale` FOREIGN KEY (`ID_materiale`) REFERENCES `bmateriale_lanciatore` (`ID_Materiale`);

--
-- Limiti per la tabella `dichiarazione_missile_gui`
--
ALTER TABLE `dichiarazione_missile_gui`
  ADD CONSTRAINT `dichiarazione_missile_gui_ibfk_1` FOREIGN KEY (`ID_Missione`) REFERENCES `missione` (`ID`),
  ADD CONSTRAINT `dichiarazione_missile_gui_ibfk_2` FOREIGN KEY (`PosizioneVelivolo`) REFERENCES `posizione_velivolo` (`PosizioneVelivolo`);

--
-- Limiti per la tabella `missione`
--
ALTER TABLE `missione`
  ADD CONSTRAINT `FK_Missione_MatricolaVelivolo` FOREIGN KEY (`MatricolaVelivolo`) REFERENCES `matricola_velivolo` (`MatricolaVelivolo`),
  ADD CONSTRAINT `fk_lanciatore_p1` FOREIGN KEY (`PartNumberLanciatoreP1`) REFERENCES `anagrafica_lanciatore` (`PartNumber`),
  ADD CONSTRAINT `fk_lanciatore_p13` FOREIGN KEY (`PartNumberLanciatoreP13`) REFERENCES `anagrafica_lanciatore` (`PartNumber`),
  ADD CONSTRAINT `fk_missile_p1` FOREIGN KEY (`PartNumberMissileP1`) REFERENCES `anagrafica_carichi` (`PartNumber`),
  ADD CONSTRAINT `fk_missile_p13` FOREIGN KEY (`PartNumberMissileP13`) REFERENCES `anagrafica_carichi` (`PartNumber`);

--
-- Limiti per la tabella `snapshot_missione`
--
ALTER TABLE `snapshot_missione`
  ADD CONSTRAINT `FK_Snapshot_Missione` FOREIGN KEY (`ID_Missione`) REFERENCES `missione` (`ID`);

--
-- Limiti per la tabella `storico_carico`
--
ALTER TABLE `storico_carico`
  ADD CONSTRAINT `FK_StoricoCarico_Anagrafica` FOREIGN KEY (`PartNumber`) REFERENCES `anagrafica_carichi` (`PartNumber`),
  ADD CONSTRAINT `storico_carico_ibfk_1` FOREIGN KEY (`PosizioneVelivolo`) REFERENCES `posizione_velivolo` (`PosizioneVelivolo`);

--
-- Limiti per la tabella `storico_lanciatore`
--
ALTER TABLE `storico_lanciatore`
  ADD CONSTRAINT `FK_StoricoLanciatore_Anagrafica` FOREIGN KEY (`PartNumber`) REFERENCES `anagrafica_lanciatore` (`PartNumber`),
  ADD CONSTRAINT `storico_lanciatore_ibfk_1` FOREIGN KEY (`PosizioneVelivolo`) REFERENCES `posizione_velivolo` (`PosizioneVelivolo`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
