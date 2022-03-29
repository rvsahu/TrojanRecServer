package com.errawi.trojanrec.server;


import com.errawi.trojanrec.utils.User;
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
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
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
                user.setNetID(rs.getString(4));
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
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
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
        return user;
    }

    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @return     String printout of timeslots (can format in a more desirable way - ArrayList?)
     *
     */
    public synchronized String getCenterTimeslots(int center_id){
        String output = "";
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
                output = output + '\n' + rs.getString("reservation_time");
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
        return output;
    }


    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @param timedate   Time and date of the gym timeslot
     * @return     True if max capacity for timeslot has been filled
     *
     */
    public synchronized boolean isCapMax(int center_id, String timedate) { // what inputs do we want? this okay?

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT cap_max, cap_curr "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");
            ResultSet rs = pst.executeQuery();

            int max = -1;
            int curr = -1;

            if(rs.next()){
                max = rs.getInt("cap_max");
                //System.out.println(max);
                curr = rs.getInt("cap_curr");
                //System.out.println(curr);
            }
            if(max == curr){
                return true;
            }
            if((max == -1) || (curr == -1)){
                System.out.println("values in isCapMax were not initialized");
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
        return false;
    }


    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @param timedate   Time and date of the gym timeslot
     * @param user       User to add to waitlist table
     *
     */
    public synchronized void addToWaitlist(int center_id, String timedate, User user) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");

            ResultSet rs = pst.executeQuery();

            PreparedStatement pst_k;
            ResultSet rs_k;
            Statement stmt;

            if(rs.next()){
                int timeslot_id = rs.getInt("timeslot_id");
                pst_k = conn.prepareStatement("SELECT user_id "
                		+ "FROM trojanrec.userinfo "
                		+ "WHERE name = '" + user.getName() + "'");
                rs_k = pst_k.executeQuery();
                if(rs_k.next()){
                    String sql = "INSERT INTO trojanrec.waitlist(timeslot_id, user_id) "
                    		+ "VALUES ('" + timeslot_id + "', '" + rs_k.getInt("user_id") + "')";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
    }

    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @param timedate   Time and date of the gym timeslot
     * @param user       User to add to booking table
     *
     */
    public synchronized void makeBooking(int center_id, String timedate, User user) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");

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
                		+ "WHERE name = '" + user.getName() + "'");
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
                            
                        }
                        else {
                        	log.info("A user tried to make a duplicate booking - this is not allowed! No futher action is necessary :-)");
                        }                  
                    }             
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
    }
    
    /**
    *
    * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
    * @param timedate   Time and date of the gym timeslot
    * @param user       User to remove from booking table
    *
    */
   public synchronized void removeBooking(int center_id, String timedate, User user) {
       try {
           conn = datasource.getConnection();

           PreparedStatement pst = conn.prepareStatement
                   ("SELECT timeslot_id "
                   		+ "FROM trojanrec.timeslot "
                   		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");

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
               		+ "WHERE name = '" + user.getName() + "'");
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
                       	log.info("A user tried to remove a booking, but they didn't have a booking - this is not allowed! No futher action is necessary :-)");
                       }                  
                   }             
           }
       }
       catch(SQLException e) {
           log.info("SQLException Message: " + e.getMessage());
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
               log.info("SQLException Message: " + e.getMessage());
           }
       }
	   
   }


    /**
     *
     * @param user       User for whom we want to retrieve current scheduled bookings
     * @return bookings  List of bookings
     *
     */
    public synchronized ArrayList<String> getFutureBookings(User user) {
        ArrayList<String> bookings = new ArrayList<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_now = formatter.format(new Date(System.currentTimeMillis()));

        try{
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement("SELECT user_id "
            		+ "FROM trojanrec.userinfo "
            		+ "WHERE name = '" + user.getName() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()){
                PreparedStatement pst_k = conn.prepareStatement("SELECT timeslot_id "
                		+ "FROM trojanrec.booking " +
                        "WHERE user_id = '" + rs.getInt("user_id") + "'");
                ResultSet rs_k = pst_k.executeQuery();

                while(rs_k.next()){

                    PreparedStatement pst_j = conn.prepareStatement("SELECT reservation_time "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE timeslot_id = '" + rs_k.getInt("timeslot_id") + "'");
                    ResultSet rs_j = pst_j.executeQuery();

                    while(rs_j.next()){
                        int compare = date_now.compareTo(rs_j.getString("reservation_time"));
                        // if current datetime is less than to a booking time, booking is in future
                        if(compare < 0){
                            bookings.add(rs_j.getString("reservation_time"));
                        }
                    }
                }
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
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
    public synchronized ArrayList<String> getPastBookings(User user) {
        ArrayList<String> bookings = new ArrayList<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_now = formatter.format(new Date(System.currentTimeMillis()));

        try{
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement("SELECT user_id "
            		+ "FROM trojanrec.userinfo "
            		+ "WHERE name = '" + user.getName() + "'");
            ResultSet rs = pst.executeQuery();

            if(rs.next()){
                PreparedStatement pst_k = conn.prepareStatement("SELECT timeslot_id "
                		+ "FROM trojanrec.booking " +
                        "WHERE user_id = '" + rs.getInt("user_id") + "'");
                ResultSet rs_k = pst_k.executeQuery();

                while(rs_k.next()){

                    PreparedStatement pst_j = conn.prepareStatement("SELECT reservation_time "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE timeslot_id = '" + rs_k.getInt("timeslot_id") + "'");
                    ResultSet rs_j = pst_j.executeQuery();

                    while(rs_j.next()){
                        int compare = date_now.compareTo(rs_j.getString("reservation_time"));
                        // if current datetime is greater than or equal to a booking time, booking is in past
                        if(compare >= 0){
                            bookings.add(rs_j.getString("reservation_time"));
                        }
                    }
                }
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
        return bookings;
    }




    // SERVER-SIDE ACTIONS (probably need some more of these)
    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @param timedate   Time and date of the gym timeslot
     * @return users     Users that were on waitlist for a specific timeslot
     *
     */
    public synchronized ArrayList<User> getWaitlist(int center_id, String timedate) {
        ArrayList<Integer> user_ids = new ArrayList<>();
        ArrayList<User> users = new ArrayList<>();
        User fetch;

        PreparedStatement pst_j;
        ResultSet rs_j;

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");
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
            log.info("SQLException Message: " + e.getMessage());
            e.printStackTrace();
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
                log.info("SQLException Message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return users;
    }



    /**
     *
     * @param center_id  The rec center's id: 1 is Lyon, 2 is Cromwell, 3 is Village
     * @param timedate   Time and date of the gym timeslot
     * clears the entries in the waitlist for a specific center
     *
     */
    public synchronized void clearWaitlist(int center_id, String timedate) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id "
                    		+ "FROM trojanrec.timeslot "
                    		+ "WHERE center_id = '" + center_id + "' AND reservation_time = '" + timedate + "'");
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
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
    }
    
    
    /**
    *
    * clears the entire bookings table - for testing suite purposes
    * also makes all curr capacities be 0 in timeslot table
    *
    */
    public synchronized void clearBookingsTable() {
        try {
            conn = datasource.getConnection();
            
            String sql = "DELETE FROM trojanrec.booking";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            
            sql = "UPDATE trojanrec.timeslot "
            		+ "SET cap_curr = 0";
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
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
                log.info("SQLException Message: " + e.getMessage());
            }
        }
    	
    }

}