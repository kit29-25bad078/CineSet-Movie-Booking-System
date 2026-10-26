# CineSet Real-World Movie Booking — No Maven

## One-click start
1. Open MySQL Workbench.
2. Open `sql/schema.sql` and run the complete script once. It creates `cineset_booking` and sample movies, theatres, shows and seats.
3. Make sure MySQL Server is running.
4. Double-click `START_CINESET.bat`.
5. The script downloads MySQL Connector/J 9.4.0 if it is not already in `lib/`, compiles the Java backend and opens the website.

### MySQL login used by this package
- User: `root`
- Password: `Kayal@2007`

If your MySQL password is different, edit `src/AppConfig.java` and change only `DB_PASSWORD`.

## URLs
- Customer: http://localhost:8080/
- Admin: http://localhost:8080/admin.html
- Admin username: `admin`
- Admin password: `admin123`

## Important
Do NOT paste Java lines such as `public static final...` into PowerShell. Those lines belong inside the `.java` file.

You can use VS Code Live Server too, but the Java backend must still be running on port 8080. The frontend is configured to call the backend correctly from both `localhost:8080` and Live Server.

## Features
- Real cinema-style responsive dashboard
- Real poster URLs
- Movie search and filters
- Tamil / English interface toggle
- Genre, language, rating and cinema filters
- Cinema/show selection
- Interactive 64-seat map
- Real-time booked/available seat state from MySQL
- OTP confirmation (demo OTP is printed by the server and returned to the demo UI)
- UPI, card and wallet payment selection
- My Bookings and cancellation/refund state
- Smart recommendations based on booking history
- Dark/light mode
- Browser notification permission
- PWA manifest/service worker
- Separate admin dashboard
- Admin movie, theatre and show management
- Booking, refund, revenue, occupancy and popular-movie analytics

## If the connector cannot download
Download MySQL Connector/J 9.4.0 manually and put the JAR in:
`lib\mysql-connector-j-9.4.0.jar`
Then run `START_CINESET.bat` again.
