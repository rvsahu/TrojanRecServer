package com.errawi.trojanrec.server;


import com.errawi.trojanrec.utils.User;
import com.errawi.trojanrec.utils.Reservation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;



public class DatabaseHandler {

    String properties_file;
    HikariConfig config;
    HikariDataSource datasource;
    Connection conn;
    PreparedStatement pst;
    ResultSet rs;

    private static final Logger log;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-7s] %5$s %n");
        log = Logger.getLogger(DatabaseHandler.class.getName());
    }

    public DatabaseHandler() {
        properties_file = "db.properties";
        config = new HikariConfig(properties_file);
        datasource = new HikariDataSource(config);
        conn = null;
    }
    
    public synchronized HikariDataSource datasource() {
		return datasource;
	}

    /**
     *
     * @param net_id  The user's username.
     * @param password  The user's password.
     * @return     True if the user exists, false otherwise.
     */
    public synchronized boolean authenticateUser(String net_id, String password) {

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT * FROM trojanrec.userauth "
                    		+ "WHERE net_id = '" + net_id + "' AND password = '" + password + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                return true;
            }
            // no user was present in ResultSet, user does not exist
            return false;
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
                //datasource.close();
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     *
     * @param net_id  The user's username.
     * @return     User object with the name, student ID, and user photo path set
     *
     */
    public synchronized User retrieveUser(String net_id){

        User user = new User();
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT name, student_id, image_path, net_id " +
                            "FROM trojanrec.userinfo "
                            + "WHERE net_id = '" + net_id + "'");
            ResultSet rs = pst.executeQuery();

            // Use setters defined in User class
            if(rs.next()){
                user.setName(rs.getString(1));
                user.setStudentID(rs.getLong(2));
                user.setUserPhoto(rs.getString(3));
                user.setNetID(net_id);
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            user = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                user = null;
            }
        }
        return user;
    }


    /**
     *
     * @param user_id  The user's id, assigned as primary key in SQL db.
     * @return     User object with the name, student ID, and user photo path set
     *
     */
    public synchronized User retrieveUserById(int user_id){

        User user = new User();
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT name, student_id, image_path, net_id " +
                            "FROM trojanrec.userinfo "
                            + "WHERE user_id = '" + user_id + "'");
            ResultSet rs = pst.executeQuery();

            // Use setters defined in User class
            if(rs.next()){
                user.setName(rs.getString(1));
                user.setStudentID(rs.getLong(2));
                user.setUserPhoto(rs.getString(3));
                user.setNetID(rs.getString(4));
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            user = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                user = null;
            }
        }
        return user;
    }
    
    /*
     * Check that the gym timeslot actually exists before allowing user to make booking, etc.
     */
    
    public synchronized boolean reservationExists(Reservation reservation) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT * FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                return true;
            }
            // no user was present in ResultSet, reservation does not exist
            return false;
            
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
                //datasource.close();
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
        return false;
    	
    }

    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @return     String printout of timeslots (can format in a more desirable way - ArrayList?)
     *
     */
    public synchronized ArrayList<String> getCenterTimeslots(int center_id){
    	
        if(center_id != 1 && center_id != 2 && center_id != 3) {
        	return null;
        }
        
        ArrayList<String> timeslots = new ArrayList<>();
        
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT reservation_time "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id 
                    		+ "' ORDER BY reservation_time");
            ResultSet rs = pst.executeQuery();

            // Use setters defined in User class
            while(rs.next()){
            	timeslots.add(rs.getString("reservation_time"));
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            timeslots = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                timeslots = null;
            }
        }
        return timeslots;
    }

    public synchronized ArrayList<String> getFutureCenterTimeslots(int center_id){       	
        if(center_id != 1 && center_id != 2 && center_id != 3) {
        	return null;
        }
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_now = formatter.format(new Date(System.currentTimeMillis()));
        
        ArrayList<String> timeslots = new ArrayList<>();
        
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT reservation_time "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id 
                    		+ "' ORDER BY reservation_time");
            ResultSet rs = pst.executeQuery();

            // Use setters defined in User class
            while(rs.next()){

                int compare = date_now.compareTo(rs.getString("reservation_time"));
                // if current datetime is less than to a reservation time, reservation is in future
                if(compare < 0){
                    timeslots.add(rs.getString("reservation_time"));
                }

            	
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            timeslots = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                timeslots = null;
            }
        }
        return timeslots;
    }

    /**
     *
     * @param Reservation  Reservation object containing the time/date and center
     * @return     True if max capacity for timeslot has been filled
     *
     */
    public synchronized boolean isCapMax(Reservation reservation) { 

    	boolean exists = reservationExists(reservation);
    	if(!exists) {
    		System.out.println("Reservation does not exist.");
    		return false;
    	}
    	
        try {
	
            conn = datasource.getConnection();
            PreparedStatement pst = conn.prepareStatement
                    ("SELECT cap_max, cap_curr "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");
            ResultSet rs = pst.executeQuery();
            int max = -1;
            int curr = -1;
            if(rs.next()){
                max = rs.getInt("cap_max");
                //System.out.println(max);
                curr = rs.getInt("cap_curr");
                //System.out.println(curr);
            } else {
            	System.out.println("isCapMax: no results");
            }
            if(max == curr){
            	//System.out.println("isCapMax: returning at max == curr (true)");
                return true;
            }
            if((max == -1) || (curr == -1)){
                System.out.println("values in isCapMax were not initialized");
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
        return false;
    }


    /**
     *
     * @param Reservation  Reservation object containing the time/date and center
     * @param user       User to add to waitlist table
     *
     */
    public synchronized void addToWaitlist(Reservation reservation, User user) {
    	
    	boolean exists = reservationExists(reservation);
    	if(!exists) {
    		return;
    	}
    	
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");

            ResultSet rs = pst.executeQuery();

            PreparedStatement pst_k;
            ResultSet rs_k;
            Statement stmt;

            if(rs.next()){
                int timeslot_id = rs.getInt("timeslot_id");
                pst_k = conn.prepareStatement("SELECT user_id "
                		+ "FROM trojanrec.userinfo "
                		+ "WHERE net_id = '" + user.getNetID() + "'");
                rs_k = pst_k.executeQuery();
                if(rs_k.next()){
                	
                	
                    // query - if user already has made waitlist booking at that center/timedate
                    PreparedStatement pst_j = conn.prepareStatement("SELECT EXISTS(SELECT * "
                    		+ "FROM trojanrec.waitlist "
                    		+ "WHERE timeslot_id = '" + timeslot_id + "' AND user_id = '" + rs_k.getInt("user_id") + "')");                      
                    ResultSet rs_j = pst_j.executeQuery();
                    int booking_made = 0;
                    if(rs_j.next()) {
                    	booking_made = rs_j.getInt(1);
                    } 
                	
                    if(booking_made == 0) {
                        String sql = "INSERT INTO trojanrec.waitlist(timeslot_id, user_id) "
                        		+ "VALUES ('" + timeslot_id + "', '" + rs_k.getInt("user_id") + "')";
                        stmt = conn.createStatement();
                        stmt.executeUpdate(sql);                	
                    }
                    else {
                    	System.out.println("A user tried to make a duplicate waitlist booking - this is not allowed! No futher action is necessary :-)");
                    }   

                }
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
    }

    /**
     *
     * @param Reservation  Reservation object containing the time/date and center
     * @param user       User to add to booking table
     *
     */
    public synchronized boolean makeBooking(Reservation reservation, User user) {
    	
    	boolean exists = reservationExists(reservation);
    	if(!exists) {
    		System.out.println("Reservation does not exist");
    		return false;
    	}
    	boolean successfulBooking = false;
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");

            ResultSet rs = pst.executeQuery();

            PreparedStatement pst_k, pst_j;
            ResultSet rs_k, rs_j;
            Statement stmt;
            
            int timeslot_id = -1;

            if(rs.next()){
                timeslot_id = rs.getInt("timeslot_id");
                
                // fetch user
                pst_k = conn.prepareStatement("SELECT user_id "
                		+ "FROM trojanrec.userinfo "
                		+ "WHERE net_id = '" + user.getNetID() + "'");
                rs_k = pst_k.executeQuery();
                
                

                    if(rs_k.next()){
                    	
                    	int userID = rs_k.getInt("user_id");
                    	
                        // query - if user already has made booking at that center/timedate
                        pst_j = conn.prepareStatement("SELECT EXISTS(SELECT * "
                        		+ "FROM trojanrec.booking "
                        		+ "WHERE timeslot_id = '" + timeslot_id + "' AND user_id = '" + userID + "')");                      
                        rs_j = pst_j.executeQuery();
                        int booking_made = 0;
                        if(rs_j.next()) {
                        	booking_made = rs_j.getInt(1);
                        }                                            
                        System.out.println("booking made value: " + booking_made);
                        // user doesn't have booking yet
                        if(booking_made == 0) {                                                 
                            String sql = "INSERT INTO trojanrec.booking(timeslot_id, user_id) "
                            		+ "VALUES ('" + timeslot_id + "', '" + rs_k.getInt("user_id") + "')";
                            stmt = conn.createStatement();
                            stmt.executeUpdate(sql);
                            
                            sql = "UPDATE trojanrec.timeslot "
                            		+ "SET cap_curr = cap_curr + 1 "
                            		+ "WHERE timeslot_id = '" + timeslot_id + "'";
                            stmt = conn.createStatement();
                            stmt.executeUpdate(sql);
                            successfulBooking = true;
                        }
                        else {
                        	System.out.println("A user tried to make a duplicate booking - this is not allowed! No futher action is necessary :-)");
                        }                  
                    } else {
                    	System.out.println("doesn't enter next() block");
                    }
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
            
        }
        return successfulBooking;
    }
    
    /**
    *
    * @param Reservation  Reservation object containing the time/date and center
    * @param user       User to remove from booking table
    *
    */
   public synchronized void removeBooking(Reservation reservation, User user) {

	   boolean exists = reservationExists(reservation);
	   if(!exists) {
   		   System.out.println("Reservation does not exist");
   		   return;
	   }
   	
       try {
           conn = datasource.getConnection();

           PreparedStatement pst = conn.prepareStatement
                   ("SELECT timeslot_id "
                   		+ "FROM trojanrec.timeslot "
                   		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");

           ResultSet rs = pst.executeQuery();

           PreparedStatement pst_k, pst_j;
           ResultSet rs_k, rs_j;
           Statement stmt;
           
           int timeslot_id = -1;

           if(rs.next()){
               timeslot_id = rs.getInt("timeslot_id");
               
               // fetch user
               pst_k = conn.prepareStatement("SELECT user_id "
               		+ "FROM trojanrec.userinfo "
               		+ "WHERE net_id = '" + user.getNetID() + "'");
               rs_k = pst_k.executeQuery();
               
               

                   if(rs_k.next()){
                   	
                   	int userID = rs_k.getInt("user_id");
                   	
                       // query - make sure user does have booking at that center/timedate
                   	   // might be redundant because client-side likely won't show the option to 
                   	   // cancel a booking you haven't made, but keeping this in here for now just in case
                       pst_j = conn.prepareStatement("SELECT EXISTS(SELECT * "
                       		+ "FROM trojanrec.booking "
                       		+ "WHERE timeslot_id = '" + timeslot_id + "' AND user_id = '" + userID + "')");                      
                       rs_j = pst_j.executeQuery();
                       int booking_made = 0;
                       if(rs_j.next()) {
                       	booking_made = rs_j.getInt(1);
                       }
                       
                       // user has booking that can be deleted
                       if(booking_made != 0) {                                                 
                           String sql = "DELETE FROM trojanrec.booking "
                           		+ "WHERE timeslot_id = '" + timeslot_id + "' AND user_id = '" + userID + "'";
                           stmt = conn.createStatement();
                           stmt.executeUpdate(sql);
                           
                           sql = "UPDATE trojanrec.timeslot "
                           		+ "SET cap_curr = cap_curr - 1 "
                           		+ "WHERE timeslot_id = '" + timeslot_id + "'";
                           stmt = conn.createStatement();
                           stmt.executeUpdate(sql);
                           
                       }
                       else {
                       	System.out.println("A user tried to remove a booking, but they didn't have a booking - this is not allowed! No futher action is necessary :-)");
                       }                  
                   }             
           }
       }
       catch(SQLException e) {
           System.out.println("SQLException Message: " + e.getMessage());
       }
       finally {
           try{
               if(rs != null){
                   rs.close();
               }
               if(pst != null){
                   pst.close();
               }
               if(conn != null){
                   conn.close();
               }
           }
           catch(SQLException e){
               System.out.println("SQLException Message: " + e.getMessage());
           }
       }
	   
   }


    /**
     *
     * @param user       User for whom we want to retrieve current scheduled bookings
     * @return bookings  List of bookings
     *
     */
    public synchronized ArrayList<Reservation> getFutureBookings(User user) {
               
        if(user.getStudentID() == -1) {
        	return null;
        }
        
        ArrayList<Reservation> bookings = new ArrayList<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_now = formatter.format(new Date(System.currentTimeMillis()));

        try{
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement("SELECT user_id "
            		+ "FROM trojanrec.userinfo "
            		+ "WHERE net_id = '" + user.getNetID() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()){
                PreparedStatement pst_k = conn.prepareStatement("SELECT timeslot_id "
                		+ "FROM trojanrec.booking " +
                        "WHERE user_id = '" + rs.getInt("user_id") + "'");
                ResultSet rs_k = pst_k.executeQuery();

                while(rs_k.next()){

                    PreparedStatement pst_j = conn.prepareStatement("SELECT reservation_time, center_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE timeslot_id = '" + rs_k.getInt("timeslot_id") + "'");
                    ResultSet rs_j = pst_j.executeQuery();

                    while(rs_j.next()){
                        int compare = date_now.compareTo(rs_j.getString("reservation_time"));
                        // if current datetime is less than to a booking time, booking is in future
                        if(compare < 0){
                        	Reservation reservation = new Reservation();
                        	reservation.setRecCentre(rs_j.getInt("center_id"));
                        	reservation.setTimedate(rs_j.getString("reservation_time"));
                            bookings.add(reservation);
                        }
                    }
                }
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            bookings = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                bookings = null;
            }
        }
        return bookings;
    }


    /**
     *
     * @param user       User for whom we want to retrieve current scheduled bookings
     * @return bookings  List of bookings
     *
     */
    public synchronized ArrayList<Reservation> getPastBookings(User user) {
    	
        if(user.getStudentID() == -1) {
        	return null;
        }
        
        ArrayList<Reservation> bookings = new ArrayList<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_now = formatter.format(new Date(System.currentTimeMillis()));

        try{
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement("SELECT user_id "
            		+ "FROM trojanrec.userinfo "
            		+ "WHERE net_id = '" + user.getNetID() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()){
                PreparedStatement pst_k = conn.prepareStatement("SELECT timeslot_id "
                		+ "FROM trojanrec.booking " +
                        "WHERE user_id = '" + rs.getInt("user_id") + "'");
                ResultSet rs_k = pst_k.executeQuery();

                while(rs_k.next()){

                    PreparedStatement pst_j = conn.prepareStatement("SELECT reservation_time, center_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE timeslot_id = '" + rs_k.getInt("timeslot_id") + "'"); 
                    ResultSet rs_j = pst_j.executeQuery();

                    while(rs_j.next()){
                        int compare = date_now.compareTo(rs_j.getString("reservation_time"));
                        // if current datetime is greater than or equal to a booking time, booking is in past
                        if(compare >= 0){
                        	Reservation reservation = new Reservation();
                        	reservation.setRecCentre(rs_j.getInt("center_id"));
                        	reservation.setTimedate(rs_j.getString("reservation_time"));
                            bookings.add(reservation);
                        }
                    }
                }
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            bookings = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                bookings = null;
            }
        }
        return bookings;
    }
    
    public synchronized ArrayList<Reservation> getWaitlistForUser(User user) {
    	
        if(user.getStudentID() == -1) {
        	return null;
        }
    	
    	ArrayList<Reservation> waitlist_reservations = new ArrayList<>();

        try {
            conn = datasource.getConnection();
            
            PreparedStatement pst = conn.prepareStatement("SELECT user_id "
            		+ "FROM trojanrec.userinfo "
            		+ "WHERE net_id = '" + user.getNetID() + "'");
            ResultSet rs = pst.executeQuery();
            
            if(rs.next()) {

                PreparedStatement pstt = conn.prepareStatement("SELECT timeslot_id "
                		+ "FROM trojanrec.waitlist "
                		+ "WHERE user_id = '" + rs.getInt("user_id") + "'");
                ResultSet rss = pstt.executeQuery();

                while(rss.next()) {
                	PreparedStatement pst_j = conn.prepareStatement("SELECT reservation_time, center_id "
                			+ "FROM trojanrec.timeslot "
                			+ "WHERE timeslot_id = '" + rss.getInt("timeslot_id") + "'");
                	ResultSet rs_j = pst_j.executeQuery();
                	
                	if(rs_j.next()) {
                    	Reservation reservation = new Reservation();
                    	reservation.setRecCentre(rs_j.getInt("center_id"));
                    	reservation.setTimedate(rs_j.getString("reservation_time"));
                        waitlist_reservations.add(reservation);
                	}
                }
            	
            }
            

        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            e.printStackTrace();
            waitlist_reservations = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                e.printStackTrace();
                waitlist_reservations = null;
            }
        }
        return waitlist_reservations;   	
    }




    // SERVER-SIDE ACTIONS (probably need some more of these)
    /**
     *
     * @param Reservation  Reservation object containing the time/date and center
     * @return users     Users that were on waitlist for a specific timeslot
     *
     */
    public synchronized ArrayList<User> getWaitlist(Reservation reservation) {
        ArrayList<Integer> user_ids = new ArrayList<>();
        ArrayList<User> users = new ArrayList<>();
        User fetch;

        PreparedStatement pst_j;
        ResultSet rs_j;
        
    	boolean exists = reservationExists(reservation);
    	if(!exists) {
    		return null;
    	}

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()){
                pst_j = conn.prepareStatement("SELECT user_id "
                		+ "FROM trojanrec.waitlist "
                		+ "WHERE timeslot_id = '" + rs.getInt("timeslot_id") + "'");
                rs_j = pst_j.executeQuery();

                // find all users in waitlist for that reservation
                while(rs_j.next()){
                    user_ids.add(rs_j.getInt("user_id"));
                }
            }

            for(int i=0; i < user_ids.size(); i++){
               int id = user_ids.get(i);
               fetch = retrieveUserById(id);
               users.add(fetch);

            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
            e.printStackTrace();
            users = null;
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
                e.printStackTrace();
                users = null;
            }
        }
        return users;
    }



    /**
     *
     * @param Reservation  Reservation object containing the time/date and center
     * clears the entries in the waitlist for a specific center
     *
     */
    public synchronized void clearWaitlist(Reservation reservation) {
    	
    	boolean exists = reservationExists(reservation);
    	if(!exists) {
    		System.out.println("Reservation does not exist");
    		return;
    	}
    	
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + reservation.getRecCentre() + "' AND reservation_time = '" + reservation.getTimedate() + "'");
            ResultSet rs = pst.executeQuery();

            // delete all entires in waitlist for that timeslot_id
            if(rs.next()){
                String sql = "DELETE FROM trojanrec.waitlist "
                		+ "WHERE timeslot_id = '" + rs.getString("timeslot_id") + "'";
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            }
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
    }
    
    
    /**
    *
    * clears the entire bookings and waitlists tables - for testing suite purposes
    * also makes all curr capacities be 0 in timeslot table
    *
    */
    public synchronized void clearBookingsWaitlistsTables() {
        try {
            conn = datasource.getConnection();
            
            String sql = "DELETE FROM trojanrec.booking";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            
            sql = "DELETE FROM trojanrec.waitlist";
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            
            sql = "UPDATE trojanrec.timeslot "
            		+ "SET cap_curr = 0";
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        }
        catch(SQLException e) {
            System.out.println("SQLException Message: " + e.getMessage());
        }
        finally {
            try{
                if(rs != null){
                    rs.close();
                }
                if(pst != null){
                    pst.close();
                }
                if(conn != null){
                    conn.close();
                }
            }
            catch(SQLException e){
                System.out.println("SQLException Message: " + e.getMessage());
            }
        }
    	
    }
    


}
