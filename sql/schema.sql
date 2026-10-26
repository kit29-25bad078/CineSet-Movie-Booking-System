DROP DATABASE IF EXISTS cineset_booking;
CREATE DATABASE cineset_booking;
USE cineset_booking;

CREATE TABLE movies(
 id INT PRIMARY KEY AUTO_INCREMENT,
 title VARCHAR(150) NOT NULL,
 genre VARCHAR(100) NOT NULL,
 language VARCHAR(50) NOT NULL,
 rating DECIMAL(3,1) DEFAULT 0,
 duration_min INT DEFAULT 120,
 poster_url VARCHAR(600),
 backdrop_url VARCHAR(600),
 description VARCHAR(1200),
 release_year INT,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE theatres(
 id INT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(150) NOT NULL,
 location VARCHAR(150) NOT NULL,
 city VARCHAR(100) NOT NULL DEFAULT 'Coimbatore'
);

CREATE TABLE shows(
 id INT PRIMARY KEY AUTO_INCREMENT,
 movie_id INT NOT NULL,
 theatre_id INT NOT NULL,
 show_date DATE NOT NULL,
 show_time TIME NOT NULL,
 price DECIMAL(10,2) NOT NULL,
 FOREIGN KEY(movie_id) REFERENCES movies(id) ON DELETE CASCADE,
 FOREIGN KEY(theatre_id) REFERENCES theatres(id) ON DELETE CASCADE
);

CREATE TABLE seats(
 id INT PRIMARY KEY AUTO_INCREMENT,
 show_id INT NOT NULL,
 seat_no VARCHAR(10) NOT NULL,
 status ENUM('AVAILABLE','BOOKED') DEFAULT 'AVAILABLE',
 UNIQUE(show_id,seat_no),
 FOREIGN KEY(show_id) REFERENCES shows(id) ON DELETE CASCADE
);

CREATE TABLE users(
 id INT PRIMARY KEY AUTO_INCREMENT,
 mobile VARCHAR(20) UNIQUE NOT NULL,
 name VARCHAR(120),
 language VARCHAR(10) DEFAULT 'en',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings(
 id INT PRIMARY KEY AUTO_INCREMENT,
 booking_code VARCHAR(30) UNIQUE NOT NULL,
 user_id INT NOT NULL,
 show_id INT NOT NULL,
 amount DECIMAL(10,2) NOT NULL,
 status ENUM('CONFIRMED','CANCELLED','REFUNDED') DEFAULT 'CONFIRMED',
 payment_method VARCHAR(30),
 payment_ref VARCHAR(100),
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 cancelled_at TIMESTAMP NULL,
 FOREIGN KEY(user_id) REFERENCES users(id),
 FOREIGN KEY(show_id) REFERENCES shows(id)
);

CREATE TABLE booking_seats(
 id INT PRIMARY KEY AUTO_INCREMENT,
 booking_id INT NOT NULL,
 seat_id INT NOT NULL,
 UNIQUE(booking_id,seat_id),
 FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
 FOREIGN KEY(seat_id) REFERENCES seats(id)
);

CREATE TABLE otp_codes(
 id INT PRIMARY KEY AUTO_INCREMENT,
 mobile VARCHAR(20) NOT NULL,
 otp VARCHAR(10) NOT NULL,
 expires_at DATETIME NOT NULL,
 used BOOLEAN DEFAULT FALSE
);

CREATE TABLE payments(
 id INT PRIMARY KEY AUTO_INCREMENT,
 booking_id INT NOT NULL,
 method VARCHAR(30) NOT NULL,
 transaction_ref VARCHAR(100) NOT NULL,
 amount DECIMAL(10,2) NOT NULL,
 status VARCHAR(30) DEFAULT 'SUCCESS',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(booking_id) REFERENCES bookings(id)
);

INSERT INTO movies(title,genre,language,rating,duration_min,poster_url,backdrop_url,description,release_year) VALUES
('Vikram','Action, Thriller','Tamil',8.4,174,
'https://image.tmdb.org/t/p/original/k340hGNTfE0DYlDqXRJfMyXJtDx.jpg',
'https://image.tmdb.org/t/p/original/9n8Hk9V7g8y7m5f2mJx4nP7a6b3.jpg',
'An intense Tamil action thriller from Lokesh Kanagaraj.',2022),
('Jailer','Action, Comedy','Tamil',7.1,168,
'https://indianfilmhistory.com/admin_panel_new_dev/media/files_i/Jailer_Movie_Details_Indian_Film_History.jpg',
'https://image.tmdb.org/t/p/original/6z8nV9c5f0p4a3s7x2k1m6h9j0.jpg',
'A retired jailer is pulled into a dangerous mission.',2023),
('Master','Action, Drama','Tamil',7.3,179,
'https://cdn.district.in/movies-assets/images/cinema/Vijay-The-Master----Gallery-6e6877e0-4dcd-11f0-a68c-6d02d77cb994.jpg',
'https://image.tmdb.org/t/p/original/8q5fQ3L0c9V7m2k1j4h6p0s9d8.jpg',
'A professor confronts a criminal network.',2021),
('Interstellar','Sci-Fi, Drama','English',8.7,169,
'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg',
'https://image.tmdb.org/t/p/original/xJHokMbljvjADYdit5fK5jVQ1c5.jpg',
'Explorers travel through a wormhole to save humanity.',2014),
('Inception','Sci-Fi, Thriller','English',8.8,148,
'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
'https://image.tmdb.org/t/p/original/s3TBrRGB1iav7gFOCNx3H31MoES.jpg',
'A specialist enters dreams to plant an idea.',2010),
('The Dark Knight','Action, Crime, Drama','English',9.0,152,
'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg',
'https://image.tmdb.org/t/p/original/hZkgoQYus5vegHoetLkCJzb17zJ.jpg',
'Batman faces a criminal mastermind who plunges Gotham into chaos.',2008);

INSERT INTO theatres(name,location,city) VALUES
('KG Cinemas','Race Course','Coimbatore'),
('Fun Republic','Peelamedu','Coimbatore'),
('Broadway Cinemas','RS Puram','Coimbatore'),
('PVR INOX','Prozone Mall','Coimbatore');

INSERT INTO shows(movie_id,theatre_id,show_date,show_time,price) VALUES
(1,1,CURDATE(),'10:15:00',180),(1,1,CURDATE(),'14:30:00',210),(1,2,CURDATE(),'19:30:00',240),
(1,3,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'20:00:00',220),
(2,1,CURDATE(),'11:00:00',160),(2,2,CURDATE(),'16:00:00',190),(2,4,CURDATE(),'21:15:00',220),
(3,2,CURDATE(),'10:45:00',170),(3,3,CURDATE(),'18:30:00',210),
(4,1,CURDATE(),'19:45:00',260),(4,4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'20:30:00',280),
(5,2,CURDATE(),'21:00:00',250),(5,3,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'19:00:00',260),
(6,4,CURDATE(),'18:00:00',260);

DELIMITER $$

CREATE PROCEDURE seed_all_seats()
BEGIN
 DECLARE done INT DEFAULT 0;
 DECLARE sid INT;
 DECLARE cur CURSOR FOR SELECT id FROM shows;
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
 OPEN cur;
 loop1: LOOP
  FETCH cur INTO sid;
  IF done=1 THEN LEAVE loop1; END IF;
  INSERT IGNORE INTO seats(show_id,seat_no)
  SELECT sid,CONCAT(r.row_name,n.num)
  FROM
  (SELECT 'A' row_name UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F' UNION SELECT 'G' UNION SELECT 'H') r
  CROSS JOIN
  (SELECT 1 num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) n;
 END LOOP;
 CLOSE cur;
END$$

DELIMITER ;
CALL seed_all_seats();
DROP PROCEDURE seed_all_seats;
