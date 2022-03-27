package com.errawi.trojanrec.server;


import com.errawi.trojanrec.utils.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;



public class DatabaseMain {

    String properties_file;
    HikariConfig config;
    HikariDataSource datasource;
    Connection conn;
    PreparedStatement pst;
    ResultSet rs;

    private static final Logger log;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-7s] %5$s %n");
        log = Logger.getLogger(DatabaseMain.class.getName());
    }

    public DatabaseMain() {
        properties_file = "db.properties";
        config = new HikariConfig(properties_file);
        datasource = new HikariDataSource(config);
        conn = null;
    }

    /**
     *
     * @param net_id  The user's username.
     * @param password  The user's password.
     * @return     True if the user exists, false otherwise.
     */
    public boolean authenticateUser(String net_id, String password) {

        try {
            //log.info("Connecting to the database");
            conn = datasource.getConnection();
            //log.info("Connected to the database");

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT * FROM trojanrec.userauth WHERE net_id = '"
                            + net_id + "' AND password = '" + password + "'");
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
    public User retrieveUser(String net_id){

        User user = new User();
        try {
            //log.info("Connecting to the database");
            conn = datasource.getConnection();
            //log.info("Connected to the database");

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT name, student_id, image_path " +
                            "FROM trojanrec.userinfo WHERE net_id = '" + net_id + "'");
            ResultSet rs = pst.executeQuery();

            // Use setters defined in User class
            if(rs.next()){
                user.setName(rs.getString(1));
                user.setStudentID(rs.getLong(2));
                user.setUserPhoto(rs.getString(3));
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
    public String getCenterTimeslots(int center_id){
        String output = "";
        try {
            //log.info("Connecting to the database");
            conn = datasource.getConnection();
            //log.info("Connected to the database");

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT reservation_time FROM trojanrec.timeslot WHERE center_id = '"
                            + center_id + "' ORDER BY reservation_time");
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
    public boolean isCapMax(int center_id, String timedate) { // what inputs do we want? this okay?

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT cap_max, cap_curr FROM trojanrec.timeslot WHERE center_id = '"
                            + center_id + "' AND reservation_time = '" + timedate + "'");
            ResultSet rs = pst.executeQuery();

            int max = -1;
            int curr = -1;

            if(rs.next()){
                max = rs.getInt(1);
                curr = rs.getInt(2);
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
    public void addToWaitlist(int center_id, String timedate, User user) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id FROM trojanrec.timeslot WHERE center_id = '"
                            + center_id + "' AND reservation_time = '" + timedate + "'");

            ResultSet rs = pst.executeQuery();

            PreparedStatement pst_k;
            ResultSet rs_k;
            Statement stmt;

            if(rs.next()){
                int timeslot_id = rs.getInt("timeslot_id");
                pst_k = conn.prepareStatement("SELECT user_id FROM trojanrec.userinfo WHERE " +
                        "name = '" + user.getName() + "'");
                rs_k = pst_k.executeQuery();
                if(rs_k.next()){
                    String sql = "INSERT INTO trojanrec.waitlist(timeslot_id, user_id) VALUES " +
                                    "('" + timeslot_id + "', '" + rs_k.getInt("user_id") + "')";
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
    public void makeBooking(int center_id, String timedate, User user) {

    }


    /**
     *
     * @param user       User for whom we want to retrieve current scheduled bookings
     * @return bookings  List of bookings
     *
     */
    public ArrayList<String> getCurrentBookings(User user) {
        ArrayList<String> bookings = new ArrayList<>();
        return bookings;
    }


    /**
     *
     * @param user       User for whom we want to retrieve current scheduled bookings
     * @return bookings  List of bookings
     *
     */
    public ArrayList<String> getAllBookings(User user) {
        ArrayList<String> bookings = new ArrayList<>();
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
    public ArrayList<User> getWaitlist(int center_id, String timedate) {
        ArrayList<User> users = new ArrayList<>();
        User fetch;

        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id FROM trojanrec.timeslot WHERE center_id = '"
                            + center_id + "' AND reservation_time = '" + timedate + "'");
            ResultSet rs = pst.executeQuery();

            PreparedStatement pst_j, pst_k;
            ResultSet rs_j, rs_k;
            if(rs.next()){
                pst_j = conn.prepareStatement("SELECT * FROM trojanrec.waitlist WHERE " +
                        "timeslot_id = '" + rs.getString("timeslot_id") + "'");
                rs_j = pst_j.executeQuery();

                // find all users in waitlist for that reservation
                while(rs_j.next()){
                    pst_k = conn.prepareStatement("SELECT net_id FROM trojanrec.userinfo " +
                            "WHERE user_id = '" + rs_j.getString("user_id") + "'");
                    rs_k = pst_k.executeQuery();

                    // create user object for found user and add to list
                    if(rs_k.next()){
                        fetch = retrieveUser(rs_k.getString("net_id"));
                        users.add(fetch);
                    }
                }
            }
        }
        catch(SQLException e) {
            log.info("SQLException Message: " + e.getMessage());
            log.info("here 1");
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
                log.info("here 2");
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
    public void clearWaitlist(int center_id, String timedate) {
        try {
            conn = datasource.getConnection();

            PreparedStatement pst = conn.prepareStatement
                    ("SELECT timeslot_id FROM trojanrec.timeslot WHERE center_id = '"
                            + center_id + "' AND reservation_time = '" + timedate + "'");
            ResultSet rs = pst.executeQuery();

            // delete all entires in waitlist for that timeslot_id
            if(rs.next()){
                String sql = "DELETE FROM trojanrec.waitlist WHERE timeslot_id = '"
                        + rs.getString("timeslot_id") + "'";
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












}