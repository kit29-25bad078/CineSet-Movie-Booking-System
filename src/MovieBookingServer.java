import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class MovieBookingServer {
    static final Map<String,String> OTP = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(AppConfig.PORT),0);

        s.createContext("/", MovieBookingServer::staticFile);
        s.createContext("/api/movies", MovieBookingServer::movies);
        s.createContext("/api/theatres", MovieBookingServer::theatres);
        s.createContext("/api/shows", MovieBookingServer::shows);
        s.createContext("/api/seats", MovieBookingServer::seats);
        s.createContext("/api/otp", MovieBookingServer::otp);
        s.createContext("/api/booking", MovieBookingServer::booking);
        s.createContext("/api/bookings", MovieBookingServer::bookings);
        s.createContext("/api/cancel", MovieBookingServer::cancel);
        s.createContext("/api/recommendations", MovieBookingServer::recommendations);
        s.createContext("/api/admin/login", MovieBookingServer::adminLogin);
        s.createContext("/api/admin/summary", MovieBookingServer::adminSummary);
        s.createContext("/api/admin/movies", MovieBookingServer::adminMovies);
        s.createContext("/api/admin/theatres", MovieBookingServer::adminTheatres);
        s.createContext("/api/admin/shows", MovieBookingServer::adminShows);
        s.createContext("/api/admin/movie", MovieBookingServer::adminMovie);
        s.createContext("/api/admin/theatre", MovieBookingServer::adminTheatre);
        s.createContext("/api/admin/show", MovieBookingServer::adminShow);

        s.setExecutor(Executors.newFixedThreadPool(20));
        s.start();
        System.out.println("======================================");
        System.out.println(" CineSet is running");
        System.out.println(" Customer : http://localhost:"+AppConfig.PORT);
        System.out.println(" Admin    : http://localhost:"+AppConfig.PORT+"/admin.html");
        System.out.println("======================================");
    }

    static void staticFile(HttpExchange e) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(e.getRequestMethod())) { HttpUtil.send(e,204,"","text/plain"); return; }
        String p = e.getRequestURI().getPath();
        if (p.equals("/")) p="/index.html";
        Path base=Path.of("web").toAbsolutePath().normalize();
        Path f=base.resolve(p.substring(1)).normalize();
        if(!f.startsWith(base)||!Files.exists(f)||Files.isDirectory(f)){
            HttpUtil.send(e,404,"Not Found","text/plain"); return;
        }
        String ct="text/plain";
        if(p.endsWith(".html"))ct="text/html";
        else if(p.endsWith(".css"))ct="text/css";
        else if(p.endsWith(".js"))ct="application/javascript";
        else if(p.endsWith(".json"))ct="application/json";
        byte[] data=Files.readAllBytes(f);
        e.getResponseHeaders().set("Content-Type",ct+"; charset=utf-8");
        e.sendResponseHeaders(200,data.length);
        try(OutputStream o=e.getResponseBody()){o.write(data);}
    }

    static void movies(HttpExchange e)throws IOException{
        Map<String,String>q=HttpUtil.query(e.getRequestURI().getRawQuery());
        String search=q.getOrDefault("search",""),genre=q.getOrDefault("genre","All"),
               language=q.getOrDefault("language","All"),rating=q.getOrDefault("rating","0"),
               theatre=q.getOrDefault("theatreId","");
        StringBuilder sql=new StringBuilder(
            "SELECT DISTINCT m.* FROM movies m LEFT JOIN shows s ON s.movie_id=m.id WHERE 1=1");
        List<Object> ps=new ArrayList<>();
        if(!search.isBlank()){sql.append(" AND LOWER(m.title) LIKE ?");ps.add("%"+search.toLowerCase()+"%");}
        if(!genre.equals("All")&&!genre.isBlank()){sql.append(" AND m.genre LIKE ?");ps.add("%"+genre+"%");}
        if(!language.equals("All")&&!language.isBlank()){sql.append(" AND m.language=?");ps.add(language);}
        try{sql.append(" AND m.rating>=?");ps.add(Double.parseDouble(rating));}catch(Exception ignored){}
        if(!theatre.isBlank()){sql.append(" AND s.theatre_id=?");ps.add(Integer.parseInt(theatre));}
        sql.append(" ORDER BY m.rating DESC,m.title");
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){
            for(int i=0;i<ps.size();i++)p.setObject(i+1,ps.get(i));
            try(ResultSet r=p.executeQuery()){
                List<String>a=new ArrayList<>();
                while(r.next()){
                    Map<String,Object>m=new LinkedHashMap<>();
                    m.put("id",r.getInt("id"));m.put("title",r.getString("title"));
                    m.put("genre",r.getString("genre"));m.put("language",r.getString("language"));
                    m.put("rating",r.getDouble("rating"));m.put("duration",r.getInt("duration_min"));
                    m.put("poster",r.getString("poster_url"));m.put("backdrop",r.getString("backdrop_url"));
                    m.put("description",r.getString("description"));m.put("year",r.getString("release_year"));
                    a.add(Json.obj(m));
                }
                HttpUtil.json(e,200,Json.arr(a));
            }
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void theatres(HttpExchange e)throws IOException{
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
                "SELECT * FROM theatres ORDER BY city,location");ResultSet r=p.executeQuery()){
            List<String>a=new ArrayList<>();
            while(r.next()){
                Map<String,Object>m=new LinkedHashMap<>();
                m.put("id",r.getInt("id"));m.put("name",r.getString("name"));
                m.put("location",r.getString("location"));m.put("city",r.getString("city"));
                a.add(Json.obj(m));
            }
            HttpUtil.json(e,200,Json.arr(a));
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void shows(HttpExchange e)throws IOException{
        Map<String,String>q=HttpUtil.query(e.getRequestURI().getRawQuery());
        int movieId=Integer.parseInt(q.getOrDefault("movieId","0"));
        String date=q.getOrDefault("date","");
        StringBuilder sql=new StringBuilder(
            "SELECT s.*,t.name theatre_name,t.location FROM shows s JOIN theatres t ON t.id=s.theatre_id WHERE s.movie_id=? AND s.show_date>=CURDATE()");
        if(!date.isBlank())sql.append(" AND s.show_date=?");
        if(!q.getOrDefault("theatreId","").isBlank())sql.append(" AND s.theatre_id=?");
        sql.append(" ORDER BY s.show_date,s.show_time");
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){
            int k=1;p.setInt(k++,movieId);
            if(!date.isBlank())p.setString(k++,date);
            if(!q.getOrDefault("theatreId","").isBlank())p.setInt(k++,Integer.parseInt(q.get("theatreId")));
            try(ResultSet r=p.executeQuery()){
                List<String>a=new ArrayList<>();
                while(r.next()){
                    Map<String,Object>m=new LinkedHashMap<>();
                    m.put("id",r.getInt("id"));m.put("date",r.getString("show_date"));
                    m.put("time",r.getString("show_time"));m.put("price",r.getDouble("price"));
                    m.put("theatreId",r.getInt("theatre_id"));m.put("theatre",r.getString("theatre_name"));
                    m.put("location",r.getString("location"));a.add(Json.obj(m));
                }
                HttpUtil.json(e,200,Json.arr(a));
            }
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void seats(HttpExchange e)throws IOException{
        Map<String,String>q=HttpUtil.query(e.getRequestURI().getRawQuery());
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
                "SELECT id,seat_no,status FROM seats WHERE show_id=? ORDER BY id")){
            p.setInt(1,Integer.parseInt(q.getOrDefault("showId","0")));
            try(ResultSet r=p.executeQuery()){
                List<String>a=new ArrayList<>();
                while(r.next()){
                    Map<String,Object>m=new LinkedHashMap<>();
                    m.put("id",r.getInt(1));m.put("seat",r.getString(2));m.put("status",r.getString(3));
                    a.add(Json.obj(m));
                }
                HttpUtil.json(e,200,Json.arr(a));
            }
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void otp(HttpExchange e)throws IOException{
        String mobile=HttpUtil.field(HttpUtil.body(e),"mobile");
        if(!mobile.matches("\\d{10}")){
            HttpUtil.json(e,400,"{\"error\":\"Enter a valid 10 digit mobile number\"}");return;
        }
        String code=String.valueOf(100000+new Random().nextInt(900000));
        OTP.put(mobile,code);
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
                "INSERT INTO otp_codes(mobile,otp,expires_at) VALUES(?,?,DATE_ADD(NOW(),INTERVAL 5 MINUTE))")){
            p.setString(1,mobile);p.setString(2,code);p.executeUpdate();
        }catch(Exception ignored){}
        System.out.println("DEMO OTP "+mobile+" = "+code);
        HttpUtil.json(e,200,"{\"message\":\"OTP sent\",\"demoOtp\":\""+code+"\"}");
    }

    static void booking(HttpExchange e)throws IOException{
        String b=HttpUtil.body(e);
        String mobile=HttpUtil.field(b,"mobile"),name=HttpUtil.field(b,"name"),
               code=HttpUtil.field(b,"otp"),payment=HttpUtil.field(b,"payment"),
               showText=HttpUtil.field(b,"showId");
        String[] seatArray=HttpUtil.arrayField(b,"seatIds");
        if(!OTP.containsKey(mobile)||!OTP.get(mobile).equals(code)){
            HttpUtil.json(e,400,"{\"error\":\"Invalid or expired OTP\"}");return;
        }
        if(seatArray.length==0){HttpUtil.json(e,400,"{\"error\":\"Select at least one seat\"}");return;}

        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            int showId=Integer.parseInt(showText), userId;

            try(PreparedStatement p=c.prepareStatement(
                    "INSERT INTO users(mobile,name) VALUES(?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),id=LAST_INSERT_ID(id)",
                    Statement.RETURN_GENERATED_KEYS)){
                p.setString(1,mobile);p.setString(2,name);p.executeUpdate();
                try(ResultSet r=p.getGeneratedKeys()){r.next();userId=r.getInt(1);}
            }

            double price;
            try(PreparedStatement p=c.prepareStatement("SELECT price FROM shows WHERE id=?")){
                p.setInt(1,showId);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new Exception("Show not found");price=r.getDouble(1);
                }
            }

            List<Integer>ids=new ArrayList<>();
            for(String s:seatArray)ids.add(Integer.parseInt(s));
            String in=String.join(",",Collections.nCopies(ids.size(),"?"));
            try(PreparedStatement p=c.prepareStatement(
                    "SELECT id,status FROM seats WHERE show_id=? AND id IN ("+in+") FOR UPDATE")){
                p.setInt(1,showId);
                for(int i=0;i<ids.size();i++)p.setInt(i+2,ids.get(i));
                try(ResultSet r=p.executeQuery()){
                    int count=0;
                    while(r.next()){
                        count++;
                        if(!"AVAILABLE".equals(r.getString(2)))throw new Exception("A selected seat was just booked");
                    }
                    if(count!=ids.size())throw new Exception("Invalid seat selection");
                }
            }

            String bookingCode="CS"+(System.currentTimeMillis()%100000000);
            String payRef="PAY-"+UUID.randomUUID().toString().substring(0,10).toUpperCase();
            double total=price*ids.size();

            int bookingId;
            try(PreparedStatement p=c.prepareStatement(
                    "INSERT INTO bookings(booking_code,user_id,show_id,amount,status,payment_method,payment_ref) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)){
                p.setString(1,bookingCode);p.setInt(2,userId);p.setInt(3,showId);p.setDouble(4,total);
                p.setString(5,"CONFIRMED");p.setString(6,payment);p.setString(7,payRef);p.executeUpdate();
                try(ResultSet r=p.getGeneratedKeys()){r.next();bookingId=r.getInt(1);}
            }

            try(PreparedStatement p=c.prepareStatement("UPDATE seats SET status='BOOKED' WHERE id=?")){
                for(int id:ids){p.setInt(1,id);p.addBatch();}p.executeBatch();
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO booking_seats(booking_id,seat_id) VALUES(?,?)")){
                for(int id:ids){p.setInt(1,bookingId);p.setInt(2,id);p.addBatch();}p.executeBatch();
            }
            try(PreparedStatement p=c.prepareStatement(
                    "INSERT INTO payments(booking_id,method,transaction_ref,amount,status) VALUES(?,?,?,?,?)")){
                p.setInt(1,bookingId);p.setString(2,payment);p.setString(3,payRef);p.setDouble(4,total);p.setString(5,"SUCCESS");p.executeUpdate();
            }

            c.commit();OTP.remove(mobile);
            HttpUtil.json(e,200,"{\"success\":true,\"bookingCode\":\""+bookingCode+"\",\"paymentRef\":\""+payRef+"\",\"amount\":"+total+"}");
        }catch(Exception x){HttpUtil.json(e,400,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void bookings(HttpExchange e)throws IOException{
        Map<String,String>q=HttpUtil.query(e.getRequestURI().getRawQuery());
        String mobile=q.getOrDefault("mobile","");
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
            "SELECT b.*,m.title,t.name theatre,t.location,s.show_date,s.show_time,"+
            "(SELECT GROUP_CONCAT(se.seat_no ORDER BY se.seat_no SEPARATOR ', ') FROM booking_seats bs JOIN seats se ON se.id=bs.seat_id WHERE bs.booking_id=b.id) seats "+
            "FROM bookings b JOIN users u ON u.id=b.user_id JOIN shows s ON s.id=b.show_id JOIN movies m ON m.id=s.movie_id JOIN theatres t ON t.id=s.theatre_id "+
            "WHERE u.mobile=? ORDER BY b.created_at DESC")){
            p.setString(1,mobile);
            try(ResultSet r=p.executeQuery()){
                List<String>a=new ArrayList<>();
                while(r.next()){
                    Map<String,Object>m=new LinkedHashMap<>();
                    m.put("id",r.getInt("id"));m.put("code",r.getString("booking_code"));
                    m.put("movie",r.getString("title"));m.put("theatre",r.getString("theatre"));
                    m.put("location",r.getString("location"));m.put("date",r.getString("show_date"));
                    m.put("time",r.getString("show_time"));m.put("seats",r.getString("seats"));
                    m.put("amount",r.getDouble("amount"));m.put("status",r.getString("status"));
                    m.put("payment",r.getString("payment_method"));m.put("paymentRef",r.getString("payment_ref"));
                    a.add(Json.obj(m));
                }
                HttpUtil.json(e,200,Json.arr(a));
            }
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void cancel(HttpExchange e)throws IOException{
        int id=Integer.parseInt(HttpUtil.field(HttpUtil.body(e),"bookingId"));
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("SELECT status FROM bookings WHERE id=? FOR UPDATE")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new Exception("Booking not found");
                    if(!"CONFIRMED".equals(r.getString(1)))throw new Exception("Booking already cancelled");
                }
            }
            try(PreparedStatement p=c.prepareStatement(
                    "UPDATE bookings SET status='REFUNDED',cancelled_at=NOW() WHERE id=?")){p.setInt(1,id);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement(
                    "UPDATE seats se JOIN booking_seats bs ON bs.seat_id=se.id SET se.status='AVAILABLE' WHERE bs.booking_id=?")){
                p.setInt(1,id);p.executeUpdate();
            }
            try(PreparedStatement p=c.prepareStatement(
                    "UPDATE payments SET status='REFUNDED' WHERE booking_id=?")){p.setInt(1,id);p.executeUpdate();}
            c.commit();
            HttpUtil.json(e,200,"{\"success\":true,\"message\":\"Booking cancelled. Demo refund completed.\"}");
        }catch(Exception x){HttpUtil.json(e,400,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void recommendations(HttpExchange e)throws IOException{
        String mobile=HttpUtil.query(e.getRequestURI().getRawQuery()).getOrDefault("mobile","");
        try(Connection c=DB.getConnection()){
            String fav="";
            try(PreparedStatement p=c.prepareStatement(
                "SELECT m.genre FROM bookings b JOIN users u ON u.id=b.user_id JOIN shows s ON s.id=b.show_id JOIN movies m ON m.id=s.movie_id "+
                "WHERE u.mobile=? AND b.status IN ('CONFIRMED','REFUNDED') GROUP BY m.genre ORDER BY COUNT(*) DESC LIMIT 1")){
                p.setString(1,mobile);try(ResultSet r=p.executeQuery()){if(r.next())fav=r.getString(1);}
            }
            String sql=fav.isBlank()?"SELECT * FROM movies ORDER BY rating DESC LIMIT 8":
                "SELECT * FROM movies WHERE genre LIKE ? ORDER BY rating DESC LIMIT 8";
            try(PreparedStatement p=c.prepareStatement(sql)){
                if(!fav.isBlank())p.setString(1,"%"+fav.split(",")[0].trim()+"%");
                try(ResultSet r=p.executeQuery()){
                    List<String>a=new ArrayList<>();
                    while(r.next()){
                        Map<String,Object>m=new LinkedHashMap<>();
                        m.put("id",r.getInt("id"));m.put("title",r.getString("title"));m.put("genre",r.getString("genre"));
                        m.put("language",r.getString("language"));m.put("rating",r.getDouble("rating"));
                        m.put("poster",r.getString("poster_url"));m.put("description",r.getString("description"));
                        a.add(Json.obj(m));
                    }
                    HttpUtil.json(e,200,Json.arr(a));
                }
            }
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void adminLogin(HttpExchange e)throws IOException{
        String b=HttpUtil.body(e);
        String u=HttpUtil.field(b,"username"),p=HttpUtil.field(b,"password");
        boolean ok="admin".equals(u)&&"admin123".equals(p);
        HttpUtil.json(e,ok?200:401,ok?"{\"success\":true,\"role\":\"ADMIN\"}":"{\"error\":\"Invalid admin credentials\"}");
    }

    static void adminSummary(HttpExchange e)throws IOException{
        try(Connection c=DB.getConnection()){
            Map<String,Object>m=new LinkedHashMap<>();
            m.put("movies",scalar(c,"SELECT COUNT(*) FROM movies"));
            m.put("theatres",scalar(c,"SELECT COUNT(*) FROM theatres"));
            m.put("shows",scalar(c,"SELECT COUNT(*) FROM shows"));
            m.put("bookings",scalar(c,"SELECT COUNT(*) FROM bookings"));
            m.put("cancelled",scalar(c,"SELECT COUNT(*) FROM bookings WHERE status='REFUNDED'"));
            m.put("revenue",scalar(c,"SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='SUCCESS'"));
            m.put("occupancy",scalar(c,"SELECT ROUND(100*SUM(status='BOOKED')/COUNT(*),1) FROM seats"));
            m.put("popular",table(c,
                "SELECT m.title name,COUNT(*) value FROM bookings b JOIN shows s ON s.id=b.show_id JOIN movies m ON m.id=s.movie_id WHERE b.status='CONFIRMED' GROUP BY m.id ORDER BY value DESC LIMIT 6",
                "name","value"));
            m.put("peak",table(c,
                "SELECT DATE_FORMAT(s.show_time,'%H:00') name,COUNT(*) value FROM bookings b JOIN shows s ON s.id=b.show_id WHERE b.status='CONFIRMED' GROUP BY HOUR(s.show_time) ORDER BY value DESC",
                "name","value"));
            HttpUtil.json(e,200,Json.obj(m));
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void adminMovies(HttpExchange e)throws IOException{
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,title,genre,language,rating FROM movies ORDER BY id DESC");ResultSet r=p.executeQuery()){
            List<String>a=new ArrayList<>();while(r.next()){
                Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getInt(1));m.put("title",r.getString(2));
                m.put("genre",r.getString(3));m.put("language",r.getString(4));m.put("rating",r.getDouble(5));a.add(Json.obj(m));
            }HttpUtil.json(e,200,Json.arr(a));
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void adminTheatres(HttpExchange e)throws IOException{
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,name,location,city FROM theatres ORDER BY id DESC");ResultSet r=p.executeQuery()){
            List<String>a=new ArrayList<>();while(r.next()){
                Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getInt(1));m.put("name",r.getString(2));
                m.put("location",r.getString(3));m.put("city",r.getString(4));a.add(Json.obj(m));
            }HttpUtil.json(e,200,Json.arr(a));
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void adminShows(HttpExchange e)throws IOException{
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
            "SELECT s.id,m.title,t.name theatre,s.show_date,s.show_time,s.price,"+
            "(SELECT COUNT(*) FROM seats se WHERE se.show_id=s.id AND se.status='BOOKED') booked,"+
            "(SELECT COUNT(*) FROM seats se WHERE se.show_id=s.id) total "+
            "FROM shows s JOIN movies m ON m.id=s.movie_id JOIN theatres t ON t.id=s.theatre_id ORDER BY s.show_date,s.show_time");ResultSet r=p.executeQuery()){
            List<String>a=new ArrayList<>();while(r.next()){
                Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getInt(1));m.put("movie",r.getString(2));
                m.put("theatre",r.getString(3));m.put("date",r.getString(4));m.put("time",r.getString(5));
                m.put("price",r.getDouble(6));m.put("booked",r.getInt(7));m.put("total",r.getInt(8));a.add(Json.obj(m));
            }HttpUtil.json(e,200,Json.arr(a));
        }catch(Exception x){HttpUtil.json(e,500,"{\"error\":\"db\"}");}
    }

    static void adminMovie(HttpExchange e)throws IOException{
        String b=HttpUtil.body(e);
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
            "INSERT INTO movies(title,genre,language,rating,duration_min,poster_url,backdrop_url,description,release_year) VALUES(?,?,?,?,?,?,?,?,?)")){
            p.setString(1,HttpUtil.field(b,"title"));p.setString(2,HttpUtil.field(b,"genre"));
            p.setString(3,HttpUtil.field(b,"language"));p.setDouble(4,Double.parseDouble(HttpUtil.field(b,"rating")));
            p.setInt(5,Integer.parseInt(HttpUtil.field(b,"duration")));p.setString(6,HttpUtil.field(b,"poster"));
            p.setString(7,HttpUtil.field(b,"backdrop"));p.setString(8,HttpUtil.field(b,"description"));
            p.setInt(9,Integer.parseInt(HttpUtil.field(b,"year")));p.executeUpdate();
            HttpUtil.json(e,200,"{\"success\":true}");
        }catch(Exception x){HttpUtil.json(e,400,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void adminTheatre(HttpExchange e)throws IOException{
        String b=HttpUtil.body(e);
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
            "INSERT INTO theatres(name,location,city) VALUES(?,?,?)")){
            p.setString(1,HttpUtil.field(b,"name"));p.setString(2,HttpUtil.field(b,"location"));
            p.setString(3,HttpUtil.field(b,"city"));p.executeUpdate();HttpUtil.json(e,200,"{\"success\":true}");
        }catch(Exception x){HttpUtil.json(e,400,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static void adminShow(HttpExchange e)throws IOException{
        String b=HttpUtil.body(e);
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(
            "INSERT INTO shows(movie_id,theatre_id,show_date,show_time,price) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            p.setInt(1,Integer.parseInt(HttpUtil.field(b,"movieId")));p.setInt(2,Integer.parseInt(HttpUtil.field(b,"theatreId")));
            p.setString(3,HttpUtil.field(b,"date"));p.setString(4,HttpUtil.field(b,"time"));
            p.setDouble(5,Double.parseDouble(HttpUtil.field(b,"price")));p.executeUpdate();
            int sid;try(ResultSet r=p.getGeneratedKeys()){r.next();sid=r.getInt(1);}
            try(PreparedStatement q=c.prepareStatement(
                "INSERT INTO seats(show_id,seat_no) SELECT ?,CONCAT(r.row_name,n.num) FROM "+
                "(SELECT 'A' row_name UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F' UNION SELECT 'G' UNION SELECT 'H') r "+
                "CROSS JOIN (SELECT 1 num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) n")){
                q.setInt(1,sid);q.executeUpdate();
            }
            HttpUtil.json(e,200,"{\"success\":true}");
        }catch(Exception x){HttpUtil.json(e,400,"{\"error\":\""+Json.esc(x.getMessage())+"\"}");}
    }

    static String scalar(Connection c,String sql)throws SQLException{
        try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){r.next();return String.valueOf(r.getObject(1));}
    }
    static String table(Connection c,String sql,String... cols)throws SQLException{
        try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            List<String>a=new ArrayList<>();
            while(r.next()){Map<String,Object>m=new LinkedHashMap<>();for(String col:cols)m.put(col,r.getObject(col));a.add(Json.obj(m));}
            return Json.arr(a);
        }
    }
}
